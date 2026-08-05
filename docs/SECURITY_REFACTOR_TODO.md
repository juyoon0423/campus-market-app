# 다음 작업: "팀장 리뷰" 기준 필수 리팩토링 2차

`docs/REFACTOR_LOG.md`의 "4) 팀장 리뷰 필수 리팩토링 1차"에 이어지는 목록. 🔴 Critical 1, 2번,
🟡 3번(메인 페이지 페이지네이션), 4번(리뷰 중복 작성 차단), 📎 7번(채팅방 중복 생성 레이스)은
완료(`docs/REFACTOR_LOG.md`의 "5)", "9)", "10)", "11)" 참고). 아래 🟢 5, 6번(선택)만 남음.

## 🟢 낮음 — 선택

### 5. `infra/email/EmailService.java` 죽은 코드
- `LocalFileStorage.java`와 동일 패턴의 빈 클래스. 삭제만 하면 됨.

### 6. 이메일 인증코드 인메모리 저장
- `domain/user/service/EmailService.java`가 인증코드를 `ConcurrentHashMap`에 저장(코드 자체
  주석에 "실무에서는 Redis 권장"이라고 적혀 있음). Redis는 이미 프로젝트에 있으니 옮기면
  일관성은 좋아지지만, 지금 단일 인스턴스 규모에선 급하지 않음.

## 재개할 때 체크리스트
1. 로컬 MySQL/Redis 떠 있는지 확인(`nc -z localhost 3306`, `nc -z localhost 6379`)
2. `docs/REFACTOR_LOG.md` 읽고 지금까지 뭘 했는지 파악
3. 이 파일의 🟢 5번부터 순서대로 진행(둘 다 선택 사항 — 급하지 않으면 스킵 가능)
4. 각 항목 수정 후 `./gradlew test`로 전체 테스트 통과 확인(가능하면 새 항목마다 검증 테스트 추가)
5. 완료되면 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거(둘 다 비면 파일 삭제), 커밋
