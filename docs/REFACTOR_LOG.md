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
- Gmail 앱 비밀번호(`GOOGLE_APP_PASSWORD`), Gemini API 키(`GEMINI_API_KEY`) 재발급 완료.
  값은 모노레포 루트 `.env`(신규, 루트 `.gitignore`로 보호)에 두고, `application.yml`은
  `${GOOGLE_APP_PASSWORD}`/`${GEMINI_API_KEY}` 플레이스홀더로 바꿈. 백엔드는 독립된 Gradle
  프로젝트라 Spring Boot가 루트 `.env`를 자동으로 읽지 못하므로, `backend/build.gradle`이
  `.env`를 직접 파싱해 `test`/`bootRun` 태스크의 프로세스 환경변수로 주입하도록 연결함.
  카카오 맵 API 키(`NEXT_PUBLIC_KAKAO_MAP_KEY`, 신규 발급)는 Next.js가 자동으로 읽는
  `frontend/.env.local`에 둠(아직 코드에서 실제로 쓰는 곳은 없음 — 지도 기능 추가 시 사용 예정).
- **아직 재발급 안 함** — 카카오 OAuth `client-secret`(`application.yml`의
  `spring.security.oauth2.client.registration.kakao.client-secret`)은 여전히 예전 값이 하드코딩된
  채로 남아있음. 재발급 필요.

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

### 5) "팀장 리뷰" 기준 필수 리팩토링 2차 — Critical 2건
- **비밀번호 평문 저장/비교** (`UserService.java`, `SecurityConfig.java`)
  - `SecurityConfig`에 `PasswordEncoder`(`BCryptPasswordEncoder`) 빈 추가
  - `signUp`: `passwordEncoder.encode(...)`로 인코딩 후 저장 / `login`: `passwordEncoder.matches(...)`로 비교
  - OAuth 가입 경로(`CustomOAuth2UserService`)는 `UserService.signUp`을 거치지 않고 하드코딩
    placeholder(`"OAUTH_USER"`)를 쓰며 로그인 비교에도 안 쓰여 영향 없음을 확인, 손대지 않음
  - `UserServiceTest` 신규 작성(해시 저장 확인 + 정상/오답 비밀번호 로그인 검증)
  - 기존 로컬 DB의 평문 비밀번호 계정은 로그인이 깨짐(예상된 동작, 재가입 필요)
- **`/api/dummy/**` 무인증 노출** (`SecurityConfig.java`)
  - TODO는 `@Profile("local")`(옵션 b)을 추천했지만, 이 프로젝트엔 프로필 분리 인프라가
    전혀 없어서(`application-local.yml`, `SPRING_PROFILES_ACTIVE` 설정 등 전무) 그대로
    적용하면 로컬 실행 방식 자체를 바꿔야 하는 부작용이 있었음. 대신 옵션 (a)로 처리:
    `permitAll` 목록에서 `/api/dummy/**` 제거해 인증을 요구하도록만 변경(로그인은 필요하지만
    관리자 권한 개념 자체가 프로젝트에 없어 role 기반 제한은 별도 과제로 남음)
  - 인증 없이 호출 시 401 반환 확인(`curl -X POST /api/dummy/products` → 401)

### 6) 코드베이스 전수 점검 후 발견한 항목 5건 수정
Claude Code(Explore 서브에이전트 2개, 백엔드/프론트 각각)로 문서에 없던 이슈를 새로 찾아
심각도순으로 5개를 골라 순서대로 수정함.

1. **`updateProduct` multipart 헤더 회귀 버그** (`frontend/src/lib/apis/productApi.ts`)
   - `README.md`에 "해결했다"고 적힌 "Content-Type을 수동 지정하면 boundary가 빠져 멀티파트
     파싱이 깨진다" 문제가 `updateProduct`에서 재발한 상태였음(`createProduct`는 정상).
     `headers: { "Content-Type": "multipart/form-data" }` 제거.
