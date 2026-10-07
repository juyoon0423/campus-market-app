# 🛒 캠퍼스 마켓 (Campus Market) - Frontend

> 프로젝트 전체 소개는 [루트 README](../README.md)를 참고하세요.

> **대학생 중고거래 플랫폼을 위한 Next.js 기반 프론트엔드 애플리케이션**
>
> JWT/OAuth2 인증, 실시간 채팅(WebSocket), 상품 관리 기능을 구현하였으며,
> SSR/CSR 환경에서 발생하는 인증 및 상태 관리 문제를 해결하며 사용자 경험을 개선했습니다.

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/TailwindCSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" />
  <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge" />
  <img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge" />
</p>

---

## 🏗️ Architecture

서비스는 Next.js(App Router) 기반으로 구현되었으며 Spring Boot API 서버와 통신합니다.

| Layer     | Technology           | Role                       |
| --------- | -------------------- | -------------------------- |
| Frontend  | Next.js (App Router) | CSR/SSR 렌더링, UI 구성    |
| State     | React Context API    | 인증 상태 관리             |
| Network   | Axios                | API 통신 및 JWT 자동 처리  |
| Real-time | SockJS, STOMP        | 실시간 채팅                |
| Backend   | Spring Boot          | 비즈니스 로직 및 인증 처리 |

---

## 🛠️ Tech Stack

### Frontend

- **Framework:** `Next.js 16 (App Router)`
- **Language:** `TypeScript`
- **Styling:** `Tailwind CSS`
- **State Management:** `React Context API`
- **HTTP Client:** `Axios`
- **Real-time Communication:** `SockJS`, `STOMP`
- **Icons:** `Lucide React`

---

## ✨ Key Features

| 기능                 | 기술적 포인트                                           |
| :------------------- | :------------------------------------------------------ |
| 🔒 OAuth2 & JWT 인증 | Axios Interceptor 기반 토큰 자동 주입 및 인증 상태 관리 |
| 📦 상품 관리         | FormData 기반 이미지 및 상품 정보 동시 처리             |
| ❤️ 찜하기 기능       | Optimistic UI 적용으로 즉각적인 사용자 피드백 제공      |
| 💬 실시간 채팅       | STOMP + WebSocket 기반 1:1 채팅                         |
| 👤 마이페이지        | 탭 기반 사용자 활동 내역 관리                           |
| 🔍 상품 검색         | 키워드 및 카테고리 기반 필터링                          |

---

## 🔍 Troubleshooting

### 1️⃣ Next.js SSR 환경에서 JWT 인증 처리

#### 문제

새로고침 시 사용자의 `isLiked` 상태가 초기화되는 문제가 발생했습니다.

Server Component 단계에서는 브라우저의 `localStorage`에 접근할 수 없었기 때문입니다.

#### 해결

인증이 필요한 데이터 요청을 Client Component 내부의 `useEffect`로 이동하여 브라우저 마운트 이후 토큰을 읽어 API 요청을 수행하도록 변경했습니다.

---

### 2️⃣ Multipart/FormData Boundary 문제

#### 문제

상품 수정 요청 시 백엔드에서 Multipart 데이터를 정상적으로 파싱하지 못했습니다.

#### 해결

Axios에서 `Content-Type: multipart/form-data`를 직접 설정하지 않고 브라우저가 Boundary를 자동 생성하도록 수정했습니다.

---

### 3️⃣ Boolean 직렬화 필드명 불일치

#### 문제

백엔드의 `isLiked` 값이 프론트엔드에서 정상적으로 매핑되지 않았습니다.

#### 해결

네트워크 분석을 통해 직렬화 과정에서 필드명이 변경되는 것을 확인하고 백엔드와 협업하여 `@JsonProperty("isLiked")`를 적용했습니다.

---

### 4️⃣ App Router 환경에서 roomId 초기화 문제

#### 문제

채팅방 진입 시 `roomId`가 `NaN`으로 전달되어 API 호출 오류가 발생했습니다.

#### 해결

`useParams()` 값에 대한 Guard Logic을 추가하여 유효한 roomId가 존재할 때만 API 요청과 WebSocket 연결이 수행되도록 수정했습니다.

---

### 5️⃣ STOMP 기반 채팅에서 한글 입력(IME) 중복 전송

#### 문제

한글 입력 후 Enter 입력 시 메시지가 두 번 전송되는 문제가 발생했습니다.

#### 해결

`e.nativeEvent.isComposing` 상태를 검사하여 조합 중인 입력 이벤트를 무시하도록 처리했습니다.

---

### 6️⃣ API 응답 타입 분리 및 컴포넌트 재사용성 개선

#### 문제

상품 목록 조회와 상세 조회 API의 이미지 필드 구조가 달라 컴포넌트 재사용이 어려웠습니다.

#### 해결

`ProductListResponse`, `ProductDetailResponse` 타입을 분리하고 Type Guard를 활용하여 안전하게 렌더링하도록 개선했습니다.

---

## 🚀 Getting Started

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경 변수 설정

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_ENDPOINT=http://localhost:8080/ws-stomp
NEXT_PUBLIC_KAKAO_MAP_KEY=your-kakao-javascript-key
```

### 3. 개발 서버 실행

```bash
npm run dev
```

---

## 🔗 Related Links

백엔드는 별도 저장소가 아닌 같은 모노레포의 `backend/` 디렉터리입니다. 전체 구조와 실행 방법은 [루트 README](../README.md)를 참고하세요.

---

<p align="center">
  <b>Spring Boot 기반 백엔드 서버와 연동하여 동작합니다.</b>
</p>
