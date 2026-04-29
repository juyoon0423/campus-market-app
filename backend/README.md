# 🚀 캠퍼스 마켓 (Campus Market) - Backend
> **대학생 중고거래 플랫폼을 위한 고성능, 보안 중심 REST API 서버** > Spring Boot와 Spring Security, JWT를 활용하여 안정적이고 확장 가능한 백엔드 시스템을 구축하였습니다.

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



- **Security Layer**: `OncePerRequestFilter`를 상속받은 JWT 필터가 모든 요청의 인증을 사전 처리합니다.
- **Domain Layer**: JPA 엔티티와 연관관계 매핑을 통해 데이터 정밀도를 유지합니다.
- **API Layer**: RESTful 원칙을 준수하여 프론트엔드와의 통신 효율을 극대화했습니다.

---

## 🛠️ Tech Stack

- **Language:** `Java 17`
- **Framework:** `Spring Boot 3.x`
- **Build Tool:** `Gradle`
- **Database:** `MySQL`, `Spring Data JPA`
- **Security:** `Spring Security`, `JWT (jjwt 0.12.3)`
- **Lombok** & **Validation**

---

## ✨ Key Features

| 기능 | 기술적 포인트 |
| :--- | :--- |
| **🔐 JWT Auth** | `Stateless` 환경 구현, 커스텀 `JwtAuthenticationFilter`를 통한 보안 강화 |
| **🖼️ Image Server** | `ResourceHandler` 설정을 통한 로컬 이미지 서빙 및 UUID 기반 파일명 관리 |
| **🛡️ Security Config** | CORS 설정 통합 관리 및 `AuthenticationEntryPoint`를 통한 401 에러 핸들링 |
| **📈 Product API** | `@RequestPart`를 활용한 JSON 데이터와 다중 이미지 파일의 동시 처리 |

---

## 🔍 Troubleshooting (문제 해결 과정)

### 1️⃣ 세션 기반 인증에서 JWT 방식으로의 전환
- **문제:** 프론트엔드(Next.js)와 백엔드의 포트 번호 차이로 인한 세션/쿠키 공유 이슈 발생.
- **해결:** 서버에 상태를 저장하지 않는 **Stateless JWT 인증 방식**으로 전환. `SecurityContextHolder`에 인증 객체를 직접 주입하는 필터를 구현하여 CORS 환경에서의 인증 문제를 원천적으로 해결하였습니다.

### 2️⃣ API 응답 DTO 설계 최적화
- **문제:** 엔티티의 복잡한 연관관계(List<ProductImage>)를 그대로 반환할 경우 순환 참조 및 과도한 데이터 노출 위험 발생.
- **해결:** `ProductListResponse`와 `ProductDetailResponse` 등 용도별 **DTO(Data Transfer Object)**를 분리. 목록 조회 시에는 대표 이미지만, 상세 조회 시에는 전체 이미지 배열을 반환하도록 최적화하였습니다.

### 3️⃣ 보안 예외 처리 고도화 (403 -> 401)
- **문제:** 인증되지 않은 사용자가 보호된 자원에 접근 시 시큐리티 기본 설정에 의해 모호한 403 Forbidden 에러가 반환됨.
- **해결:** `AuthenticationEntryPoint`를 커스텀 구현하여 인증 실패 시 명확한 **401 Unauthorized** 상태 코드와 커스텀 에러 메시지를 반환하도록 개선하였습니다.

---

## ⚙️ Configuration & Installation

### 1. Database 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_market
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
  jpa:
    hibernate:
      ddl-auto: update
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

 
