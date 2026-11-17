# 프론트엔드 Lighthouse 측정 기록

프론트 화면 리팩토링(브랜치 `feat/frontend-redesign`) 전후의 성능, 접근성 수치를 비교하기 위한 기록이다.
지금까지 이 프로젝트에는 프론트 성능 수치 기록이 없었다. 그래서 리팩토링 전 측정값이 첫 기준선이 된다.
**전후 비교는 아래 조건을 똑같이 맞췄을 때만 의미가 있다.** 조건을 바꿔야 하면 이 문서에 먼저 적고, 전과 후를 둘 다 다시 잰다.

## 1. 측정 조건 (2026-11-17 확정, 전후 동일 적용)

### 로컬 실행에 필요한 것
| 구성 | 필요 여부 | 비고 |
|---|---|---|
| MySQL 8 (`campus_market`) | 필요 | 로컬 설치본 사용 |
| Redis (6379) | 필요 | 목록 캐시(`products`, TTL 10분). 측정 때는 영속화를 끄고 띄움(`--save "" --appendonly no`) |
| Elasticsearch | 불필요 | 백엔드에 의존성 없음. 목록은 MySQL(QueryDSL) + Redis 캐시 |
| OpenAI 키 | 불필요 | AI 설명 생성은 Gemini(`GEMINI_API_KEY`)를 쓴다. 측정 화면에서는 호출하지 않음 |
| 카카오맵 JS 키 | 필요 | `frontend/.env.local`의 `NEXT_PUBLIC_KAKAO_MAP_KEY`. 상세, 등록, 수정 화면의 지도 |
| 카카오 OAuth | 측정에는 불필요 | 로그인 화면의 "카카오로 시작하기" 버튼은 이동만 한다 |
| Gmail 앱 비밀번호 | 측정에는 불필요 | 회원가입 인증번호 발송용 |

- 라이브 데모 백엔드는 꺼져 있어서 모두 로컬에서 잰다.

