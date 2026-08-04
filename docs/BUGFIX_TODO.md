# 다음 작업: 카카오맵 기능 실기동 점검 중 발견한 버그/미완성 기능

`docs/REFACTOR_LOG.md`의 "7) 카카오맵 거래 희망 장소 기능 + 전체 기능 실기동 점검"에서 이어지는
목록. 그 세션에서 발견한 버그 3건은 이미 고쳤고(회귀 테스트 포함), 아래 2건은 범위 밖이라
아직 안 건드림. 각 항목에 파일 경로·현재 코드·수정 방향을 적어뒀으니 바로 착수 가능.

## 🔴 버그

### 1. 상품 수정 페이지, 새로고침하면 로그인 상태여도 로그인 화면으로 튕김
- **파일**: `frontend/app/products/[id]/edit/page.tsx`
- **현재 코드** (컴포넌트 최상단 `useEffect`):
  ```tsx
  useEffect(() => {
    if (!isLoggedIn) {
      router.push("/login");
      return;
    }
    if (isInvalidProductId) { ... }
    const fetchProduct = async () => { ... };
    fetchProduct();
  }, [productId, isInvalidProductId, isLoggedIn, router]);
  ```
- **원인**: `AuthContext`(`frontend/src/context/AuthContext.tsx`)는 `token`을 `useEffect`로
  마운트 후 `localStorage`에서 읽어와 채우고, 그 완료 여부를 `isHydrated`로 알려준다. 이 페이지는
  `isHydrated`를 기다리지 않고 `isLoggedIn`만 보고 즉시 `router.push("/login")`을 호출하기 때문에,
  새로고침이나 URL 직접 진입처럼 컴포넌트가 처음부터 마운트되는 경우 항상 "hydration 전 = 아직
  `isLoggedIn=false`인 순간"에 리다이렉트가 먼저 발생해버린다. 같은 프로젝트의
  `frontend/app/upload/page.tsx`는 `if (isHydrated && !isLoggedIn) { router.replace("/login"); }`
  로 올바르게 처리하고 있어서 이 파일만 고치면 된다.
- **재현 방법**: 로그인한 상태에서 `/products/{id}/edit`를 새로고침(또는 주소창에 직접 입력)하면
  로그인 화면으로 이동함. 앱 내부 `<Link>`/`router.push`로 그 페이지에 들어가는 경우는
  AuthContext가 이미 hydrate된 상태라 재현 안 됨 — 그래서 지금까지 안 걸렸을 가능성 높음.
- **수정 방향**:
  ```tsx
  const { isLoggedIn, isHydrated } = useAuth(); // isHydrated 추가로 꺼내기

  useEffect(() => {
    if (!isHydrated) {
      return; // hydration 끝날 때까지 아무 것도 안 함
    }
    if (!isLoggedIn) {
      router.push("/login");
      return;
    }
    ...
  }, [isHydrated, isLoggedIn, productId, isInvalidProductId, router]);
  ```
  `isLoading` 초기값 처리(로딩 화면이 hydration 대기 중에도 자연스럽게 보이는지)만 같이 확인.
- 예상 소요: 10~15분(수정 자체는 5분, 새로고침으로 재현/회귀 확인 포함)

## 🟡 미완성 기능 (버그는 아님 — 구현 여부 확인 필요)

### 2. 리뷰 작성 프론트엔드 UI가 아예 없음
- 백엔드는 완비돼 있음: `ReviewController`(`POST /api/reviews/{productId}`), `ReviewService`,
  `ReviewRequest`(이번 세션에 `@DecimalMin`/`@DecimalMax`/`@NotBlank` 검증까지 추가함),
  `ReviewRepository.existsByProductIdAndWriterId`로 중복 작성도 막혀 있음.
- 프론트엔드(`frontend/`) 어디에도 리뷰 작성 폼이나 API 클라이언트 코드가 없음
  (`src/lib/apis/reviewApi.ts` 같은 파일 자체가 없음 — `grep -rln "review" frontend/app frontend/src`
  결과 0건, "preview" 같은 단어의 부분일치만 걸림). 판매완료(SOLD_OUT)된 상품이라도 구매자가
  리뷰를 남길 방법이 UI상 전혀 없다.
- **수정 방향**(구현하기로 결정되면):
  1. `frontend/src/lib/apis/reviewApi.ts` 신규 작성 — `writeReview(productId, { rating, content })`
  2. `frontend/app/products/[id]/page.tsx`에서 `product.status === "SOLD_OUT"` +
     `currentUserId === product.buyerId`(주의: 현재 `ProductDetailResponse`에 `buyerId` 필드가
     없을 수 있음 — 있는지 먼저 확인, 없으면 백엔드에 추가 필요)일 때만 리뷰 작성 폼 노출
  3. 중복 작성 시 서버가 409/400을 주므로 에러 메시지 처리
- 예상 소요: 1~2시간(백엔드 `buyerId` 응답 필드 확인/추가 포함하면 좀 더 걸릴 수 있음)

## 재개할 때 체크리스트
1. 로컬 MySQL/Redis 떠 있는지 확인(`nc -z localhost 3306`, `nc -z localhost 6379`)
2. `mysql` CLI는 PATH에 등록돼 있음(`/usr/local/mysql/bin`, `~/.zshrc`에 추가함) — 그냥 `mysql`로 바로 사용 가능
3. `docs/REFACTOR_LOG.md`의 "7)" 항목 읽고 이번 세션에서 뭘 했는지 파악
4. 이 파일의 🔴 1번부터 진행
5. 완료 후 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거(둘 다 비면 파일 삭제), 커밋

## 참고 — 이번 세션에서 만든 테스트 계정 (DB에 남아있음, 필요 없으면 정리)
- `maptest-seller1@sj.sangji.ac.kr` / `testpass1234`
- `maptest-buyer1@sj.sangji.ac.kr` / `testpass1234`
