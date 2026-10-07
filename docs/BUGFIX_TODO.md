# 다음 작업: 카카오맵 기능 실기동 점검 중 발견한 버그/미완성 기능

`docs/REFACTOR_LOG.md`의 "7) 카카오맵 거래 희망 장소 기능 + 전체 기능 실기동 점검"에서 이어지는
목록. 그 세션에서 발견한 버그 3건은 이미 고쳤고(회귀 테스트 포함), 🔴 1번도 이번 세션에서 완료함
(`docs/REFACTOR_LOG.md`의 "8)" 참고). 아래 2번은 범위 밖이라 아직 안 건드림.

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
3. `docs/REFACTOR_LOG.md`의 "7)", "8)" 항목 읽고 지금까지 뭘 했는지 파악
4. 이 파일의 🟡 2번(리뷰 작성 UI) 구현 여부부터 결정하고 진행
5. 완료 후 `docs/REFACTOR_LOG.md`에 완료 기록 추가하고 이 파일에서 항목 제거(비면 파일 삭제), 커밋

## 참고 — 이번 세션에서 만든 로컬 테스트 계정 (DB에 남아있음, 필요 없으면 정리)
- 판매자/구매자용 로컬 테스트 계정 2개
