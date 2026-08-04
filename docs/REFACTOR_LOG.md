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

## 진행 중 / 다음에 할 일

`docs/SECURITY_REFACTOR_TODO.md` 참고. 🔴 Critical 2건은 완료했고, 다음은 🟡 3(메인 페이지
페이지네이션/N+1), 4(리뷰 중복 작성 차단) 순서로 진행하면 됨. 이번 세션에서 새로 찾은
"채팅방 중복 생성 레이스"(`TODO`의 7번 항목)는 아직 미착수.
