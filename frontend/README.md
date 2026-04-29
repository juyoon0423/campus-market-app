# 🛒 캠퍼스 마켓 (Campus Market)
> **대학생들을 위한 스마트한 중고거래 플랫폼** > Next.js와 Spring Boot를 연동하여 실무 수준의 JWT 인증 및 상품 관리 시스템을 구축한 프로젝트입니다.

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" />
</p>

---

## 🏗️ Architecture Diagram

서비스의 전체적인 흐름은 다음과 같습니다. 사용자의 요청은 Next.js 서버를 거쳐 Spring Boot API 서버와 통신하며, 모든 데이터는 MySQL에 안전하게 저장됩니다.

| Layer | Technology | Role |
| :--- | :--- | :--- |
| **Frontend** | **Next.js (App Router)** | CSR/SSR 최적화, Axios 인터셉터 기반 JWT 관리 |
| **Backend** | **Spring Boot** | RESTful API 제공, Spring Security 보안 인증 |
| **Database** | **MySQL** | 상품 정보, 유저 데이터, 이미지 경로 저장 |

---

## 🛠️ Tech Stack

### Frontend
- **Framework:** `Next.js 14+`
- **Styling:** `Tailwind CSS`
- **State Management:** `React Context API` (Auth)
- **HTTP Client:** `Axios`
- **Icons:** `Lucide React`

### Backend (Core)
- **Language:** `Java 17`
- **Framework:** `Spring Boot 3.x`
- **Security:** `Spring Security`, `JWT`

---

## ✨ Key Features

| 기능 | 상세 설명 |
| :--- | :--- |
| **🔒 JWT 인증** | 로그인 시 발급받은 토큰을 LocalStorage 및 Axios 헤더에 자동 등록하여 보안 유지 |
| **📦 상품 관리** | MultipartFile을 활용한 상품 등록(이미지 업로드), 수정, 삭제 기능 |
| **🔍 지능형 검색** | 키워드 및 카테고리별 실시간 필터링 시스템 |
| **👤 마이페이지** | 현재 로그인된 사용자의 판매 내역 및 프로필 실시간 연동 |

---

## 🔍 Troubleshooting (문제 해결 과정)

> 프로젝트를 진행하며 겪은 기술적 난관과 이를 극복한 과정을 기록합니다.

### 1️⃣ 이미지 렌더링 시 DTO 필드명 불일치
- **문제:** 상세 조회(`imageUrls` 배열)와 목록 조회(`representativeImageUrl` 단일 필드)의 API 응답 필드명이 달라 메인 화면에서 사진이 보이지 않음.
- **해결:** TypeScript 인터페이스를 API별로 정교하게 분리하고, 컴포넌트 내에서 조건부 렌더링을 적용하여 데이터 구조에 맞는 정확한 필드 접근을 구현하였습니다.

### 2️⃣ JWT 인증 및 전역 상태 관리
- **문제:** 페이지 전환 시마다 인증 상태를 체크해야 하는 번거로움과 보안 통신 설정의 어려움.
- **해결:** `AuthContext`를 구축하여 유저 정보를 전역 상태화하고, **Axios Interceptor**를 도입하여 모든 요청 헤더에 `Bearer` 토큰이 자동으로 포함되도록 중앙 집중식 인증 구조를 확립하였습니다.

### 3️⃣ Next.js 외부 이미지 도메인 보안 정책
- **문제:** 보안상의 이유로 `localhost:8080` 리소스를 차단하는 Next.js 이미지 최적화 정책.
- **해결:** `next.config.mjs`에 `remotePatterns` 설정을 추가하여 특정 백엔드 도메인의 이미지 리소스를 허용하도록 설정하였습니다.

### 4️⃣ 비동기 렌더링 시 roomId 유효성 검증 및 API 호출 최적화
- **문제**: Next.js App Router의 `useParams()`가 초기 렌더링 시 빈 객체를 반환하면서, 채팅방 ID가 `NaN` 또는 `undefined`로 백엔드에 전달되어 API 호출 에러(`MethodArgumentTypeMismatchException`)가 발생하는 이슈.
- **해결**: 
  - **입구 컷(Guard Logic)**: `useEffect` 내에서 `roomId`의 존재 여부와 숫자 타입을 검증하는 조건을 강화하여 유효한 데이터가 확보된 시점에만 WebSocket 연결 및 데이터 로딩이 시작되도록 로직을 개선하였습니다.
  - **연결 자동화**: `useChat` 커스텀 훅을 설계하여 `roomId` 변화를 감지하고, 방 이동 시 기존 WebSocket 세션을 자동으로 해제(Cleanup)한 뒤 새 연결을 맺도록 라이프사이클을 관리하였습니다.

### 5️⃣ WebSocket 순환 참조로 인한 JSON 파싱 에러 대응
- **문제**: 백엔드 엔티티의 양방향 연관관계로 인해 채팅 메시지 수신 시 프론트엔드에서 `JSON.parse` 에러가 발생하거나 브라우저 메모리가 급증하는 현상.
- **해결**: 백엔드와 협업하여 순환 참조를 제거한 **`ChatMessageResponse` DTO** 규격을 정의하였습니다. 데이터 구조를 단순화함으로써 클라이언트 사이드에서의 파싱 속도를 향상시키고 불필요한 메모리 점유를 방지하였습니다.
- 
---

## 🚀 Getting Started

### 1. 의존성 설치
```bash
npm install
```

### 2. 환경 설정(.env.local)
```코드 스니펫
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. 개발 서버 실행
```bash
npm run dev
```

---

## 🔗 Related Links

본 프론트엔드와 연동되어 데이터 처리를 담당하는 **백엔드(Spring Boot)** 소스코드 레포지토리입니다.

| 프로젝트 | 레포지토리 링크 | 기술 스택 |
| :--- | :--- | :--- |
| **Campus Market Backend** | [![Backend Repo](https://img.shields.io/badge/Backend-GitHub-green?style=flat-square&logo=github)](https://github.com/juyoon0423/campus-market-backend) | `Java`, `Spring Boot`, `MySQL` |

---

<p align="center">
  <b>백엔드 서버 가동 후 프론트엔드 개발 서버를 실행해야 정상적인 데이터 연동이 가능합니다.</b>
</p>

---
© 2026 Campus Market Project. Developed by juyoon0423.
