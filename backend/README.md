# 🚀 캠퍼스 마켓 (Campus Market) - Backend



> **대학생 중고거래 플랫폼을 위한 고성능 · 보안 중심 REST API 서버**



> Spring Boot, Spring Security, JWT, OAuth2를 기반으로 안정적이고 확장 가능한 백엔드 시스템을 구축하였습니다.



> **또한 100만 건 규모 데이터 환경에서 Redis 캐싱 및 DB 튜닝을 통해 조회 성능을 개선한 경험을 담고 있습니다.**



<p align="center">

  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white" />

  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />

  <img src="https://img.shields.io/badge/JSON%20Web%20Tokens-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />

  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />

  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />

</p>



---



## 🏗️ Backend Architecture



이 프로젝트는 계층형 아키텍처(Layered Architecture)를 준수하며, 유지보수와 테스트가 용이하도록 설계되었습니다.



* **Security Layer**: `OncePerRequestFilter`를 상속받은 JWT 필터와 OAuth2 로그인 로직이 모든 요청의 인증을 사전 처리합니다.

* **Domain Layer**: JPA 엔티티와 연관관계 매핑을 통해 데이터 정합성을 유지합니다.

* **API Layer**: RESTful 원칙을 준수하여 프론트엔드와의 통신 효율을 극대화했습니다.

* **External Integration Layer**: AI 모델(Gemini) 및 SMTP 이메일 서버와의 S2S(Server-to-Server) 통신을 담당하여 비즈니스 로직을 확장합니다.



---



## 🛠️ Tech Stack



* **Language:** `Java 17`

* **Framework:** `Spring Boot 3.x`

* **Build Tool:** `Gradle`

* **Database:** `MySQL`, `Spring Data JPA`

* **Cache:** `Redis`

* **Security:** `Spring Security`, `OAuth2 Client`, `JWT (jjwt 0.12.3)`

* **Infra & DevOps:** `Docker`, `GitHub Actions`

* **Performance Testing:** `k6`

* **External API:** `Google Gemini 2.5 API` (RestClient), `JavaMailSender` (SMTP)

* **Lombok** & **Validation**



---



## ✨ Key Features



| 기능                          | 기술적 포인트                                                                              |

| :-------------------------- | :----------------------------------------------------------------------------------- |

| **🔐 OAuth2 & JWT Auth**    | Spring Security 기반 카카오 소셜 로그인 구현 및 자체 JWT 발급을 통한 Stateless 인증 인가 로직 고도화              |

| **✉️ 대학 이메일 인증**            | `JavaMailSender`를 활용한 특정 도메인(`@sj.sangji.ac.kr`) 전용 6자리 인증번호 발송 및 메모리 기반 상태 검증 로직 구현 |

| **🤖 AI 지능형 서비스**           | `RestClient`를 활용한 Gemini API 연동. 판매글 자동 생성 및 부적절 게시글 필터링 기능 구현                       |

| **🖼️ Image & Product API** | `@RequestPart` 활용 및 Multipart 기반 이미지 추가·수정·삭제 처리                                     |

| **❤️ 찜하기 & 조회수**            | DTO 기반 응답 설계, 사용자별 `isLiked` 상태 관리 및 조회 최적화                                          |

| **💬 Real-time Chat**       | Spring WebSocket & STOMP 프로토콜을 활용한 실시간 양방향 채팅 시스템 구축                                 |

| **🛡️ Security Config**     | CORS 설정 통합 관리, HTTP Method별 세밀한 권한 제어 및 `AuthenticationEntryPoint` 커스텀 처리            |



---



## 🔍 Troubleshooting (문제 해결 과정)



### 1️⃣ 소셜 로그인 보안 흐름 설계 (OAuth2 + JWT)



* **문제:** 프론트엔드에서 직접 카카오 Access Token을 처리할 경우 토큰 탈취 위험이 존재.

* **해결:** 백엔드 주도 방식(Spring Security `.oauth2Login()`)을 채택하여 백엔드가 직접 카카오 서버와 통신 후 JWT를 발급하도록 구현.



### 2️⃣ 생성형 AI(Gemini) 연동 시 S2S 아키텍처 도입



* **문제:** 프론트엔드에서 직접 AI API를 호출할 경우 API Key 노출 위험 존재.

* **해결:** Server-to-Server(S2S) 구조를 채택하여 API Key를 안전하게 관리하고 비즈니스 로직을 통제.



### 3️⃣ Jackson Boolean 직렬화 문제 해결



* **문제:** `isLiked` 필드가 JSON 직렬화 시 `liked`로 변환되어 상태 관리 오류 발생.

* **해결:** `@JsonProperty("isLiked")` 적용을 통해 데이터 바인딩 문제 해결.



### 4️⃣ 세션 기반 인증에서 JWT 방식으로 전환



* **문제:** 프론트엔드와 백엔드의 포트 분리 환경에서 세션/쿠키 공유 문제 발생.

* **해결:** Stateless JWT 인증 방식을 도입하여 인증 문제 해결.



### 5️⃣ API 응답 DTO 설계 최적화 및 N+1 문제 방지



* **문제:** 엔티티 직접 반환 시 순환 참조 위험 및 과도한 쿼리 발생.

* **해결:** DTO 분리 및 `fetch join` 활용을 통해 N+1 문제를 방지하고 조회 성능을 개선.



### 6️⃣ WebSocket 순환 참조 및 JSON 직렬화 오류 해결



* **문제:** 채팅 엔티티 간 양방향 연관관계로 인해 JSON 직렬화 과정에서 무한 루프 발생.

* **해결:** `ChatMessageResponse` DTO를 구축하여 순환 참조 문제 해결.



### 7️⃣ STOMP 기반 실시간 채팅 동기화 문제 해결



* **문제:** 한글 입력 환경에서 메시지 중복 전송 및 roomId 초기화 이슈 발생.

* **해결:** 입력 상태 검증 및 roomId 유효성 검증 로직을 추가하여 해결.



### 8️⃣ 100만 건 데이터 환경에서 조회 성능 최적화



* **문제:** 100만 건의 더미 데이터를 적재한 후 k6 기반 부하 테스트를 수행한 결과 메인 페이지 조회 API에서 9초 이상의 응답 지연과 OOM 현상이 발생.

* **해결:**



* EXPLAIN 분석을 통해 Full Scan 발생 확인

* `Category`, `Status`, `CreatedAt` 기준 복합 인덱스 설계

* Fetch Join 제거 및 `default_batch_fetch_size=100` 적용

* Redis Look-Aside 패턴 기반 캐싱 도입

* 사용자별 `isLiked` 상태를 JWT 기반 동적 조회로 분리

* Self Invocation 문제 해결을 위한 캐시 전용 서비스 계층 분리

* **결과:**



* 평균 응답 시간 약 9,000ms → 7ms (Redis Cache Hit 기준)

* Timeout 제거

* OOM 해결

* 에러율 0% 달성



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

```



### 2. 프로젝트 실행



```bash

./gradlew bootRun

```



---



## 🔗 Related Links



본 백엔드 서버와 연동하여 동작하는 **프론트엔드(Next.js)** 소스코드 레포지토리입니다.



| 프로젝트                       | 레포지토리 링크                                             | 기술 스택                              |

| :------------------------- | :--------------------------------------------------- | :--------------------------------- |

| **Campus Market Frontend** | https://github.com/juyoon0423/campus-market-frontend | `Next.js`, `Tailwind CSS`, `Axios` |



---



<p align="center">

<b>본 프로젝트는 Next.js 프론트엔드와 Spring Boot 백엔드를 분리하여 개발한 프로젝트입니다.</b>

</p>

