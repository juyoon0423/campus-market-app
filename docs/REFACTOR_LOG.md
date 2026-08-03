# Campus Market 리팩토링 작업 로그

이 프로젝트를 Claude Code와 함께 리팩토링/포트폴리오 정리하면서 진행한 작업 기록. 다음에 이어서
작업할 때는 이 파일과 `docs/SECURITY_REFACTOR_TODO.md`를 먼저 읽을 것.

## 저장소 현황

- 저장소: `https://github.com/juyoon0423/campus-market-app` (기존 backend/frontend 두 저장소를
  히스토리 보존한 채 하나로 병합함. 기존 두 저장소는 삭제됨)
- 로컬 경로: `/Users/bagjuyun/Desktop/STUDY/프로젝트/Campus Market` (이 폴더 자체가 모노레포
  루트, `backend/`와 `frontend/` 서브디렉토리 포함)
- 로컬 MySQL(`campus_market`, campus_user/1234), Redis가 떠 있어야 백엔드 테스트가 돌아감
  (`@SpringBootTest` 기반, Testcontainers 없음 — 프로젝트 기존 관례)

## 완료된 작업

### 1) 보안 사고 대응 — API 키/비밀번호 git 히스토리 정리
- `application.yml`에 커밋돼있던 카카오 client-secret, Gmail 앱 비밀번호, Gemini API 키를
  `git filter-repo`로 전체 히스토리에서 제거하고 force-push함(당시 backend 저장소가 public이었음)
- **아직 재발급 안 함** — 카카오 client-secret / Gmail 앱 비밀번호 / Gemini API 키 세 개는
  여전히 재발급이 필요함. 미룬 상태.

### 2) 포트폴리오 5번 항목 — 동시성 정합성 (commit `7fa0216`)
- `completeTrade`(거래완료 처리)가 락/상태체크 없이 buyer를 덮어쓰는 문제, `toggleLike`가
  락 없는 read-modify-write라 카운트가 어긋나는 문제를 실제로 재현하고 해결
- `completeTrade`/`updateStatus`: 비관적 락(`ProductRepository.findByIdForUpdate`) + 이미
  SOLD_OUT이면 재처리 거부하는 상태 가드
- `toggleLike`: 낙관적 락(`Product.version`) + 재시도(`ProductLikeService`, REQUIRES_NEW).
  처음엔 `ObjectOptimisticLockingFailureException`만 재시도했는데, 고경합 상황에서 InnoDB
  데드락이 먼저 터지는 걸 실측으로 발견해 `ConcurrencyFailureException`으로 재시도 범위를 넓힘
- 빈 클래스였던 `GlobalExceptionHandler`를 `@RestControllerAdvice`로 구현(락 충돌 → 409)
- `ProductConcurrencyTest` 신규 작성(재현 + 검증 + 낙관적/비관적 벤치마크)
- 포트폴리오 5번 문구 초안: `docs/portfolio_item5_draft.md` 참고 (스크래치패드에서 옮겨옴)

### 3) 포트폴리오 1번 항목("조회 성능 최적화") — 삭제 결정
- 재검증 결과 문제가 알려진 것(캐시 무효화 부재)보다 심각했음:
  - 복합 인덱스가 코드에도, 실제 DB에도 없음(`SHOW INDEX` 확인 — PK, FK 인덱스뿐)
  - DB에 100만 건 없음(현재 15건, 재적재는 `DummyDataController`로 가능)
  - **실제 메인 페이지(`GET /api/products` → `getAllProducts`)가 최적화된 엔드포인트가 아님.**
    k6가 실측한 건 `/api/products/search`이고, 프론트 홈 화면(`app/page.tsx`)은
    `getAllProducts()`를 호출해서 최적화 이전 코드가 그대로 돌아감
- 결론: 포트폴리오에서 삭제하기로 함. Campus Market은 동시성 항목 하나로 감.

### 4) "팀장 리뷰" 기준 필수 리팩토링 1차 (commit `0c60adf`)
- JWT 시크릿을 소스코드 상수 → `application.yml`의 `jwt.secret`로 외부화
- 파일 업로드 경로 조작 취약점(원본 파일명을 저장 경로에 그대로 이어붙이던 것) 제거,
  이미지 확장자 화이트리스트 검증 추가
- `updateStatus`에도 `completeTrade`와 동일한 비관적 락 + SOLD_OUT 이후 상태 변경 차단 가드
- 죽은 코드 `LocalFileStorage.java` 삭제
- `ChatService.findAllRooms`/`findRoomsByProductId`의 N+1(방 개수만큼 반복 쿼리) →
  배치 네이티브 쿼리 1회로 축소
- `FileUploadUtilTest`, `ChatServiceTest` 신규 작성, `ProductConcurrencyTest`에 케이스 추가

## 진행 중 / 다음에 할 일

`docs/SECURITY_REFACTOR_TODO.md` 참고. 요약하면 **비밀번호 평문 저장**이 최우선 순위.