2. **채팅 메시지 조회 IDOR** (`ChatRoomController`, `ChatService`, `ChatRoom` 엔티티)
   - `GET /api/chat/room/{roomId}/messages`가 `@AuthenticationPrincipal`조차 받지 않아 로그인한
     아무 사용자나 roomId를 순회하며 타인의 대화를 열람 가능했음. `ChatRoom.validateParticipant`
     신규 추가, `findMessagesByRoomId`가 seller/buyer 여부를 검증하도록 수정.
   - `ChatServiceTest`에 참여자 아닌 사용자의 조회 실패 케이스 추가.
3. **상품 삭제 시 FK 연관 데이터 정리 누락** (`ProductService.deleteProduct`)
   - `ProductLike`/`ChatRoom`(+`ChatMessage`)/`Review`를 먼저 정리하지 않아 찜/채팅방/리뷰가
     있는 상품은 삭제 시 `DataIntegrityViolationException`으로 실패했음. 삭제 전 연관 데이터를
     순서대로(메시지→채팅방, 리뷰, 좋아요) 정리하도록 수정. `ChatRoomRepository`/
     `ChatMessageRepository`/`ReviewRepository`에 `deleteAllByProduct_Id` 등 추가.
   - `ProductServiceTest` 신규 작성(찜+채팅방+리뷰가 걸린 상품도 정상 삭제되는지 검증).
4. **WebSocket(STOMP) 채팅 무인증 + senderId 위조** (`WebSocketConfig`, `ChatController`, `ChatService`)
   - `/ws-stomp`는 HTTP `JwtAuthenticationFilter`를 거치지 않아 STOMP 레벨에 인증이 전혀
     없었고, 클라이언트가 보낸 `senderId`를 그대로 신뢰해 타인 명의로 메시지 위조가 가능했음.
   - `StompAuthChannelInterceptor` 신규 추가: CONNECT 프레임의 `Authorization` 네이티브 헤더로
     JWT를 검증하고 유효하면 `StompHeaderAccessor`에 인증된 유저를 심음(`WebSocketConfig`의
     `configureClientInboundChannel`에 등록). `ChatController.message`는 클라이언트가 보낸
     `senderId` 대신 `Principal`에서 얻은 값을 사용하고, `ChatService.saveMessage`도 방
     참여자인지 검증 후 저장. `ChatMessageRequest`에서 이제 무의미해진 `senderId` 필드 제거.
   - 프론트(`app/chat/page.tsx`)는 STOMP `connectHeaders`에 `Authorization: Bearer {token}`을
     실어 보내도록 수정하고, 더 이상 쓰지 않는 `senderId`를 publish 페이로드에서 제거.
   - `StompAuthChannelInterceptorTest` 신규 작성(정상/누락/위조 토큰 케이스), `ChatServiceTest`에
     비참여자 senderId로 메시지 전송 시도 시 거부되는 케이스 추가.
5. **요청 DTO Bean Validation 전면 부재** (`ProductCreateRequest`/`ProductUpdateRequest`/
   `StatusUpdateRequest`/`ReviewRequest`/`UserSignUpRequest`/`UserLoginRequest`, `GlobalExceptionHandler`)
   - `build.gradle`에 `spring-boot-starter-validation` 추가, 각 DTO에 `@NotBlank`/`@NotNull`/
     `@Positive`/`@Email`/`@Size`/`@DecimalMin`·`@DecimalMax` 적용하고 해당 컨트롤러 메서드에
     `@Valid` 추가. `GlobalExceptionHandler`에 `MethodArgumentNotValidException` 핸들러 추가해
     첫 번째 필드 에러 메시지를 400으로 반환하도록 통일.
   - STOMP 쪽 `ChatMessageRequest`는 `@RestControllerAdvice`가 적용되지 않는 별도 경로라
     이번 범위에서 제외(필요해지면 `@MessageExceptionHandler`로 별도 처리 필요).
   - `UserControllerValidationTest`, `ProductControllerValidationTest`(멀티파트 `@RequestPart`
     경로에서도 `@Valid`가 실제로 동작하는지 별도 확인) 신규 작성.
   - Spring Boot 4.0.6 기준 Jackson이 `tools.jackson.*`(Jackson 3)로 바뀐 것을 테스트 작성
     중 확인(`com.fasterxml.jackson.databind.ObjectMapper` import 시 컴파일 에러 발생).

