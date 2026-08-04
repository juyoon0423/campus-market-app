# 다음 작업: "팀장 리뷰" 기준 필수 리팩토링 2차

`docs/REFACTOR_LOG.md`의 "4) 팀장 리뷰 필수 리팩토링 1차"에 이어지는 목록. 🔴 Critical 1, 2번은
완료(`docs/REFACTOR_LOG.md`의 "5) 팀장 리뷰 필수 리팩토링 2차" 참고). 아래 🟡 3번부터 이어서
진행하면 됨.

## 🟡 중요 — 여유 되면

### 3. 메인 페이지 조회 페이지네이션 없음 + fetch join 컬렉션
- **파일**:
  - `backend/.../domain/product/repository/ProductRepositoryImpl.java`의 `findActiveProducts()`
  - `backend/.../domain/product/controller/ProductController.java`의 `getAllProducts`(Pageable 파라미터 없음)
  - 프론트 `frontend/app/page.tsx`가 `getAllProducts()`(`GET /api/products`)를 호출 — 실제
    최적화된 엔드포인트는 `/api/products/search`인데 홈 화면은 이걸 안 씀
- **현재 코드**(`findActiveProducts`):
  ```java
  return queryFactory.selectFrom(product).distinct()
          .leftJoin(product.seller).fetchJoin()
          .leftJoin(product.images).fetchJoin()
          .where(product.status.in(SELLING, RESERVED))
          .orderBy(product.createdAt.desc())
          .fetch();   // ← LIMIT 없음, 컬렉션 fetch join이라 카테시안 곱
  ```
- **수정 방향**: 가장 저렴한 옵션은 프론트 `app/page.tsx`의 `fetchProducts`가 `getAllProducts()`
  대신 `searchProducts({})`(빈 파라미터)를 호출하도록 바꾸는 것. 이미 페이지네이션·캐싱·
  N+1 방지가 다 돼있는 경로라 백엔드는 거의 안 건드려도 됨. 단, `search()`는 status 필터가
  없으면 SOLD_OUT도 노출되니 기본 필터 정책을 정해야 함(예: status 파라미터 없을 때도
  SELLING/RESERVED만 보이게 서비스단에서 기본값 처리).
- 예상 소요: 1~2시간(프론트 변경 + 상태 필터 정책 결정 + 재검증)

### 4. 리뷰 중복 작성 미차단
- **파일**: `backend/.../domain/product/review/service/ReviewService.java`의 `writeReview`
- 이미 `ReviewRepository.existsByProductIdAndWriterId(productId, writerId)`가 정의돼 있는데
  호출부가 없음(미사용 상태).
- **수정 방향**: `writeReview` 맨 앞에 가드 추가
  ```java
  if (reviewRepository.existsByProductIdAndWriterId(productId, writerId)) {
      throw new IllegalStateException("이미 리뷰를 작성한 거래입니다.");
  }
  ```
- 예상 소요: 15분(테스트 포함해도 30분)

## 🟢 낮음 — 선택

### 5. `infra/email/EmailService.java` 죽은 코드
- `LocalFileStorage.java`와 동일 패턴의 빈 클래스. 삭제만 하면 됨.

### 6. 이메일 인증코드 인메모리 저장
- `domain/user/service/EmailService.java`가 인증코드를 `ConcurrentHashMap`에 저장(코드 자체
  주석에 "실무에서는 Redis 권장"이라고 적혀 있음). Redis는 이미 프로젝트에 있으니 옮기면
  일관성은 좋아지지만, 지금 단일 인스턴스 규모에선 급하지 않음.

## 📎 추가로 발견한 것 (이번 세션에서 새로 찾음, 아직 코드는 안 건드림)

### 7. `ChatService.createOrGetRoom`의 채팅방 중복 생성 레이스
- 저장소 안에 이미 이 문제를 분석해둔 문서가 있었음: `frontend/BACKEND_IMPROVEMENTS.md`
  ("`createOrGetRoom` API에서 동시에 요청이 들어올 때 Check-Then-Act 패턴으로 채팅방이
  중복 생성됨"). 다시 확인해보니 **아직 고쳐지지 않은 상태** — `ChatRoom` 엔티티에 unique
  제약이 없고, `createOrGetRoom`도 조회 후 없으면 생성하는 락 없는 구조 그대로.
- 지금 세션에서 고친 `ProductLike` unique 제약 + REQUIRES_NEW 재시도와 정확히 같은 패턴으로
  고칠 수 있음(`ChatRoom`에 `(product_id, buyer_id)` unique 제약 추가, `DataIntegrityViolationException`
  잡아서 기존 방 재조회). `docs/REFACTOR_LOG.md`의 2번 작업(commit `7fa0216`)을 참고해서
  같은 방식으로 처리하면 됨.
- 우선순위는 중간 정도 — 실제 피해는 "중복 채팅방 생성"이라 사용자 경험 문제이지 데이터
  유실/보안 문제는 아님. 시간 되면 3번/4번과 묶어서 처리.

## 재개할 때 체크리스트
1. 로컬 MySQL/Redis 떠 있는지 확인(`nc -z localhost 3306`, `nc -z localhost 6379`)
2. `docs/REFACTOR_LOG.md` 읽고 지금까지 뭘 했는지 파악
3. 이 파일의 🟡 3번부터 순서대로 진행
4. 각 항목 수정 후 `./gradlew test`로 전체 테스트 통과 확인(가능하면 새 항목마다 검증 테스트 추가)
5. 완료되면 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거, 커밋
