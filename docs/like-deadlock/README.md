# 좋아요 동시 요청 데드락 — 원인 확정 기록

수집일: 2026-11-20 · 기준 커밋 `4c0baca`(코드 변경 없음, 진단용 테스트만 추가)

## 결론

**좋아요 INSERT의 외래 키(FK) 검사가 `product` 행에 공유 락(S)을 걸고, 이어지는 `like_count` UPDATE가
같은 행에 배타 락(X)을 요구하면서 생기는 "S → X 승격" 데드락이다.**
동시에 좋아요를 누른 두 트랜잭션이 서로 S 락을 쥔 채 상대가 S를 놓기를 기다린다.

- 수집한 데드락 **약 516건(트랜잭션 블록 1,032개)이 전부 이 한 가지 패턴**이었다.
- 두 세션으로 같은 순서를 재현하면 **5/5회 데드락**, FK 검사만 끈 대조군은 **3/3회 정상**이었다.
- 낙관적 락(`@Version`)이 막아 줄 수 없는 종류다. 버전을 비교하기 전, UPDATE가 행 락을 얻는 단계에서 막힌다.

## 측정 환경

- MySQL 9.1.0(Docker `mysql:9.1.0`, 127.0.0.1:3307), `REPEATABLE-READ`, `innodb_print_all_deadlocks=ON`
- 로컬 MySQL은 root 권한(PROCESS)이 없어 `SHOW ENGINE INNODB STATUS`를 볼 수 없어서, 같은 버전을 Docker로 따로 띄웠다.
  테이블과 인덱스는 앱의 `ddl-auto: update`가 같은 엔티티로 만든다.
- 진단용 테스트: `backend/src/test/java/com/compus/campusmarket/domain/product/service/LikeDeadlockCaptureTest.java`
  - 상품 1개에 서로 다른 사용자 20명이 동시에 `ProductService.toggleLike`를 호출하는 라운드를 12번 돌렸다.
  - 라운드마다 `INNODB_METRICS.lock_deadlocks` 증가분과 재시도 소진 건수, `SHOW ENGINE INNODB STATUS`의
    LATEST DETECTED DEADLOCK 원문을 저장했다.
  - 이 환경은 기존 README 수치(로컬 MySQL)와 다르다. 서로 직접 비교하지 않는다.

## 수집한 원문

| 파일 | 내용 |
|---|---|
| `rounds/deadlock-round-01.txt` ~ `12.txt` | 라운드마다 LATEST DETECTED DEADLOCK 원문(맨 위 `#` 줄 두 개만 수집 메타데이터) |
| `rounds/summary.tsv` | 라운드별 데드락 수, 재시도 소진 수, `like_count`, 실제 row 수 |
| `rounds/mysqld-error-log-all-deadlocks.log.gz` | `innodb_print_all_deadlocks`로 에러 로그에 남은 **모든** 데드락 원문. MySQL 9 에러 로그에는 `*** (1) TRANSACTION` 머리글 없이 트랜잭션 블록만 이어서 찍힌다 |
| `experiment/` | 아래 두 실험의 SQL과 출력 원문 |

라운드 결과(`rounds/summary.tsv`): 12라운드 × 20건 = 240건 요청에서 데드락 **515건**, 6회 재시도 소진 **8건**.
에러 로그의 트랜잭션 블록은 1,032개(데드락 1건당 2개 → 516건)로, 1건 차이는 집계 구간 차이로 보인다(추측).

## 패턴 비교

12개 라운드 파일과 에러 로그의 1,032개 블록을 같은 기준(락 줄과 SQL 종류)으로 분류했고, 모두 아래와 같았다.

```
(1) HOLDS:  index PRIMARY of table `product` lock mode S locks rec but not gap
(1) WAITS:  index PRIMARY of table `product` lock_mode X locks rec but not gap waiting
(2) HOLDS:  index PRIMARY of table `product` lock mode S locks rec but not gap
(2) WAITS:  index PRIMARY of table `product` lock_mode X locks rec but not gap waiting
두 트랜잭션 모두 실행 중인 SQL = update product ... where id=? and version=?
```

## 원문 해설 (`rounds/deadlock-round-01.txt`)

### 1) 두 트랜잭션은 모두 "상품 행 UPDATE"를 하다가 멈췄다
```
*** (1) TRANSACTION:
TRANSACTION 2400, ACTIVE 0 sec starting index read
LOCK WAIT 7 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 1
update product set ... like_count=16, ... version=16 where id=1 and version=15
```
- 멈춘 문장은 Hibernate가 `product.increaseLikeCount()`를 반영하려고 보낸 UPDATE다.
- `undo log entries 1`은 이 트랜잭션이 **이미 한 건을 바꿨다**는 뜻이다. 앞서 실행한 `product_like` INSERT다.
  Hibernate는 한 번에 반영(flush)할 때 INSERT를 UPDATE보다 먼저 보낸다.