### 7) 카카오맵 거래 희망 장소 기능 + 전체 기능 실기동 점검
판매자가 상품 등록/수정 시 카카오맵으로 거래 희망 장소를 지정하고, 구매자가 상세 페이지에서
확인할 수 있는 기능을 신규 구현. 이후 회원가입(실제 이메일 인증 코드 발송까지)~로그인~
상품 등록/조회/검색/수정/삭제~찜하기~채팅~판매완료까지 브라우저로 전부 실기동 검증함.

- **백엔드**: `Product`에 `tradeLocationName`/`tradeLatitude`/`tradeLongitude` 추가(nullable —
  기능 생기기 전 상품은 null). `ProductCreateRequest`/`ProductUpdateRequest`에 필수값으로 검증
  추가, `ProductDetailResponse`에 포함. `ProductService.createProduct`/`updateProduct`에서
  `Product.updateTradeLocation(...)` 호출하도록 배선. `ProductServiceTest`에 저장/수정 검증
  케이스 추가.
- **프론트엔드**: `src/lib/kakaoMap.ts`(SDK 동적 로더, 중복 로드 방지 캐싱), `src/types/kakao-maps.d.ts`
  (최소 타입 선언 — 공식 `@types` 패키지 없음), `KakaoMapPicker`(등록/수정용, 클릭 → 마커 이동 →
  역지오코딩으로 주소 자동 채움, 직접 수정 가능), `KakaoMapView`(상세 페이지 읽기 전용 표시).
  `upload/page.tsx`·`products/[id]/edit/page.tsx`·`products/[id]/page.tsx`에 연결. 지도가 없는
  기존 상품은 섹션 자체가 안 보이도록 조건부 렌더링.
- **카카오 개발자 콘솔 이슈**: 최초 테스트 시 SDK가 403(`NotAuthorizedError: App disabled
  OPEN_MAP_AND_LOCAL service`)을 반환 — 콘솔에서 카카오맵 서비스 자체가 비활성화돼 있었음
  (코드 문제 아님). 사용자가 콘솔에서 활성화한 뒤 정상 동작 확인.
- **실기동 점검 중 발견해서 그 자리에서 고친 버그 3건**:
  1. **STOMP 인증이 실제로는 전혀 반영 안 되던 회귀** — 5번 항목에서 만든
     `StompAuthChannelInterceptor`가 deprecation 경고를 없애려고 `StompHeaderAccessor.wrap()`을
     쓰도록 되어 있었는데, 이게 Spring이 CONNECT 시점에 인증 결과를 세션에 등록하는 내부 콜백
     (`userChangeCallback`)과의 연결을 끊어버려서 **채팅 메시지 전송이 전부 NPE로 실패**하고
     있었음(유닛 테스트는 이 콜백을 검증하지 않아서 못 잡았음 — 브라우저로 실제 메시지를
     보내봐서 발견). `MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)`
     (deprecated지만 정확한 API)로 되돌리고, `userChangeCallback`이 실제로 호출되는지까지
     검증하는 테스트(`CONNECT_인증_결과가_userChangeCallback으로_세션에_등록된다`) 추가.
  2. **`/chat`, `/oauth/callback` 페이지가 프로덕션 빌드를 깨던 기존 버그** — `useSearchParams()`를
     Suspense로 안 감싸서 `npm run build`가 실패(이전엔 프로덕션 빌드를 한 번도 안 돌려봐서
     안 걸림). 두 페이지 다 내부 컴포넌트를 `<Suspense>`로 감싸서 해결.
  3. **지도 클릭 위치가 브라우저 GPS 위치로 덮어써지는 레이스 컨디션** — `KakaoMapPicker`가
     초기 로드시 `navigator.geolocation.getCurrentPosition`으로 현재 위치를 가져와 지도를
     이동시키는데, 사용자가 그보다 먼저 지도를 클릭해도 뒤늦게 도착한 GPS 콜백이 클릭한 위치를
     덮어써버렸음(서울을 클릭했는데 DB엔 강원도로 저장되는 걸로 발견). `hasUserPickedRef`
     플래그로 사용자가 이미 클릭했으면 GPS 콜백을 무시하도록 수정.
- 이번 점검에서 새로 찾았지만 이번 세션 범위 밖이라 아직 안 고친 것은
  `docs/BUGFIX_TODO.md`에 정리해둠.

