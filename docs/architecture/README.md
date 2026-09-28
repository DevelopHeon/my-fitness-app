# My Fitness Architecture

이 문서는 현재 My Fitness의 소프트웨어 구조를 빠르게 이해하기 위한 문서입니다.

복잡한 클래스 목록보다 C4 Model의 Context, Container, Component 수준과 핵심 설계 규칙만 설명합니다.

---

## 1. Architecture Summary

My Fitness는 하나의 Spring Boot 서버를 배포 단위로 사용하는 Modular Monolith입니다.

프론트엔드는 Next.js 정적 PWA이며 빌드 시 Spring Boot 정적 리소스에 포함됩니다. 브라우저는 REST API를 같은 origin으로 호출합니다.

Backend 내부는 기능 모듈과 4-layer 구조를 사용하며 Spring Modulith와 ArchUnit으로 경계를 검증합니다.

핵심 원칙:

- 기능별 모듈 경계를 먼저 나눈다.
- Controller는 Application In Port만 호출한다.
- Application Service가 Use Case와 transaction boundary를 담당한다.
- Repository와 외부 시스템은 Out Port 뒤에 둔다.
- 다른 모듈은 공개된 Named Interface만 사용한다.
- JPA Entity는 HTTP 응답 경계까지 노출하지 않는다.
- Schema는 Flyway로만 변경한다.

---

## 2. C4 Level 1 - System Context

~~~mermaid
flowchart LR
    User["사용자<br/>iOS / Android / Desktop"]
    System["My Fitness<br/>운동 · 신체 · 식단 · AI Coach"]
    Google["Google Identity<br/>OAuth2 / OIDC"]
    OpenAI["OpenAI API<br/>AI Coach Provider"]
    Ollama["Ollama<br/>로컬 개발 Provider"]

    User -->|"HTTPS"| System
    System -->|"로그인"| Google
    System -->|"운영 AI 요청"| OpenAI
    System -.->|"로컬 선택"| Ollama
~~~

My Fitness는 사용자의 피트니스 기록을 저장하고 이를 기반으로 통계와 AI Coach 응답을 제공합니다.

Google은 사용자 인증만 담당하며, 로그인 완료 후 애플리케이션은 서버 세션으로 인증 상태를 유지합니다.

---

## 3. C4 Level 2 - Containers

~~~mermaid
flowchart LR
    User["사용자"]

    subgraph Browser["사용자 기기"]
        PWA["Next.js PWA<br/>정적 Web App"]
    end

    subgraph Runtime["운영 서버"]
        Caddy["Caddy<br/>TLS / Reverse Proxy"]
        App["Spring Boot Application<br/>REST API + 정적 Frontend"]
    end

    DB[("PostgreSQL 17<br/>RDS")]
    Google["Google OIDC"]
    AI["OpenAI API"]

    User --> PWA
    PWA -->|"HTTPS / REST"| Caddy
    Caddy -->|"127.0.0.1:8080"| App
    App -->|"JPA / JDBC"| DB
    App -->|"OAuth2 / OIDC"| Google
    App -->|"Spring AI"| AI
~~~

### Container 역할

| Container | 역할 |
| --- | --- |
| Next.js PWA | 화면, 입력, 그래프, 모바일 설치 경험 |
| Caddy | HTTPS 종료, HTTP → HTTPS, reverse proxy |
| Spring Boot | 인증, Use Case, 비즈니스 규칙, API, AI orchestration |
| PostgreSQL | 사용자, 세션, 운동, 신체, 식단, AI 대화 데이터 |
| Google OIDC | 외부 사용자 인증 |
| OpenAI | 운영 AI 응답 생성 |

Next.js 결과물은 별도 Node 서버로 운영하지 않습니다. 정적 export 결과가 Spring Boot JAR에 포함됩니다.

Spring Session JDBC를 사용하므로 인증 세션도 PostgreSQL에 저장됩니다.

---

## 4. C4 Level 3 - Backend Components

기능 모듈을 Component 단위로 보면 다음과 같습니다.

~~~mermaid
flowchart LR
    UserModule["User<br/>Google OIDC / 사용자"]
    Exercise["Exercise<br/>운동 카탈로그"]
    Workout["Workout<br/>운동 기록 / 세트"]
    Routine["Routine<br/>운동 템플릿"]
    Body["Body<br/>신체 기록"]
    Nutrition["Nutrition<br/>식단 / 영양"]
    Dashboard["Dashboard<br/>통계 Read Model"]
    AI["AI Coach<br/>기록 기반 대화"]

    Workout -->|"catalog / domain-model"| Exercise
    Routine -->|"catalog"| Exercise
    Routine -->|"routine-api"| Workout
    Dashboard -->|"insight"| Workout
    Dashboard -->|"insight"| Body
    AI -->|"insight"| Workout
    AI -->|"insight"| Body
    AI -->|"insight"| Nutrition
~~~

### 모듈 책임

| Module | 책임 |
| --- | --- |
| user | Google OIDC 사용자 식별과 인증 컨텍스트 |
| exercise | 기본 운동, 사용자 커스텀 운동, 공통 운동 참조 |
| workout | 운동 일자, 종목 snapshot, 세트, 완료 상태 |
| routine | 반복 가능한 운동 템플릿과 Workout 시작 |
| body | 체중, 체지방률, 골격근량 기록 |
| nutrition | Food, Meal, 영양 snapshot, 일일 목표 |
| dashboard | Workout / Body 데이터를 조합한 통계 |
| ai | Workout / Body / Nutrition insight 기반 AI Coach |
| common | HTTP 오류 변환과 공통 기술 설정 |

