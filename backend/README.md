# 🚀 캠퍼스 마켓 (Campus Market) - Backend
> **대학생 중고거래 플랫폼을 위한 고성능, 보안 중심 REST API 서버** > Spring Boot와 Spring Security, JWT, 그리고 OAuth2를 활용하여 안정적이고 확장 가능한 백엔드 시스템을 구축하였습니다.

<p align="center">
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/JSON%20Web%20Tokens-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
</p>

---

## 🏗️ Backend Architecture

본 서버는 계층형 아키텍처(Layered Architecture)를 준수하며, 유지보수와 테스트가 용이하도록 설계되었습니다.

- **Security Layer**: `OncePerRequestFilter`를 상속받은 JWT 필터와 OAuth2 로그인 로직이 모든 요청의 인증을 사전 처리합니다.
- **Domain Layer**: JPA 엔티티와 연관관계 매핑을 통해 데이터 정밀도를 유지합니다.
- **API Layer**: RESTful 원칙을 준수하여 프론트엔드와의 통신 효율을 극대화했습니다.
- **External Integration Layer**: AI 모델(Gemini) 및 SMTP 이메일 서버와의 S2S(Server-to-Server) 통신을 담당하여 비즈니스 로직을 확장합니다.

---

## 🛠️ Tech Stack

- **Language:** `Java 17`
- **Framework:** `Spring Boot 3.x`
- **Build Tool:** `Gradle`
- **Database:** `MySQL`, `Spring Data JPA`
- **Security:** `Spring Security`, `OAuth2 Client`, `JWT (jjwt 0.12.3)`
- **External API:** `Google Gemini 2.5 API` (RestClient), `JavaMailSender` (SMTP)
- **Lombok** & **Validation**

---

## ✨ Key Features

| 기능 | 기술적 포인트 |
| :--- | :--- |
| **🔐 OAuth2 & JWT Auth** | Spring Security 기반 카카오 소셜 로그인 구현 및 자체 JWT 발급을 통한 Stateless 인증 인가 로직 고도화 |
| **✉️ 대학 이메일 인증** | `JavaMailSender`를 활용한 특정 도메인(`@sj.sangji.ac.kr`) 전용 6자리 인증번호 발송 및 메모리 기반 상태 검증 로직 구현 |
| **🤖 AI 지능형 서비스** | `RestClient`를 활용한 Gemini 2.5 API 연동. 프롬프트 엔지니어링 기반 판매글 자동 생성 및 부적절 매물 차단(필터링) 시스템 구축 |
| **🖼️ Image & Product API** | `@RequestPart` 활용 및 폼 데이터(multipart/form-data) 기반 기존 이미지 유지/삭제 및 새 이미지 추가 동적 처리 |
| **❤️ 찜하기 & 조회수** | 본인 상품 필터링, N+1 문제 방지를 위한 Fetch Join 활용, 프론트엔드 상태 관리를 위한 `isLiked` DTO 매핑 |
| **💬 Real-time Chat** | Spring WebSocket & STOMP 프로토콜을 활용한 실시간 양방향 채팅 시스템 구축 |
| **🛡️ Security Config** | CORS 설정 통합 관리, HTTP Method별 세밀한 권한 제어 및 `AuthenticationEntryPoint` 커스텀 처리 |

---

## 🔍 Troubleshooting (문제 해결 과정)

### 1️⃣ 소셜 로그인 보안 흐름 설계 (OAuth2 + JWT)
- **문제:** 프론트엔드에서 직접 카카오 Access Token을 탈취당할 경우 보안 취약점이 발생할 수 있음.
- **해결:** 백엔드 주도 방식(Spring Security `.oauth2Login()`)을 채택. 프론트엔드는 단순 리다이렉트만 수행하고, 백엔드가 직접 카카오 서버와 Server-to-Server로 통신하여 인가 코드를 교환한 뒤 자체 JWT를 발급하도록 하여 보안성을 극대화하였습니다.

### 2️⃣ 생성형 AI(Gemini) 연동 시 S2S 아키텍처 도입 및 JSON 직렬화
- **문제:** 프론트엔드에서 직접 AI API를 호출할 경우 API Key 탈취 위험 및 프롬프트 인젝션 취약점이 존재하며, 단순 String 반환 시 프론트엔드에서 JSON 파싱 에러가 발생함.
- **해결:** 백엔드가 클라이언트와 AI 서버 사이의 프록시 역할을 하는 **Server-to-Server(S2S)** 구조를 채택하여 API 키를 안전하게 격리하고 비즈니스 로직(프롬프트)을 철저히 통제했습니다. 또한, 응답 데이터를 `Map.of("description", result)`를 활용해 표준 JSON 객체로 직렬화하여 클라이언트와의 통신 안정성을 확보했습니다.