### 8) 상품 수정 페이지 새로고침 시 로그인 화면으로 튕기는 버그 수정
`docs/BUGFIX_TODO.md`의 🔴 1번. `frontend/app/products/[id]/edit/page.tsx`가 `AuthContext`의
`isHydrated`를 기다리지 않고 `isLoggedIn`만으로 즉시 `/login`으로 리다이렉트해서, 새로고침/URL
직접 진입처럼 컴포넌트가 처음부터 마운트되는 경우 hydration 전 순간에 항상 리다이렉트가
발생했음. `useAuth()`에서 `isHydrated`를 추가로 꺼내 `!isHydrated`면 아무 것도 하지 않고
리턴하도록 수정(`frontend/app/upload/page.tsx`와 동일한 패턴). 로그인 후 상품 수정 페이지에서
새로고침해도 페이지가 유지되는지 브라우저로 재현/검증 완료.

### 9) 메인 페이지 조회 페이지네이션 없음 + SOLD_OUT 노출 문제 수정
`docs/SECURITY_REFACTOR_TODO.md`의 🟡 3번. 홈 화면(`frontend/app/page.tsx`)이 페이지네이션·
N+1 방지가 없는 `getAllProducts()`(`GET /api/products`)를 호출하고 있어서, 이미 그 문제가 해결돼
있던 `/api/products/search` 경로를 안 쓰고 있었음.
- **프론트**: `fetchProducts`/`handleReset`이 `getAllProducts()` 대신 `searchProducts({})`를
  호출하도록 변경. 더 이상 쓰이지 않는 `getAllProducts` 클라이언트 함수는 삭제(백엔드
  `GET /api/products` 엔드포인트 자체는 건드리지 않음 — 범위 밖).
- **백엔드**: `search()`(`ProductRepositoryImpl.eqStatus`)는 status 파라미터가 없으면 필터를
  아예 안 걸어서 SOLD_OUT 상품까지 노출되는 문제가 있었음. status가 null이면
  `product.status.in(SELLING, RESERVED)`로 기본 필터링하도록 수정(`findActiveProducts()`와
  동일한 기본값). status=SOLD_OUT을 명시적으로 요청하면 여전히 조회 가능.
- `ProductServiceTest`에 상태 미지정 시 판매완료 상품이 제외되고, 명시적으로 SOLD_OUT을
  요청하면 조회되는지 검증하는 케이스 추가. 브라우저로 홈 화면에서 SOLD_OUT 상품이 기본
  목록엔 안 보이고 "판매완료" 필터를 걸면 보이는지 재현/검증 완료.

### 10) 리뷰 중복 작성 차단
`docs/SECURITY_REFACTOR_TODO.md`의 🟡 4번. `ReviewRepository.existsByProductIdAndWriterId`가
정의만 돼 있고 `ReviewService.writeReview`에서 호출되지 않아 같은 거래에 리뷰를 여러 번 작성할
수 있었음. 구매자/상태 검증 다음, 리뷰 저장 앞에 중복 여부 가드를 추가해
`IllegalStateException("이미 리뷰를 작성한 거래입니다.")`를 던지도록 수정
(`GlobalExceptionHandler`가 기존에 이미 `IllegalStateException`을 409로 매핑하고 있어서 별도
예외 처리 추가는 불필요했음). `ReviewServiceTest` 신규 작성(같은 거래에 두 번째 리뷰 시도 시
거부되는지 검증). 실제 서버에 로그인 후 리뷰 작성 API를 두 번 호출해 1차는 200, 2차는 409로
거부되는지 curl로 재현/검증 완료(테스트로 만든 상품은 정리함).

### 11) 채팅방 중복 생성 레이스 수정
`docs/SECURITY_REFACTOR_TODO.md`의 📎 7번(`frontend/BACKEND_IMPROVEMENTS.md`에 이미 분석돼
있던 문제). `ChatService.createOrGetRoom`이 조회 후 없으면 생성하는 락 없는 Check-Then-Act라,
동시에 같은 (상품, 구매자) 조합으로 요청이 들어오면 채팅방이 여러 개 생성될 수 있었음.
- `ChatRoom`에 `(product_id, buyer_id)` unique 제약 추가(`ProductLike`와 동일한 패턴).
- 신규 `ChatRoomCreationService.getOrCreateRoom`(REQUIRES_NEW)으로 "조회 후 없으면 생성"을
  통째로 격리. `ChatService.createOrGetRoom`은 이걸 호출하고, `DataIntegrityViolationException`
  (경쟁에서 진 경우)을 잡으면 같은 메서드를 한 번 더 호출해 재시도.