### 빌드와 서버
- **프로덕션 빌드**로 잰다: `cd frontend && npm run build && npm run start` (http://localhost:3000). 개발 서버는 번들이 압축되지 않아서 쓰지 않는다.
- 백엔드는 `cd backend && ./gradlew bootRun`(http://localhost:8080)으로 띄운다. `/products/[id]` 측정에 필요하다.

### 측정 도구
- **Lighthouse CLI 13.6.0**(`npx lighthouse@13.6.0`) + **Chrome 157**(headless). InsightStock 측정과 같은 버전이다.
- Lighthouse CLI는 측정할 때마다 임시 프로필로 새 Chrome을 띄운다. 캐시, 쿠키, localStorage가 비어 있어서 **비로그인 상태**로 잰다.
- **모바일**: Lighthouse 기본값(Moto G Power 에뮬레이션, 느린 4G, CPU 4배 감속). **데스크톱**: `--preset=desktop`.
- 카테고리: Performance, Accessibility, Best Practices, SEO.
- **페이지마다 모바일과 데스크톱 각각 3회씩 재서 중앙값**을 기록한다.
- 측정 중에는 빌드나 테스트 같은 무거운 작업을 동시에 돌리지 않는다.

```bash
npx lighthouse@13.6.0 http://localhost:3000/ \
  --only-categories=performance,accessibility,best-practices,seo \
  --chrome-flags="--headless=new" \
  --output=json --output=html --output-path=<저장경로>/<페이지>-mobile-1
# 데스크톱은 --preset=desktop 추가
```

### 측정 환경
- Apple M3, 메모리 16GB, macOS 26.1
- Node 26.5.0, Next.js 16.2.4, React 19.2.4

### 측정 페이지
| 페이지 | URL | 백엔드 필요 |
|---|---|---|
| 메인(상품 목록) | http://localhost:3000/ | 예(검색 API) |
| 로그인 | http://localhost:3000/login | 아니오 |
| 상품 상세 | http://localhost:3000/products/898 | 예(상세 API, 카카오맵) |

- 상세 페이지는 데모 상품 "아이패드 에어 5세대 64GB"(id 898)로 고정한다.
- 로그인이 필요한 화면(`/upload`, `/chat`, `/me`, `/products/[id]/edit`)은 Lighthouse가 로그인 상태를 재현하지 못한다. 그래서 점수 대신 캡처와 브라우저 확인으로 비교한다.

### 데모 데이터 (전후 캡처 공통)
- 기존 로컬 데이터는 테스트 문구와 "이미지 없음" 상품이 대부분이었다. 그래서 전 캡처 전에 데모 데이터를 만들었다(2026-11-17).
  - 계정 2개: `demo-seller@sj.sangji.ac.kr`(데모판매자), `demo-buyer@sj.sangji.ac.kr`(데모구매자)
    - 회원가입은 학교 메일 인증번호가 필요하다. 그래서 로컬 DB에 BCrypt 해시로 직접 넣었다.
    - 학번은 `DEMO0001`, `DEMO0002`(가짜 값)이다. 비밀번호는 저장소에 남기지 않는다.
  - 상품 10개: 판매자 계정으로 등록 API를 호출해 등록했다(이미지, 거래 장소 포함).
    - 예약중 1개(전기포트), 판매완료 1개(공학용 계산기)
  - 찜 3개, 채팅방 2개(아이패드 방에 지난 메시지 3건)
- 상품 사진: Unsplash 라이선스 사진 10장을 1080×1080으로 잘라 썼다. 출처(사진 ID): LUGuCtvlk1Q, 2qKrvxgPsQ8, AgLMrojqjAM, JcQu6hlBUs8, 3A4XZUopCJA, Lpqg7ypu2B4, mQ4dmENW2sM, KYw1eUx1J7Y, j4uuKnN43_M, x2Z0uNj-Quo
- 기존 로컬 데이터(상품 21개)는 지우지 않았다. 목록은 최신순이라 데모 상품 10개가 먼저 나오고, 그 뒤에 기존 상품이 이어진다.

### 원본 리포트 보관
- 원본 JSON, HTML은 저장소에 넣지 않는다. 로컬 `lighthouse-reports/`(gitignore)에 둔다. 이 문서에는 중앙값만 옮긴다.

### 캡처 조건
- Playwright 1.65.1 + 설치된 Chrome(headless)을 쓴다. 화면마다 새 브라우저 컨텍스트를 연다.
- 로그인 화면은 테스트 계정 토큰을 `localStorage.accessToken`에 넣어 연다.
- 데스크톱 1440×900, 모바일 375×812(배율 2)로 전체 페이지를 찍는다. 페이지를 연 뒤 2.5초 기다린다(지도, STOMP 연결, 이미지).
- 9개 화면: `/`, `/login`, `/signup`, `/oauth/callback`, `/upload`, `/products/898`(구매자, 판매자), `/products/898/edit`, `/chat`(목록, 대화방), `/me`

## 2. 리팩토링 전 (기준선, 2026-11-17 측정)

조건은 1절 그대로다. 3회 측정 중앙값이고, Performance는 3회 값을 모두 적었다.

| 페이지 | 기기 | Performance(3회) | Accessibility | Best Practices | SEO | FCP | LCP | TBT | CLS | 전송량 |
|---|---|---|---|---|---|---|---|---|---|---|
| `/` | 모바일 | 75 / 75 / 75 | 96 | 100 | 100 | 0.76초 | **13.51초** | 20ms | 0 | 2,361KB |
| `/` | 데스크톱 | 87 / 87 / 88 | 96 | 100 | 100 | 0.21초 | 2.41초 | 0ms | 0 | 2,368KB |
| `/login` | 모바일 | 75 / 100 / 75 | 94 | 100 | 100 | 0.75초 | **12.60초** | 18ms | 0 | 2,246KB |
| `/login` | 데스크톱 | 89 / 100 / 100 | 94 | 100 | 100 | 0.20초 | 0.21초 | 0ms | 0 | 2,246KB |
| `/products/898` | 모바일 | 75 / 75 / 75 | 95 | 81 | 100 | 0.76초 | **13.36초** | 23ms | 0.002 | 2,675KB |
| `/products/898` | 데스크톱 | 88 / 88 / 88 | 95 | 77 | 100 | 0.37초 | 2.27초 | 0ms | 0.041 | 2,456KB |

### 성능: 원인은 글꼴 파일 하나
- 세 페이지 모두 가장 큰 요청은 **`PretendardVariable.woff2` 2,010KB**다. 2026-08 UI 리뉴얼(`90eb2d0`)에서 `next/font/local`로 넣은 전체 가변 글꼴이다.
  - 한글 전 글자와 굵기 45~920 전체가 파일 하나에 들어 있다.
  - `next/font`가 모든 페이지에서 미리 불러온다(preload).
- 텍스트만 있는 `/login`도 전송량이 2,246KB이고, 그중 약 90%가 이 글꼴이다.
- FCP는 0.75초로 빠르다. 하지만 `font-display: swap`으로 OS 글꼴로 먼저 그린 뒤, 느린 4G에서 2MB가 다 받아지면 글자를 다시 그린다. 이 시점이 LCP로 기록된다(모바일 12.6~13.5초).
- `/login` 모바일 Performance가 75/100/75로 흔들린다. 3회 중 1회는 글꼴 도착 전에 LCP가 확정된 것으로 보인다.
- InsightStock에서 IBM Plex Sans KR을 넣었을 때 LCP가 0.76초에서 8.95초로 늘어난 것과 같은 현상이다. 이 프로젝트에는 이미 들어가 있었다.

### 접근성 감점 원인
| 페이지 | 항목 | 위치와 값 |
|---|---|---|
| 공통 | `color-contrast` | 코랄 버튼(헤더 "상품 등록", 로그인 제출, "채팅 문의하기") 흰 글자 / `#ff6b45` **2.82** |
| `/` | `color-contrast` | 결과 개수 `#a89c8b` / 크림 `#fbf8f4` **2.54**, 카드 판매자 이름과 조회·찜 수 `#a89c8b` / 흰색 **2.69**, "이미지 없음" `#a89c8b` / `#f5efe5` **2.35** |
| `/login` | `color-contrast` | 왼쪽 코랄 패널의 제목, 목록, 작은 글자(흰색 70%) **1.93~2.82** |
| `/login` | `landmark-one-main` | `<main>`이 없다 |
| `/products/898` | `color-contrast` | 판매자 신뢰도, 보조 문구 `#a89c8b` / 크림 **2.54** |
| `/products/898` | `presentation-role-conflict` | 카카오맵 SDK가 만드는 `<area>` 요소(외부 SDK라 직접 고칠 수 없음) |

### Lighthouse가 잡지 못한 대비 문제 (토큰 값으로 계산)
| 위치 | 전: 색 → 대비 | 기준 |
|---|---|---|
| 입력창 경계(배경 `#f5efe5`, 테두리 투명) / 흰 카드 | **1.14** | 3.0 (UI 경계) |
| 입력창 placeholder `#a89c8b` / `#f5efe5` | **2.36** | 4.5 |
| 필터 칩 테두리 `#e8e0d3` / 흰색 | **1.31** | 3.0 (UI 경계) |
| 판매완료 배지 `#a89c8b` / `#f5efe5` | **2.36** | 4.5 |
| 입력창 포커스 테두리(코랄) / 흰색 | **2.82** | 3.0 (UI 경계) |
| 본문 `#241c15` / `#fbf8f4` | 16.0 ✓ | 4.5 |
| 판매중 배지 `#0f5c3d` / `#dcf3e7` | 6.88 ✓ | 4.5 |
| 예약중 배지 `#6b4308` / `#fbedd3` | 7.47 ✓ | 4.5 |

### Best Practices 감점 (`/products/898`)
- 카카오맵 SDK 관련 항목이다. 지도 타일을 `http://mts.kakaocdn.net`에서 받는다. 외부 SDK 동작이라 이번 범위에서 고치지 않는다.

### 캡처로 확인한 화면 문제 (375px)
- 로그인한 화면 전부(8개)에서 가로 스크롤이 생긴다(문서 폭 397px). 헤더의 "Campus Market"이 두 줄로 꺾이고, 오른쪽 "로그아웃"이 화면 밖으로 밀린다.
- 참고: 상품 상세 모바일 전체 페이지 캡처에서는 하단 "채팅 문의하기" 바가 설명 문단 위에 찍힌다. 이 바는 `position: fixed`이고 본문 아래에 `pb-28` 여백이 있어서, 실제로 스크롤할 때는 가리지 않는다. 전체 페이지 캡처 방식에서 생기는 현상이다. 후 캡처에서는 화면 높이 캡처를 함께 남긴다.
- 콘솔 에러: 22장 모두 0건.