### 3️⃣ Jackson 라이브러리의 Boolean 직렬화 함정 해결
- **문제:** 프론트엔드에 `isLiked` (좋아요 여부) 상태를 넘겨줄 때, Jackson 라이브러리의 기본 동작으로 인해 JSON 키 값이 `liked`로 자동 변환되어 프론트엔드에서 상태가 초기화되는 버그 발생.
- **해결:** DTO의 필드에 `@JsonProperty("isLiked")` 어노테이션을 적용하여 직렬화되는 키 이름을 명시적으로 강제함으로써, 프론트-백엔드 간의 데이터 바인딩 에러를 완벽하게 해결하였습니다.

### 4️⃣ 세션 기반 인증에서 JWT 방식으로의 전환
- **문제:** 프론트엔드(Next.js)와 백엔드의 포트 번호 차이로 인한 세션/쿠키 공유 이슈 발생.
- **해결:** 서버에 상태를 저장하지 않는 **Stateless JWT 인증 방식**으로 전환. `SecurityContextHolder`에 인증 객체를 직접 주입하는 필터를 구현하여 CORS 환경에서의 인증 문제를 원천적으로 해결하였습니다.

### 5️⃣ API 응답 DTO 설계 최적화 및 N+1 문제 방지
- **문제:** 엔티티의 복잡한 연관관계(`List<ProductImage>`, `User` 등)를 반환할 경우 순환 참조 위험 및 과도한 쿼리 발생.
- **해결:** 용도별 **DTO(Data Transfer Object)**를 분리하고, Repository 계층에서 `@Query`와 `fetch join`을 활용하여 '내가 찜한 상품 목록'이나 '전체 상품 목록' 조회 시 발생하는 N+1 문제를 방지하고 쿼리 성능을 최적화하였습니다.

### 6️⃣ WebSocket 순환 참조 및 JSON 직렬화 오류 해결
- **문제**: 채팅 메시지 조회 시 엔티티(`ChatMessage` ↔ `ChatRoom`) 간의 양방향 연관관계로 인해 JSON 변환 과정에서 무한 루프가 발생, `HttpMessageNotWritableException` 에러와 함께 서버 다운.
- **해결**: 엔티티를 직접 노출하지 않고, 필요한 필드만 포함하는 **`ChatMessageResponse` DTO**를 구축. 스트림 API를 활용하여 변환함으로써 데이터 크기를 줄이고 순환 참조 문제를 원천 차단하였습니다.

### 7️⃣ STOMP 기반 실시간 채팅의 동기화 및 입력 이슈 처리
- **문제**: 한글 입력(IME) 환경에서 엔터 키 입력 시 메시지가 중복 전송되는 현상 및 초기 `roomId` 로딩 시점 차이로 인한 `NaN` 파라미터 전달 에러 발생.
- **해결**:
  - **FE**: `isComposing` 상태 체크 로직을 도입하여 중복 이벤트를 방지.
  - **BE**: `roomId` 파싱 시 유효성 검증 로직을 강화하고, 서비스 레이어에서 방 생성과 조회를 원자적(Atomic)으로 처리하여 중복 방 생성을 방지하였습니다.

---

## ⚙️ Configuration & Installation

### 1. Database 및 외부 API 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_market
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
  jpa:
    hibernate:
      ddl-auto: update
  mail:
    host: smtp.gmail.com
    port: 587
    username: YOUR_GMAIL_ACCOUNT
    password: YOUR_APP_PASSWORD

gemini:
  api:
    key: YOUR_GEMINI_API_KEY
    url: [https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent](https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent)
```

### 2. 프로젝트 실행
```Bash
./gradlew bootRun
```

---

## 🔗 Related Links

본 백엔드 서버와 연동하여 동작하는 **프론트엔드(Next.js)** 소스코드 레포지토리입니다.

| 프로젝트 | 레포지토리 링크 | 기술 스택 |
| :--- | :--- | :--- |
| **Campus Market Frontend** | [![Frontend Repo](https://img.shields.io/badge/Frontend-GitHub-blue?style=flat-square&logo=github)](https://github.com/juyoon0423/campus-market-frontend) | `Next.js`, `Tailwind CSS`, `Axios` |

---

<p align="center">
  <b>본 프로젝트는 프론트엔드와 백엔드가 분리된 MSA 지향 구조로 설계되었습니다.</b>
</p>

 