- **디버깅 중 발견한 두 가지 함정**(둘 다 처음 구현엔 있었고 테스트로 잡아서 고침):
  1. 처음엔 조회를 outer 트랜잭션에서 먼저 하고 실패 시에만 재조회했는데, REPEATABLE READ
     스냅샷이 outer 트랜잭션 시작 시점에 고정돼서 재조회해도 경쟁에서 이긴 다른 트랜잭션이
     방금 커밋한 방이 안 보였음(재현 테스트에서 10개 중 9개가 실패). 조회+생성을 전부
     REQUIRES_NEW 안에서 하고 실패 시 그 메서드 전체를 재호출(=새 트랜잭션 재시작)하도록
     바꿔서 매 시도마다 스냅샷이 새로 잡히게 함.
  2. 그다음엔 `LazyInitializationException`(no session)이 남음 — REQUIRES_NEW 메서드가
     끝나며 세션이 닫히는데, "이미 존재하는 방"을 조회로 찾은 경우 `ChatRoom.product`/
     `seller`/`buyer`가 지연 로딩 프록시라 세션이 닫힌 뒤 `ChatRoomResponse` 생성자에서
     접근하면 터짐(방금 생성한 경우는 이미 로드된 엔티티를 그대로 써서 문제 없었음 — "조회로
     찾은 경우"에서만 재현됨). `ChatRoomRepository.findByProductIdAndBuyerIdWithAssociations`
     (JOIN FETCH)로 바꿔서 세션이 닫히기 전에 연관 엔티티를 실제로 채워오도록 수정.
- `ChatServiceTest`에 동시 요청 10개를 쏴서 채팅방이 정확히 1개만 생성되는지 검증하는 테스트
  추가(초기 구현에서 위 두 함정을 이 테스트가 실제로 잡아냄). 실제 서버에도 동일 상품/구매자로
  curl 10개를 동시에 쏴서 전부 같은 roomId를 받고 DB에도 행이 1개만 남는지 재현/검증 완료.

### 12) 코드베이스 전수 재점검 후 Critical·High 우선순위 4건 수정
`docs/`의 기존 TODO는 다 소화된 상태라, Explore 서브에이전트 2개(백엔드/프론트 각각)로
새로 전수 점검해서 문서에 없던 이슈를 찾고 Critical·High만 우선 수정함(Medium·Low는
`docs/REFACTOR_TODO_2.md`로 정리해 다음으로 미룸).

1. **[Critical] 죽은 컴포넌트 `SellerProfile.tsx` 삭제** — 아무데서도 import 안 되는데
   `localStorage` 키가 `'token'`으로 실제 쓰는 `'accessToken'`과 달라 연결돼도 항상 깨지는
   상태였고, `fetch("http://localhost:8080/...")`도 하드코딩돼 있었음.
2. **[Critical] 상품 상세 페이지의 하드코딩 fetch → 공용 axios 클라이언트로 교체**
   (`app/products/[id]/page.tsx`) — `handleStatusChange`/`handleDeleteProduct`가
   `NEXT_PUBLIC_API_URL`을 무시하고 `fetch("http://localhost:8080/...")`를 직접 호출해서
   배포 환경에서 항상 실패하는 상태였음(로컬 개발에서만 우연히 동작). `productApi.ts`에
   `updateProductStatus` 추가, 기존 `deleteProduct` 재사용하도록 교체 — axios 인터셉터의
   401 자동 로그아웃도 다시 적용됨. 겸사겸사 이 파일과 `userApi.ts`에 남아있던 디버깅용
   `console.log`(디코딩된 JWT payload 전체 출력 포함)도 정리.
3. **[High] JWT userId 디코딩 로직 3중 구현 → `useCurrentUserId` 훅으로 통합** —
   `chat/page.tsx`, `products/[id]/page.tsx`, `products/[id]/edit/page.tsx`가 각각 다른
   우선순위 키(`userId||sub` vs `userId||id||user_id||memberId||sub`)로 JWT를 독립적으로
   디코딩하고 있었음. 셋 다 UI 표시/조건부 렌더링용이라 서버 검증을 대체하는 게 아니라
   실제 버그는 없었지만 유지보수 리스크였음. `src/hooks/useCurrentUserId.ts`로 통합.
4. **[High] 상품 검색/목록 조회 N+1 완화** (`ProductRepositoryImpl.searchProducts`) —
   홈 화면/검색이 실제로 쓰는 이 메서드에만 형제 메서드(`findActiveProducts`/
   `findMyProducts`)와 달리 fetch join이 없어서 `ProductListResponse` 생성 시 `seller`
   지연 로딩이 상품마다 발생했음. `seller`(ManyToOne)에 fetch join 추가 — `images`
   (OneToMany)는 페이징과 함께 fetch join하면 Hibernate가 메모리에서 페이징을 적용해버리는
   문제가 있어 건드리지 않음(`application.yml`의 `default_batch_fetch_size=100`이 이미
   IN절로 일괄 조회해주고 있어 실질적으로는 크게 문제 없었음).
- 백엔드 전체 테스트(10개 클래스, 27건) 통과 확인. 프론트 `tsc --noEmit`, `eslint`(터치한
  파일에서 새 경고/에러 없음 확인 — 기존 6개는 이번 변경과 무관), `npm run build` 통과 확인.

### 13) 코드베이스 전수 재점검 Medium·Low 마무리
"12)"에서 찾은 8건(Medium 6, Low 2) 중 Critical·High는 이미 끝냈고, 이번에 Medium 6건 전부와
Low 4건(EmailService 삭제, aria-label, edit 페이지 죽은 state, next/image)까지 마무리함.
남은 Low 3건(테스트 커버리지 공백, application.yml 환경 분리, STOMP setTimeout 정리)은 선택
사항이거나 위험도가 있어 `docs/REFACTOR_TODO_2.md`에 남겨둠.

1. **[Medium] `GET /api/users/{userId}` 학번 노출 차단** — `UserPublicProfileResponse`(학번
   제외) 신설, 본인 조회(`/api/users/me`)만 기존 전체 응답 유지. 프론트의 죽은
   `getUserProfile(userId)` 함수(SellerProfile.tsx 삭제 이후 아무도 안 씀)도 같이 제거.
2. **[Medium] 이메일 인증코드 TTL(5분) 추가** — `EmailService`의 `verificationCodes`를
   `Map<String, String>` → `Map<String, VerificationEntry(code, expiresAt)>`로 변경. 만료 시
   검증 실패 처리 + 정리. `EmailServiceTest` 신규 작성(정상/오답/만료 케이스).
3. **[Medium] `Product`에 `(status, created_at)` / `category` 인덱스 추가** — 목록/검색 핫패스인데
   PK/FK 인덱스뿐이었음. `ddl-auto=update`로 로컬 DB에 실제 반영되는 것까지 `SHOW INDEX`로 확인.
4. **[Medium] 리뷰 신뢰점수 재계산을 `SELECT AVG(rating)` 쿼리로 교체** — 기존엔 `findByTarget`로
   판매자의 전체 리뷰를 로드해 Java에서 평균 계산. `ReviewRepository.findAverageRatingByTarget`
   추가. `ReviewServiceTest`에 리뷰 2건 평균이 정확한지 검증하는 케이스 추가.
5. **[Medium] 프론트 에러 처리(찜하기/채팅방 생성) `alert()` → 인라인 배너로 통일** —
   `products/[id]/page.tsx`의 `handleInitiateChat`/`handleToggleLike`, `me/page.tsx`의
   `handleToggleLike`가 `alert()`를 쓰던 걸 기존 `statusError`/`deleteError`와 같은 인라인
   배너 패턴으로 교체. 겸사겸사 `handleToggleLike`의 "자신의 상품" 에러 분기가
   `error.message`(AxiosError 기본 메시지)를 검사해서 실제로는 절대 안 걸리던 죽은 코드였던
   것도 발견해서 `error.response.data.message`를 읽도록 수정. 브라우저로 로그아웃 상태에서
   찜하기 클릭 시 alert 없이 배너가 뜨는 것 확인.
6. **[Medium] 프론트 Vitest 테스트 셋업 도입** — 테스트 프레임워크가 전무했음. `vitest.config.mts`
   (jsdom + `@vitejs/plugin-react`), `package.json`에 `test` 스크립트 추가.
   `useCurrentUserId.test.ts`(JWT 디코딩), `productApi.test.ts`(axios 클라이언트 호출 검증,
   최근 고친 "하드코딩 fetch" 버그류의 회귀 방지) 작성. STOMP는 컴포넌트 내장이라 이번 범위
   밖(추출 선행 필요).
7. **[Low] `infra/email/EmailService.java` 삭제** — `docs/SECURITY_REFACTOR_TODO.md` 🟢 5번,
   여전히 빈 죽은 클래스로 남아있던 것 확인 후 삭제.
8. **[Low] 아이콘 전용 버튼에 `aria-label` 추가** — 상품 상세의 ⋮ 관리 메뉴, 이미지 갤러리
   썸네일, 채팅 모바일 뒤로가기 버튼.
9. **[Low] 상품 수정 페이지 죽은 `formData` 필드 제거** — `tradeLocationName`/`tradeLatitude`/
   `tradeLongitude`/`remainingImageUrls`가 `formData`에 채워지기만 하고 실제 제출 시엔 별도
   state(`tradeLocation`, `remainingImageUrls`)에서 읽혀서 죽어있던 것 정리. 런타임 동작
   변경 없음(제출 로직은 원래도 별도 state를 썼음). 브라우저로 수정 페이지 진입/제출 확인.
10. **[Low] 백엔드가 서빙하는 상품 이미지를 next/image로 교체** — `ProductCard`, 상품 상세
    (메인+갤러리), 수정 페이지(기존 이미지)를 `fill`+`sizes`로 전환. 로컬 File 미리보기(blob:
    URL)는 최적화 대상이 아니라 raw `<img>` 유지. 전환 중 **Next.js 16의 SSRF 방지(private/
    loopback IP 업스트림 기본 차단)에 막혀 이미지가 전부 깨지는 걸 발견** —
    `remotePatterns`가 애초에 `localhost:8080`을 가리키고 있어서 로컬 환경에서 근본적으로
    동작 불가능한 설정이었음. 사용자 확인 후 `images.dangerouslyAllowLocalIP: true` 적용
    (이미지 URL이 항상 서버가 내려준 값이라 SSRF 공격 표면은 제한적이라고 판단). 실제 배포
    도메인이 생기면 `remotePatterns`를 그 도메인으로 바꾸고 이 옵션은 제거해야 함. 브라우저로
    홈/상세/수정 페이지 이미지 렌더링 확인.
- 매 항목마다 백엔드는 `./gradlew test`(관련 테스트 + 전체 스위트) 전부 통과 확인, 프론트는
  `npx tsc --noEmit`/`eslint`/`npm run build`(+신규 `npm run test`) 통과 확인. UI가 바뀐 항목은
  전부 브라우저(Chrome MCP)로 실기동 검증까지 완료.

## 진행 중 / 다음에 할 일

1. **`docs/BUGFIX_TODO.md`** — 🔴 1번은 완료. 남은 건 🟡 2번(리뷰 작성 프론트엔드 UI 부재,
   버그 아닌 미구현 기능 — 구현 여부는 아직 미결정).
2. `docs/SECURITY_REFACTOR_TODO.md` 참고. 🔴 Critical 2건, 🟡 3·4번(페이지네이션, 리뷰 중복
   작성 차단), 📎 7번(채팅방 중복 생성 레이스)까지 모두 완료. 남은 건 🟢 5·6번(선택)뿐.
3. **`docs/REFACTOR_TODO_2.md`** — Medium 6건, Low 4건 완료(위 "13)" 참고). 남은 건 선택/위험도
   있는 Low 3건(테스트 커버리지, application.yml 환경 분리, STOMP setTimeout 정리)뿐.