- 트랜잭션 (2)도 같은 UPDATE(`version=17 where id=1 and version=16`)에 `undo log entries 1`이다.

### 2) 둘 다 상품 행에 "공유 락(S)"을 이미 쥐고 있다
```
*** (1) HOLDS THE LOCK(S):
RECORD LOCKS ... index PRIMARY of table `campus_market`.`product` trx id 2400 lock mode S locks rec but not gap
Record lock, heap no 2 ... 0: len 8; hex 8000000000000001
```
- `index PRIMARY of table product`: 상품 테이블의 기본키 인덱스, 즉 상품 행 자체.
- `hex 8000000000000001`: 기본키 값 1. 이 라운드의 상품 id(1)와 같다.
- `lock mode S`: 공유 락. 여러 트랜잭션이 동시에 가질 수 있는 "읽기용" 락이다.
- `locks rec but not gap`: 그 행 하나만 잠그는 레코드 락이다. 행 사이 간격을 잠그는 갭 락이나
  넥스트키 락은 **아니다.** 기본키로 정확히 한 행을 찾았기 때문이다.
- (2)도 똑같은 줄을 갖고 있다. S 락끼리는 충돌하지 않아서 둘 다 받을 수 있었다.

### 3) 둘 다 같은 행에 "배타 락(X)"을 기다린다
```
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS ... index PRIMARY of table `campus_market`.`product` trx id 2400 lock_mode X locks rec but not gap waiting
```
- UPDATE는 행을 바꾸므로 X 락이 필요하다. X는 다른 트랜잭션의 S와 함께 존재할 수 없다.
- (1)은 (2)가 S를 놓기를, (2)는 (1)이 S를 놓기를 기다린다. 둘 다 자기 S를 놓지 않은 채라 영원히 끝나지 않는다.
```
*** WE ROLL BACK TRANSACTION (2)
```
- InnoDB가 이 순환을 감지하고 (2)를 되돌린다. 앱에는 에러 1213(Deadlock found)으로 돌아오고,
  `ProductService.toggleLike`가 `ConcurrencyFailureException`으로 잡아 재시도한다.

### 4) 그 S 락은 어디서 왔나 → FK 검사 (실험으로 확인)
원문은 S 락이 생긴 이유를 직접 적어 주지 않는다. 그런데 앱 코드에서 상품을 읽는 `findById`는 락을 걸지 않는
일반 조회다. 그래서 `product_like` INSERT 하나만 실행하고 그 트랜잭션의 락을 `performance_schema.data_locks`로
확인했다(`experiment/01-locks-after-insert.*`).

| 조건 | 상품 행(PRIMARY 9001) | 사용자 행(PRIMARY 9002) | product_like |
|---|---|---|---|
| FK 검사 켬(기본값) | **`S,REC_NOT_GAP` GRANTED** | `S,REC_NOT_GAP` GRANTED | 테이블 IX |
| FK 검사 끔(`foreign_key_checks=0`) | 없음 | 없음 | 테이블 IX |

- INSERT할 때 InnoDB는 "참조하는 상품이 정말 있는지"를 확인하고, 트랜잭션이 끝날 때까지 그 상품 행이
  지워지거나 바뀌지 않도록 **S 레코드 락**을 건다. 원문의 `lock mode S locks rec but not gap`과 정확히 같은 락이다.
- 사용자 행에도 S 락이 걸리지만, 좋아요를 누른 사람이 서로 달라 충돌하지 않는다.
- 정황 증거: INSERT 직후 락은 5개(테이블 IS×2, IX×1, 레코드 S×2)이고, UPDATE가 테이블 IX와 레코드 X(대기)를
  더하면 7개, 행 락은 3개가 된다. 원문의 `7 lock struct(s), 3 row lock(s)`와 맞는다(이 셈 방식은 추측).

### 5) 두 세션으로 그대로 재현 (`experiment/02-*`)
세션 A와 B가 각각 `INSERT product_like` → (1초 뒤) `UPDATE product`를 0.5초 차이로 실행했다.

```
A: INSERT product_like   01:25:39.047
B: INSERT product_like   01:25:39.552
A: UPDATE product        01:25:40.049   ← B의 S 락 때문에 대기
B: UPDATE product        01:25:40.558   → ERROR 1213 (40001): Deadlock found when trying to get lock
A: UPDATE 성공            01:25:40.563   ← B가 되돌려지자 진행
```
- 5회 반복 모두 같은 결과였고, 이때 잡힌 원문(`experiment/02-two-session-deadlock.txt`)도 앱에서 잡힌 것과 같은 패턴이다.
- 대조군(`experiment/03-*`): 같은 순서에서 FK 검사만 끄면 3회 모두 A, B 둘 다 성공했고 오류는 0건이었다.
  (대조군 SQL은 02를 복사해 만들어서 A의 안내 문구에 "대기"가 남아 있지만, 실제로는 대기 없이 바로 성공했다.)

