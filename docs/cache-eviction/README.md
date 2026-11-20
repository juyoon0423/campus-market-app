# 목록 캐시 비우기가 간헐적으로 늦게 반영되던 문제 — 증거 기록

측정일: 2026-11-20 · 기준 커밋 `e43fc0d` + 작업 트리 변경(캐시 무효화 작업)

## 요약

- 상품 쓰기 작업(등록·수정·삭제·상태 변경·거래완료)에 `@CacheEvict(allEntries = true)`를 붙였는데도,
  바로 다음 목록 조회가 **간헐적으로 옛 캐시를 읽었다.**
- 원인: Spring Data Redis 4.0의 `RedisCacheWriter`는 기본값이 `immediateWrites = false`라서,
  Lettuce를 쓰면 put/clear를 **별도 커넥션으로 비동기로 보내고 끝날 때까지 기다리지 않는다.**
  `@CacheEvict(allEntries = true)`(beforeInvocation = false)는 `Cache.clear()`를 부르는데,
  이 clear는 지연 실행이 허용되는 쪽이다(즉시 실행이 필요하면 `invalidate()`).
- 수정: `RedisCacheWriter.create(cf, w -> w.batchStrategy(BatchStrategies.scan(1000)).immediateWrites())`
  (`backend/.../global/config/RedisCacheConfig.java`).

## 반복 측정 (각 50회)

`ProductListCacheEvictionTest`(6개 테스트)를 `./gradlew cleanTest test`로 50번씩 실행했다.
두 조건은 `RedisCacheConfig`의 `.immediateWrites()` 한 줄만 다르다(`transactionAware()`는 둘 다 적용).

| 조건 | 실패한 실행 | 실패한 테스트 수 (전체 300건 중) | 원본 |
|---|---|---|---|
| `immediateWrites` 적용 전 | **12 / 50** | 15 | [`bench-before-immediateWrites.txt`](bench-before-immediateWrites.txt) |
| `immediateWrites` 적용 후 | **0 / 50** | 0 | [`bench-after-immediateWrites.txt`](bench-after-immediateWrites.txt) |

적용 전 테스트별 실패 횟수: 거래완료 7, 등록 2, 수정 2, 삭제 2, 판매완료로 변경 2, 바깥 트랜잭션 0.

## 실패한 1회의 원문

- [`monitor-failing-run.log`](monitor-failing-run.log): 그 실행 동안의 `redis-cli MONITOR` 전체 출력
- [`app-log-failing-run.log`](app-log-failing-run.log): 같은 실행에서 실패한 테스트(거래완료) 구간의
  애플리케이션 로그 중 캐시·트랜잭션 줄만 원문 그대로 골라낸 것과 실패 메시지

애플리케이션 로그(시각은 KST):
```
09:54:16.331 Committing JPA transaction ...                  ← completeTrade 커밋
09:54:16.336 Invalidating entire cache for operation ...completeTrade   ← clear() 호출
09:54:16.351 Cache entry for key '[캐시무효화-478070ad-..., null, null, 0, 20]' found   ← 바로 다음 조회가 옛 캐시를 읽음
```

같은 순간의 Redis MONITOR(시각은 epoch 초):
```
1795136056.337119 [0 127.0.0.1:56123] "SCAN" "0" "MATCH" "products-v2::*" "COUNT" "1000"
1795136056.337794 [0 127.0.0.1:56124] "GET"    "products-v2::...478070ad-...,null,null,0,20"
1795136056.337862 [0 127.0.0.1:56123] "UNLINK" "products-v2::...478070ad-...,null,null,0
```

- 쓰기(SCAN, UNLINK)는 `:56123` 커넥션, 조회(GET)는 `:56124` 커넥션으로 나갔다.
- clear가 SCAN으로 키를 찾은 뒤 UNLINK를 보내기 전 **68µs 사이에** 다음 조회의 GET이 먼저 도착했다.
  그래서 GET이 지워지기 직전의 옛 값을 읽었다.

## 직접 재현하는 방법

1. `backend/src/main/java/com/compus/campusmarket/global/config/RedisCacheConfig.java`에서
   `.immediateWrites()`를 지우고, 바로 위 줄 `.batchStrategy(BatchStrategies.scan(1000))` 끝에 `)`를 붙여 문장을 닫는다.
2. 다른 터미널에서 `redis-cli monitor > monitor.log`를 실행해 둔다.
3. `backend`에서 `for i in $(seq 1 20); do ./gradlew cleanTest test --tests '*ProductListCacheEvictionTest' || break; done`
   를 실행한다(대략 4~5번에 1번 실패). 실패하면 반복이 멈춘다.
4. monitor를 멈추고 `grep -n -E '"(SCAN|GET|UNLINK)"' monitor.log`로 보면
   `SCAN` → `GET` → `UNLINK` 순서로 섞인 곳이 실패한 테스트의 키다.
5. 1번을 되돌리고(`git checkout -- <파일>` 또는 수동 복구) 같은 반복이 통과하는지 확인한다.