### 공개 모듈 경계

| Consumer | 허용된 공개 API |
| --- | --- |
| workout → exercise | exercise::catalog, exercise::domain-model |
| routine → exercise | exercise::catalog, exercise::domain-model |
| routine → workout | workout::routine-api |
| dashboard → workout | workout::insight |
| dashboard → body | body::insight |
| ai → workout | workout::insight |
| ai → body | body::insight |
| ai → nutrition | nutrition::insight |

이 규칙은 package-info.java의 Spring Modulith allowedDependencies로 검증합니다.

---

## 5. 모듈 내부 구조

각 기능 모듈은 필요한 범위에서 다음 구조를 사용합니다.

~~~text
<module>
├── presentation
│   ├── controller
│   └── dto
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   ├── service
│   ├── command
│   └── result
├── domain
│   ├── model
│   └── exception
└── infrastructure
    ├── persistence
    ├── query
    └── external
~~~

기본 호출 흐름:

~~~mermaid
flowchart LR
    HTTP["Controller"] --> InPort["Application In Port"]
    InPort --> App["Application Service"]
    App --> Domain["Domain Model"]
    App --> OutPort["Application Out Port"]
    Adapter["Infrastructure Adapter"] --> OutPort
    Adapter --> External["DB / External API"]
~~~

### 계층 규칙

- Presentation은 In Port만 호출합니다.
- Application은 Use Case와 transaction을 orchestration합니다.
- Domain은 핵심 규칙을 가집니다.
- Infrastructure는 Repository / Query / 외부 API Adapter를 구현합니다.
- Domain은 Application, Presentation, Infrastructure를 참조하지 않습니다.
- Repository 계약은 application.port.out에 둡니다.

현재 규모에서는 Domain Entity와 Persistence Entity를 별도로 복제하지 않고 Domain Model에 JPA mapping annotation을 허용합니다.

---

## 6. Transaction Boundary

DB를 사용하는 In Port 구현 Application Service는 경계를 명시합니다.

~~~java
@Service
@Transactional(readOnly = true)
public class ExampleApplicationService {

    public Result get(...) {
        ...
    }

    @Transactional
    public Result create(...) {
        ...
    }

    @Transactional
    public void delete(...) {
        ...
    }
}
~~~

규칙:

- 조회는 class-level readOnly transaction을 기본으로 사용합니다.
- 생성, 수정, 삭제 public Use Case는 method-level write transaction을 선언합니다.
- Repository Adapter의 암묵적 transaction을 Application 경계 대신 사용하지 않습니다.
- Application 내부 collaborator는 독립 transaction을 만들지 않고 호출자의 transaction에 참여합니다.

### AI 예외

AI Provider 호출은 네트워크 대기가 포함되므로 전체 sendMessage를 하나의 DB transaction으로 묶지 않습니다.

~~~text
User Message 저장 + Conversation 갱신
        ↓ COMMIT
OpenAI 호출
        ↓
Assistant / Request Log 저장
        ↓ COMMIT
~~~

AiCoachService.sendMessage는 Provider 호출 동안 DB transaction을 유지하지 않고, AiMessageTransactionService가 짧은 DB transaction을 담당합니다.

---

## 7. 주요 데이터 경계

### Workout snapshot

Workout과 Routine은 운동 카탈로그가 나중에 수정되더라도 과거 기록이 바뀌지 않도록 이름과 카테고리를 snapshot으로 저장합니다.

따라서 사용자가 CUSTOM 운동을 수정하거나 삭제해도 기존 Workout 기록은 유지됩니다.

### JPA / HTTP 경계

JPA Entity를 Controller response로 직접 반환하지 않습니다.

Application Service 안에서 Result projection으로 변환한 뒤 transaction 밖으로 전달합니다.

이 규칙 덕분에 production에서도 spring.jpa.open-in-view=false를 유지합니다.

### Database schema

- hibernate.ddl-auto=none
- Flyway migration만 사용
- Spring Session table도 Flyway가 관리

---

## 8. 주요 흐름

### Google Login

~~~text
Browser
 → /oauth2/authorization/google
 → Google
 → /login/oauth2/code/google
 → GoogleOidcUserService
 → User 저장/갱신
 → Spring Security Session
 → PostgreSQL Spring Session
~~~

기능 API는 인증된 principal의 userId를 사용합니다. 클라이언트가 임의의 사용자 ID header를 전달하지 않습니다.

### Routine → Workout

Routine은 Workout 구현체나 Repository를 직접 호출하지 않습니다.

~~~text
RoutineApplicationService
 → ExerciseCatalogQuery
 → WorkoutRoutineUseCase
 → WorkoutRoutineApplicationService
~~~

### Dashboard / AI

Dashboard와 AI는 다른 기능의 Repository를 직접 읽지 않고 읽기 전용 insight In Port를 사용합니다.

---

## 9. 경계 검증

아키텍처 규칙은 설명 문서에만 의존하지 않습니다.

- Spring Modulith: application module과 Named Interface 검증
- ArchUnit: 계층과 annotation 위치 검증
- Integration Test: HTTP, transaction, persistence 경계 검증

구조를 바꿀 때는 package-info.java, architecture test, 이 문서를 같은 변경 단위에서 갱신합니다.

---

## 10. 관련 문서

- [문서 전체 안내](../README.md)
- [AWS Infrastructure](../infra/README.md)
- [Operations](../infra/OPERATIONS.md)
- [Product Spec](../spec/PRODUCT_SPEC.md)
- [Testing](../testing/README.md)
