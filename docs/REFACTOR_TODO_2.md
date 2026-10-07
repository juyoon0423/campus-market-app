# 다음 작업: 남은 Low 우선순위 3건 (선택)

`docs/REFACTOR_LOG.md`의 "13) 코드베이스 전수 재점검 Medium·Low 마무리"에서 이어지는 목록.
Medium 6건, Low 4건은 완료. 아래 3건만 남음 — 전부 "선택"이거나 위험도가 있어 일부러 미룸.

## 🟢 Low — 선택/위험도 있음

### 1. 테스트 커버리지 공백 보강
- 여전히 테스트가 없는 곳: `JwtTokenProvider`, 카카오 OAuth 로그인 전체 경로
  (`CustomOAuth2UserService`/`OAuth2SuccessHandler`), `ChatRoomController`,
  `ProductLikeService`/`ProductCacheService`, `ReviewController`, `AiService`(Gemini API 실패 시
  "fail open"으로 검열 없이 상품 등록이 통과되는 동작도 포함).
- 프론트는 vitest 셋업이 생겼으니(`useCurrentUserId.test.ts`, `productApi.test.ts` 참고) 같은
  패턴으로 늘려가면 됨. STOMP 로직(`app/chat/page.tsx`)은 컴포넌트에 내장돼 있어 테스트하려면
  먼저 훅으로 추출하는 선행 작업이 필요함.

### 2. `application.yml` 환경 분리 검토 — 위험도 높음
- 개발자 PC 로컬 업로드 경로(`file.upload-dir`)가 하드코딩돼
  있고 `ddl-auto: update`가 무조건 적용됨. 다른 환경에 배포하면 조용히 깨짐.
- 이전 세션(`docs/REFACTOR_LOG.md` "5)")에서 `/api/dummy/**` 인증 문제를 다룰 때, 프로필 분리
  (`@Profile("local")` 등)를 적용하려다 "이 프로젝트엔 프로필 분리 인프라가 전혀 없어서
  (`application-local.yml`, `SPRING_PROFILES_ACTIVE` 설정 등 전무) 그대로 적용하면 로컬 실행
  방식 자체를 바꿔야 하는 부작용이 있다"고 판단해 다른 방법으로 우회한 전례가 있음. 이번에도
  같은 클래스의 문제 — 로컬 개발 워크플로 자체를 바꾸는 작업이라 신중하게 접근 필요.

### 3. `app/chat/page.tsx`의 STOMP 구독 시퀀싱 정리 — 위험도 있음
- `setTimeout(fn, 0/100)` 기반으로 연결 이후 구독을 순서대로 처리하고 있음. `pendingSubscribeRoomIdRef`
  폴백이 있어 실제로 깨지진 않지만, `onConnect` 콜백만으로 처리하도록 정리하면 더 단순해짐.
- 실시간 채팅 특성상 회귀가 생기면 브라우저로 직접 재현하지 않으면 잡기 어려움 — 수정 후 반드시
  실제 메시지 송수신을 브라우저로 검증해야 함.

## 재개할 때 체크리스트
1. 로컬 MySQL/Redis 떠 있는지 확인(`nc -z localhost 3306`, `nc -z localhost 6379`; Redis는
   `brew services start redis`)
2. `docs/REFACTOR_LOG.md`의 "12)", "13)" 항목 읽고 지금까지 뭘 했는지 파악
3. 위 3건 중 필요한 것만 선택해서 진행(전부 선택 사항)
4. 각 항목 수정 후 `./gradlew test`(백엔드) / `npx tsc --noEmit && npm run build && npm run
   test`(프론트)로 검증, UI 변경은 브라우저로 실기동 확인
5. 완료되면 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거(비면 파일 삭제), 커밋
