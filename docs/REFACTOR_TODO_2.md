# 다음 작업: 코드베이스 전수 재점검에서 찾은 Medium·Low 우선순위 항목

`docs/REFACTOR_LOG.md`의 "12) 코드베이스 전수 재점검 후 Critical·High 우선순위 4건 수정"에서
이어지는 목록. Critical 2건, High 2건은 완료. 아래 Medium·Low는 아직 안 건드림.

## 🟡 Medium

### 1. `GET /api/users/{userId}` — 로그인만 하면 아무나 타인의 학번(`studentId`) 조회 가능
- `UserController.getUserProfile`이 `permitAll`이 아니라 인증은 필요하지만, 요청자가 본인인지
  검증하지 않고 `UserProfileResponse`(학번 포함)를 그대로 반환함. 캠퍼스 마켓 특성상 학번은
  개인식별정보에 가까움.
- 수정 방향: 본인 조회(`/api/users/me`)와 타인 조회를 분리하거나, 타인 조회 응답에서
  `studentId` 제외.

### 2. 이메일 인증코드가 만료시간 없이 무한정 유효 + 엔드포인트 무제한
- `domain/user/service/EmailService.java`가 `ConcurrentHashMap`에 코드를 저장하는데 타임스탬프가
  없어 덮어써질 때까지 영구히 유효함. `/api/users/emails/**`는 `permitAll` + 요청 제한 없음 →
  6자리 브루트포스(백만 경우의 수) 또는 임의 `@sj.sangji.ac.kr` 주소로 메일 스팸 가능.
- 기존 SECURITY_REFACTOR_TODO의 "Redis로 옮기면 좋음(선택)" 항목보다 우선순위가 높음 —
  단순 저장소 이전이 아니라 TTL(예: 5분) 추가가 핵심. Redis로 옮기면 TTL을 자연스럽게 구현 가능.

### 3. `Product.status`/`created_at` 등 조회 핫패스에 DB 인덱스 없음
- 유니크 제약(`ChatRoom`, `ProductLike`)만 있고 `@Index`는 전무. 목록/검색이 가장 많이 실행되는
  쿼리인데 status/created_at 조합 인덱스가 없어 테이블이 커지면 풀스캔.

### 4. 프론트 에러 처리 방식이 페이지마다 제각각
- 일부는 `errorMessage` state로 인라인 표시, 일부는 `alert()`. 일관된 패턴으로 통일 필요.

### 5. 프론트 테스트 셋업 자체가 없음
- jest/vitest 등 미설치, `*.test.*` 파일 0개. STOMP/JWT/API 로직 커버리지 전무.

### 6. 리뷰 작성 시 신뢰점수 재계산이 매번 전체 리뷰를 Java로 로드
- `ReviewService.updateUserTrustScore`가 `findByTarget(user)`로 전체 리뷰를 불러와 평균을
  계산함. `SELECT AVG(rating)` 쿼리 하나로 대체 가능.

## 🟢 Low

- `infra/email/EmailService.java` 여전히 빈 죽은 클래스 — 삭제만 하면 됨
  (`docs/SECURITY_REFACTOR_TODO.md` 🟢 5번과 동일 항목, 아직 미완료 확인됨).
- 테스트 커버리지 전무: `JwtTokenProvider`, 카카오 OAuth 로그인 전체 경로
  (`CustomOAuth2UserService`/`OAuth2SuccessHandler`), `ChatRoomController`,
  `ProductLikeService`/`ProductCacheService`, `ReviewController`, `AiService`(Gemini API 실패 시
  "fail open"으로 검열 없이 상품 등록이 통과되는 동작도 포함).
- `application.yml` 하나로 환경 분리 없음 — 개발자 로컬 절대경로(`file.upload-dir`)가
  하드코딩돼 있고 `ddl-auto: update`가 무조건 적용됨. 다른 환경에 배포하면 조용히 깨짐.
- `next/image`용 `remotePatterns` 설정은 돼 있는데 실제로는 전부 raw `<img>` 사용 중.
- 아이콘 전용 버튼(⋮ 드롭다운, 채팅 뒤로가기 등)에 `aria-label` 없음.
- `app/chat/page.tsx`의 STOMP 구독 시퀀싱이 `setTimeout(fn, 0/100)` 기반 — 폴백 로직이 있어
  실제로 깨지진 않지만 `onConnect` 콜백만으로 처리하도록 정리하면 더 단순해짐.
- `app/products/[id]/edit/page.tsx`의 `formData.tradeLocationName/tradeLatitude/tradeLongitude`가
  별도 `tradeLocation` state와 중복 동기화되는데 제출 시 `tradeLocation`만 실제로 사용됨 — 혼란
  스러운 죽은 state.

## 재개할 때 체크리스트
1. 로컬 MySQL/Redis 떠 있는지 확인(`nc -z localhost 3306`, `nc -z localhost 6379`; Redis는
   `brew services start redis`)
2. `docs/REFACTOR_LOG.md`의 "12)" 항목 읽고 지금까지 뭘 했는지 파악
3. 이 파일의 🟡 Medium부터 우선순위 순서대로 진행, 🟢 Low는 선택
4. 각 항목 수정 후 `./gradlew test`(백엔드) / `npx tsc --noEmit && npm run build`(프론트)로
   검증
5. 완료되면 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거(비면 파일 삭제), 커밋