## 면접용 한 단락

> 좋아요는 `product_like`에 INSERT하고 `product.like_count`를 UPDATE합니다. INSERT할 때 InnoDB가 FK 검사를 위해
> 부모인 상품 행에 공유 락을 겁니다. 그래서 같은 상품에 동시에 좋아요가 들어오면 두 트랜잭션이 모두 상품 행에 공유 락을
> 쥔 상태가 되고, 둘 다 UPDATE를 위해 배타 락을 요청하는 순간 서로를 기다리는 데드락이 됩니다.
> `SHOW ENGINE INNODB STATUS` 원문에서 두 트랜잭션이 같은 상품 행에 `S rec but not gap`을 쥐고 `X`를 기다리는 것을
> 확인했고, `data_locks`로 그 S 락이 INSERT의 FK 검사에서 생긴다는 것을, FK 검사를 끈 대조 실험으로 그것만 빼면
> 데드락이 사라진다는 것을 확인했습니다. 버전 비교 전에 락 단계에서 막히기 때문에 낙관적 락으로는 해결되지 않는 문제였습니다.

## 수정안 실험 (`experiment/options/`)

같은 두 세션 방식(A는 0초, B는 0.5초에 시작, 단계 사이 1초)으로 각 3회 실행했다. 결과 요약은 `experiment/options/results.txt`.

| 실험 | 순서 | 결과 |
|---|---|---|
| `04-atomic-after-insert` | INSERT → 원자적 UPDATE | 3/3회 데드락(원자적 UPDATE만으로는 해결 안 됨) |
| `05-lock-first` (채택한 A) | `SELECT … FOR UPDATE` → INSERT → UPDATE | 3/3회 정상, B는 FOR UPDATE에서 A 커밋까지 대기 |
| `06-update-first` | 원자적 UPDATE → INSERT | 3/3회 정상 |
| `07-no-counter` | INSERT만, 개수는 COUNT | 3/3회 정상, 대기 없음 |
| `08-separate-counter` | INSERT → 별도 카운터 테이블 UPDATE | 3/3회 정상 |

## 수정 후 측정 (`bench/`)

`LikeContentionBenchmarkTest`를 Docker MySQL에서 수정 전(`bench/before/`, 커밋 `4c0baca`)과 수정 후(`bench/after/`)에
같은 조건으로 각 3회 실행했다. 동시 20건 × 50라운드 = 1,000건(예열 2라운드는 기록하지 않음).
회차별 폴더(`run-1`~`run-3`)에 `summary.tsv`(요약), `*-rounds.tsv`(라운드별), `*-requests.csv`(요청별 응답 시간),
`code-version.txt`가 있고, 3회 집계는 `bench/summary-3runs.tsv`(`bench/aggregate.py`로 생성)에 있다.
표는 3회의 중앙값(최소~최대)이다.

| 시나리오 | 데드락 | 낙관적 충돌 | 재시도 소진 | 응답 p50 | 응답 p95 |
|---|---|---|---|---|---|
| 상품 1개 (동시 20건) | 1,971 (1,950~1,974) → 0 | 623 (592~624) → 0 | 14 (14~15) → 0 | 84.3 (81.8~86.5) → 31.0 (30.6~31.3)ms | 195.7 (186.8~202.7) → 56.0 (55.9~58.7)ms |
| 상품 5개 (상품당 4건) | 402 (400~430) → 0 | 283 (278~297) → 0 | 0 → 0 | 22.3 (22.2~23.1) → 16.9 (16.5~17.0)ms | 56.7 (56.1~60.1) → 25.3 (24.5~25.6)ms |
| 상품 20개 (상품당 1건) | 0 → 0 | 0 → 0 | 0 → 0 | 12.9 (12.4~13.6) → 13.7 (13.4~13.9)ms | 19.5 (17.6~19.5) → 20.2 (19.2~20.6)ms |

- 데드락 수와 행 락 대기 수는 `INNODB_METRICS`(`lock_deadlocks`, `lock_row_lock_waits`) 증가분이다.
- 재시도 원인은 수정 전 코드의 재시도 경고 로그(`원인=...`)를 셌다. 수정 후에는 재시도 루프가 없어 0이다.
  "재시도 소진"은 수정 전후 모두 `toggleLike`가 예외로 끝난 요청 수다.
- 수정 후 상품 1개 시나리오의 행 락 대기 950회(3회 모두 같음, 라운드당 19회)는 FOR UPDATE에서 순서를 기다린 횟수다.
  데드락 대신 줄을 서는 비용이다.
