# 🛒 캠퍼스 마켓 (Campus Market) - Frontend
> **대학생들을 위한 스마트한 중고거래 플랫폼** > Next.js와 Spring Boot를 연동하여 실무 수준의 JWT/OAuth2 인증, 실시간 채팅, 동적 상품 관리 시스템을 구축한 프로젝트입니다.

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" />
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
</p>

---

## 🏗️ Architecture Diagram

서비스의 전체적인 흐름은 다음과 같습니다. 사용자의 요청은 Next.js 서버를 거쳐 Spring Boot API 서버와 통신하며, 모든 데이터는 MySQL에 안전하게 저장됩니다.

| Layer | Technology | Role |
| :--- | :--- | :--- |
| **Frontend** | **Next.js (App Router)** | CSR/SSR 최적화, Axios 인터셉터 기반 JWT 관리, UI/UX 렌더링 |
| **Backend** | **Spring Boot** | RESTful API 제공, Spring Security 인증, 비즈니스 로직 처리 |
| **Database** | **MySQL** | 상품 정보, 유저 데이터, 이미지 경로 저장 |

---

## 🛠️ Tech Stack

### Frontend
- **Framework:** `Next.js 14+` (App Router)
- **Language:** `TypeScript`
- **Styling:** `Tailwind CSS`
- **State Management:** `React Context API` (Auth)
- **HTTP Client:** `Axios`
- **Real-time:** `SockJS`, `STOMP`
- **Icons:** `Lucide React`

---

## ✨ Key Features

| 기능 | 상세 설명 |
| :--- | :--- |
| **🔒 OAuth2 & JWT 인증** | 카카오 소셜 로그인 연동 및 JWT 토큰을 LocalStorage와 Axios Interceptor로 자동 관리 |
| **📦 동적 상품 관리** | `FormData`를 활용하여 텍스트 정보와 다중 이미지(기존 이미지 유지/삭제 및 새 이미지 추가)를 동시에 처리 |
| **❤️ 찜하기 & 조회수** | **Optimistic UI**를 적용하여 좋아요 토글 시 서버 응답 대기 없이 즉각적인 UI 반영 |
| **💬 실시간 채팅** | WebSocket & STOMP 기반으로 구매자-판매자 간 실시간 양방향 1:1 채팅 제공 |
| **👤 마이페이지 (탭 UI)** | '나의 판매 내역'과 '관심 목록(찜)'을 탭으로 분리하여 사용자 편의성 극대화 |
| **🔍 지능형 검색** | 키워드 및 카테고리별 실시간 필터링 시스템 |

---

## 🔍 Troubleshooting (문제 해결 과정)

> 프로젝트를 진행하며 겪은 프론트엔드 기술적 난관과 극복 과정을 기록합니다.

### 1️⃣ Next.js SSR과 LocalStorage 토큰 접근 이슈
- **문제:** 새로고침 시 상품 상세 페이지나 메인 리스트에서 사용자의 '좋아요(isLiked)' 상태가 초기화(false)되는 현상 발생. Server Component(SSR) 단계에서는 브라우저의 `localStorage`에 접근할 수 없어 API 요청 시 토큰이 누락됨.
- **해결:** 데이터 패칭 로직을 Client Component(`"use client"`) 내부의 `useEffect`로 이관하여, 브라우저 마운트 이후 로컬 스토리지에서 토큰을 추출해 `Authorization: Bearer` 헤더에 담아 요청하도록 라이프사이클을 조정하였습니다.

### 2️⃣ FormData 전송 시 Content-Type 경계(Boundary) 에러
- **문제:** 상품 수정 시 `multipart/form-data`로 데이터를 전송할 때 백엔드에서 파싱 에러 발생.
- **해결:** Axios 요청 헤더에 수동으로 `Content-Type: multipart/form-data`를 설정할 경우 브라우저가 자동으로 계산해야 할 데이터 경계(Boundary) 값이 누락됨을 파악. 수동 설정을 제거하여 브라우저가 올바른 헤더와 경계를 자동 생성하도록 수정하였습니다.

### 3️⃣ 백엔드 Boolean 데이터 직렬화 명칭 불일치 대응
- **문제:** 백엔드에서 `isLiked` 상태를 `true/false`로 넘겨주었으나, 프론트엔드에서는 해당 필드를 찾지 못해 하트 아이콘이 렌더링되지 않음.
- **해결:** 네트워크 탭 분석 결과 Jackson 라이브러리가 `isLiked`를 `liked`로 임의 변환함을 확인. 백엔드 개발자와 협업하여 `@JsonProperty("isLiked")` 어노테이션을 적용하도록 요청하고, 프론트엔드 TypeScript Interface와 정확히 매핑시켰습니다.

### 4️⃣ 비동기 렌더링 시 roomId 유효성 검증 및 API 호출 최적화
- **문제**: Next.js App Router의 `useParams()`가 초기 렌더링 시 빈 객체를 반환하여, 채팅방 ID가 `NaN`으로 백엔드에 전달되어 API 호출 에러 발생.
- **해결**: `useEffect` 내에서 `roomId`의 존재 여부와 숫자 타입을 검증하는 **Guard Logic**을 도입하여 유효한 데이터가 확보된 시점에만 WebSocket 연결이 시작되도록 최적화하였습니다.

### 5️⃣ STOMP 한글 입력(IME) 중복 전송 방지
- **문제**: 채팅창에서 한글 입력 후 엔터를 칠 때, 조합 중인 글자가 분리되며 메시지가 두 번 연속으로 전송되는 현상 발생.
- **해결**: `onKeyDown` 이벤트 핸들러에 `e.nativeEvent.isComposing` 상태 체크 로직을 추가하여, 한글 조합(IME)이 완료된 후의 이벤트만 서버로 전송하도록 방어 코드를 작성하였습니다.

### 6️⃣ 이미지 렌더링 시 DTO 필드명 불일치 해결
- **문제:** 상세 조회(`imageUrls` 배열)와 목록 조회(`representativeImageUrl` 단일 필드)의 응답 필드명이 달라 UI 컴포넌트 재사용이 어려움.
- **해결:** TypeScript 인터페이스를 API 목적별로 정교하게 분리(`ProductListResponse`, `ProductDetailResponse`)하고, 렌더링 컴포넌트에서 타입 가드를 활용하여 안전하게 이미지 데이터에 접근하도록 구현하였습니다.

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
