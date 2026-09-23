# My Fitness Architecture

이 문서는 My Fitness 백엔드의 현재 모듈 경계, 4계층 구조, Port/Adapter 방향과 의존성 규칙을 정의한다.

구조를 변경할 때는 코드, package-info.java, Spring Modulith/ArchUnit 테스트와 이 문서를 같은 작업 단위에서 함께 갱신한다.

---

## 1. 아키텍처 목표

My Fitness는 하나의 Spring Boot 배포 단위를 사용하는 Modular Monolith다.

~~~text
Next.js PWA
     │
     │ REST
     ▼
Spring Boot
├── exercise
├── workout
├── routine
├── body
├── nutrition
├── dashboard
├── ai
├── user
└── common
     │
     ▼
PostgreSQL
~~~

현재 목표는 서비스를 여러 프로세스로 나누는 것이 아니라 다음을 코드 수준에서 명확하게 유지하는 것이다.

- 기능 모듈의 책임
- 모듈 내부 계층 방향
- 외부에서 Application으로 들어오는 방향
- Application에서 DB/외부 시스템으로 나가는 방향
- 모듈 간 공개 API
- Infrastructure 구현 세부사항 격리

이를 위해 4-layer 구조, Hexagonal Port/Adapter, Spring Modulith, ArchUnit, Flyway를 함께 사용한다.

---

## 2. 기술 기준

- Java 21
- Spring Boot 4.1.1
- PostgreSQL 17
- Spring Data JPA
- Flyway
- Spring Modulith
- ArchUnit
- Next.js 16 + TypeScript + PWA
- Spring AI 2.0.1

Main runtime은 Hibernate ddl-auto: none을 유지한다.
DB schema 변경은 Flyway migration으로만 관리한다.

---

## 3. 최상위 모듈

~~~text
com.myfitness
├── exercise
├── workout
├── routine
├── body
├── nutrition
├── dashboard
├── ai
├── user
└── common
~~~

각 기능은 Spring Modulith application module이다.

- exercise: 기본 운동과 사용자 커스텀 운동 카탈로그
- workout: 운동 날짜, 운동 종목 snapshot, 세트 기록, 완료 상태
- routine: 반복 운동 템플릿과 Workout 시작 orchestration
- body: 체중, 체지방률, 골격근량 기록
- nutrition: Food, Meal, MealFood, NutritionGoal
- dashboard: 다른 모듈의 읽기 API를 이용한 통계 Read Model
- ai: Phase 6 AI Coach
- user: 향후 인증/사용자 컨텍스트
- common: 전역 HTTP 오류 변환과 공통 기술 설정

Common은 비즈니스 모델을 모으는 shared-kernel로 사용하지 않는다.

---

## 4. 모듈 내부 4계층

~~~text
<module>
├── presentation
│   ├── controller
│   └── dto
│
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   ├── service
│   ├── command
│   ├── result
│   └── exception
│
├── domain
│   ├── model
│   ├── service
│   └── exception
│
└── infrastructure
    ├── persistence
    ├── query
    ├── bootstrap
    └── external
~~~

모든 하위 디렉터리를 억지로 만들지는 않는다. 실제 책임이 있을 때만 생성한다.

---

## 5. Presentation

Presentation은 외부 요청/응답 Adapter다.

주요 책임:

- REST Controller
- HTTP request/response DTO
- Bean Validation
- Application Result → API Response 변환
- HTTP status

Controller는 Application Service 구현체를 직접 주입받지 않는다.

~~~text
금지

WorkoutController
      ↓
WorkoutApplicationService
~~~

~~~text
허용

WorkoutController
      ↓
WorkoutUseCase
~~~

Presentation은 Infrastructure를 직접 참조하지 않는다.

### Entity 경계 규칙

JPA Entity는 Presentation까지 전달하지 않는다.

~~~text
금지

Controller
   ↓
In Port
   ↓
Workout / BodyRecord / Food 같은 JPA Entity
~~~

~~~text
허용

Controller
   ↓
In Port
   ↓
Application Result
   ↑
Application Service가 transaction 안에서 Entity를 projection으로 변환
~~~

응답에 연관 데이터가 필요한 경우 Application Service의 트랜잭션 안에서 Application Result projection으로 변환한 뒤 Presentation에서 Response DTO로 매핑한다. 이를 통해 `open-in-view: false`에서도 응답 직렬화가 Persistence Session에 의존하지 않도록 한다.

Application Result는 transaction-detached projection으로 취급한다. Result의 필드에는 JPA Entity를 넣지 않으며 primitive, enum, value object, 다른 Result projection만 사용한다. Result 내부의 정적 factory가 Entity를 읽어 projection을 만드는 것은 허용하지만 Entity 자체를 field/return contract로 노출하지 않는다.

---

## 6. Application

Application은 Use Case와 orchestration을 담당한다.

주요 책임:

- transaction boundary
- Use Case 실행
- 여러 Domain 객체 조합
- 다른 모듈의 공개 In Port 호출
- Out Port 호출
- command/result
- 소유권/조회 실패 등 Application 수준 예외

Application 내부에서 가장 중요한 구분은 port.in과 port.out이다.

Application Service는 Domain Entity를 내부에서 사용하되 In Port 경계를 넘길 때는 Result projection으로 변환한다. 이 변환은 transaction이 열려 있는 Application 계층에서 완료한다.

Application Service 자체는 Use Case orchestration과 transaction boundary에 집중한다. 계산, 응답 조립, AI Context 생성처럼 독립적으로 설명할 수 있는 책임이 커지면 같은 Application 계층의 전용 collaborator로 분리한다.

### Transaction boundary 규칙

DB를 사용하는 In Port 구현 Application Service는 transaction 경계를 명시한다.

- 클래스 기본값은 `@Transactional(readOnly = true)`로 두어 조회 Use Case의 의도를 드러낸다.
- 생성/수정/삭제처럼 상태를 바꾸는 public Use Case 메서드는 `@Transactional`로 override한다.
- `WorkoutService`, `RoutineService`, `FoodService`처럼 Application Service 내부에서 호출되는 도메인 작업 collaborator는 독립 transaction 경계를 만들지 않고 호출한 Use Case transaction에 참여한다.
- Repository Adapter나 Spring Data의 암묵적 transaction을 Application transaction boundary의 대체 수단으로 사용하지 않는다.

외부 네트워크 호출이 포함되는 Use Case는 DB transaction을 네트워크 대기 시간 동안 유지하지 않는다. AI 메시지 전송이 이 예외에 해당한다.

- `AiCoachService.sendMessage()`는 `Propagation.NOT_SUPPORTED`로 Provider 호출을 포함한 전체 orchestration에 DB transaction을 열지 않는다.
- User Message 저장과 Conversation 갱신, OUT_OF_SCOPE 응답 저장, Provider 성공/실패 결과 저장은 `AiMessageTransactionService`의 짧은 `@Transactional` 메서드로 각각 커밋한다.
- 따라서 Provider 호출 전에 User Message가 먼저 커밋되는 기존 제품 동작을 유지하면서도, 각 DB 변경 경계는 Application 계층에서 명시적으로 보인다.

현재 예:

- `AiCoachService` → `AiHistorySelector`, `AiProviderExecutor`, `AiMessageTransactionService`
- `AiContextBuilder` → `AiWorkoutContextBuilder`, `AiBodyContextBuilder`, `AiNutritionContextBuilder`
- `NutritionApplicationService` → `NutritionResultAssembler`
- `DashboardService` → `DashboardResultAssembler`
- `WorkoutUseCase`와 `WorkoutRoutineUseCase` → 각각 `WorkoutApplicationService`, `WorkoutRoutineApplicationService`

하나의 Service가 여러 In Port를 구현하는 것이 항상 금지는 아니지만, 호출 주체와 결과 계약이 다르고 변경 이유도 분리된다면 구현 Service도 경계별로 나눈다.

---

## 7. In Port와 Out Port

in/out은 데이터 모양이 아니라 Application을 기준으로 호출 방향을 의미한다.

### 7.1 In Port

외부가 Application을 호출하는 계약이다.

~~~text
외부 Adapter
     │
     ▼
application.port.in
     │
     ▼
Application Service
~~~

예:

~~~text
WorkoutController
      ↓
WorkoutUseCase
      ↑ implements
WorkoutApplicationService
~~~

In Port는 JPA Entity를 입력/출력 계약으로 노출하지 않는다. 외부에 필요한 데이터는 command/value object/Application Result projection으로 표현한다.

다른 application module도 공개된 In Port를 통해 호출한다.

~~~text
Routine
   ↓
workout::routine-api
   ↓
WorkoutRoutineUseCase
~~~

현재 주요 In Port:

~~~text
exercise
└── application.port.in
    ├── ExerciseManagementUseCase
    └── catalog
        └── ExerciseCatalogQuery

workout
└── application.port.in
    ├── WorkoutUseCase
    ├── routine
    │   └── WorkoutRoutineUseCase
    └── insight
        └── WorkoutInsightQuery

routine
└── application.port.in
    └── RoutineUseCase

body
└── application.port.in
    ├── BodyRecordUseCase
    └── insight
        └── BodyInsightQuery

nutrition
└── application.port.in
    └── NutritionUseCase

dashboard
└── application.port.in
    └── DashboardQueryUseCase
~~~

### 7.2 Out Port

Application이 외부 자원이나 Adapter에게 요구하는 계약이다.

~~~text
Application Service
       │
       ▼
application.port.out
       ▲
       │ implements
Infrastructure Adapter
~~~

Repository 계약은 Out Port다.

~~~text
WorkoutService
      ↓
WorkoutRepositoryPort
      ↑
WorkoutRepositoryAdapter
      ↓
SpringDataWorkoutRepository
      ↓
PostgreSQL
~~~

Repository 계약은 `application.port.out`에 둔다.

이 프로젝트에서는 Repository를 Domain 객체 자체가 요구하는 기능이 아니라 Application이 Use Case 수행을 위해 요구하는 외부 저장소 계약으로 본다. 따라서 Repository Port는 `*RepositoryPort` 이름으로 Application Out Port에 위치시킨다.

---

## 8. Domain

Domain에는 핵심 비즈니스 모델과 규칙을 둔다.

~~~text
domain
├── model
├── service
└── exception
~~~

금지 방향:

~~~text
Domain -X-> Application
Domain -X-> Presentation
Domain -X-> Infrastructure
~~~

### JPA Entity 정책

현재는 Domain Entity와 Persistence Entity를 별도로 복제하지 않는다.

~~~java
package com.myfitness.workout.domain.model;

@Entity
public class Workout {
    ...
}
~~~

jakarta.persistence mapping annotation은 Domain Model에 허용한다.

현재 규모에서는 JPA 모델을 별도 Persistence Entity로 복제하는 비용보다 의존성 방향과 모듈 경계를 명확하게 유지하는 것을 우선한다.

Domain에서 허용하지 않는 것:

- JpaRepository 의존
- Spring Service
- Controller DTO
- Infrastructure Adapter
- Application Port

---

## 9. Infrastructure

Infrastructure는 Out Port 구현과 기술 Adapter를 담당한다.

~~~text
infrastructure
├── persistence
│   ├── WorkoutRepositoryAdapter
│   └── SpringDataWorkoutRepository
│
├── query
│   └── DashboardDataAdapter
│
└── bootstrap
    └── DefaultExerciseInitializer
~~~

Infrastructure는 Presentation을 직접 참조하지 않는다.

---

## 10. @Repository와 @Component 사용 기준

둘 다 Spring Bean 등록 대상이지만 의미를 구분한다.

### @Repository

DB 영속화 Adapter에 사용한다.

~~~java
@Repository
public class WorkoutRepositoryAdapter
        implements WorkoutRepositoryPort {
    ...
}
~~~

의미:

- Persistence Adapter
- Spring Data/JPA 연결
- Persistence exception translation 의미 표현

~~~text
WorkoutService
      ↓
WorkoutRepositoryPort        OUT PORT
      ↑
WorkoutRepositoryAdapter     @Repository
      ↓
SpringDataWorkoutRepository
      ↓
PostgreSQL
~~~

### @Component

Repository가 아닌 일반 Adapter에 사용한다.

~~~java
@Component
public class DashboardDataAdapter
        implements DashboardDataPort {
    ...
}
~~~

DashboardDataAdapter는 DB Repository가 아니다.

다른 모듈의 공개 In Port를 호출하고 Dashboard가 원하는 데이터 계약으로 변환한다.

~~~text
DashboardService
      ↓
DashboardDataPort            OUT PORT
      ↑
DashboardDataAdapter         @Component
      │
      ├── WorkoutInsightQuery
      └── BodyInsightQuery
~~~

따라서 DashboardDataAdapter에는 @Repository보다 @Component가 역할상 맞다.

---

## 11. Dashboard 의존성 흐름

Dashboard는 Port 방향을 이해하기 가장 좋은 예다.

~~~text
HTTP
 │
 ▼
DashboardController
 │
 ▼
DashboardQueryUseCase                    IN PORT
 ▲
 │ implements
DashboardService
 │
 ├────────► DashboardResultAssembler
 │
 ▼
DashboardDataPort                        OUT PORT
 ▲
 │ implements
DashboardDataAdapter                     @Component
 │
 ├────────► WorkoutInsightQuery          workout 공개 IN PORT
 │               │
 │               ▼
 │        WorkoutInsightService
 │               │
 │               ▼
 │        WorkoutRepositoryPort          OUT PORT
 │               ▲
 │               │
 │        WorkoutRepositoryAdapter       @Repository
 │               │
 │               ▼
 │          Spring Data JPA
 │
 └────────► BodyInsightQuery             body 공개 IN PORT
                 │
                 ▼
          BodyInsightService
                 │
                 ▼
          BodyRecordRepositoryPort
                 ▲
                 │
          BodyRecordRepositoryAdapter    @Repository
~~~

중요한 점:

- Dashboard는 WorkoutRepositoryPort를 직접 알지 않는다.
- Dashboard는 BodyRecordRepositoryPort를 직접 알지 않는다.
- Dashboard는 Workout/Body Entity를 직접 받지 않는다.
- DashboardDataPort는 Dashboard가 필요한 자체 데이터 계약을 가진다.
- DashboardDataAdapter가 타 모듈 projection을 Dashboard 데이터로 변환한다.
- DashboardService는 조회와 시간 기준 결정만 orchestration하고, Workout/Body/Exercise 통계와 `DashboardResult` 조립은 DashboardResultAssembler가 담당한다.

---

## 12. Routine → Workout 의존성 흐름

Routine은 WorkoutService 구현체를 직접 호출하지 않는다.

~~~text
RoutineController
      ↓
RoutineUseCase
      ↑
RoutineApplicationService
      │
      ├── ExerciseCatalogQuery
      │       exercise::catalog
      │
      └── WorkoutRoutineUseCase
              workout::routine-api
                    ↑
                    │ implements
          WorkoutRoutineApplicationService
                    │
                    ▼
               WorkoutService
~~~

WorkoutRoutineUseCase는 Routine에 필요한 projection만 반환한다. 일반 Workout REST Use Case는 `WorkoutApplicationService`, Routine 모듈용 공개 경계는 `WorkoutRoutineApplicationService`가 각각 구현해 변경 이유를 분리한다.

Routine이 직접 의존하지 않는 것:

- WorkoutService
- WorkoutRepositoryPort
- WorkoutRepositoryAdapter
- Workout Entity
- Workout Infrastructure

---

## 13. Exercise shared vocabulary

Exercise는 다른 기능의 기반이 되는 모듈이다.

Workout/Routine Domain에는 다음 개념이 실제 모델의 일부로 사용된다.

- ExerciseType
- ExerciseCategory
- ExerciseReference

이 때문에 exercise::domain-model은 의도적으로 공개한 작은 shared vocabulary다.

단, Exercise Application 구현체나 Persistence는 공개하지 않는다.

운동 목록/소유권 조회는 다음 In Port를 사용한다.

~~~text
exercise::catalog
        │
        ▼
ExerciseCatalogQuery
~~~

---

## 14. Spring Modulith 모듈 경계

최상위 package-info.java에 ApplicationModule을 선언한다.

현재 주요 허용 의존성:

| Module | allowedDependencies |
| --- | --- |
| exercise | 없음 |
| workout | exercise::catalog, exercise::domain-model |
| routine | exercise::catalog, exercise::domain-model, workout::routine-api |
| body | 없음 |
| nutrition | 없음 |
| dashboard | body::insight, workout::insight |
| ai | workout::insight, body::insight, nutrition::insight |
| user | 없음 |
| common | 각 모듈의 공개 exception interface |

다른 모듈에 공개할 패키지만 NamedInterface를 선언한다.

대표 공개 API:

~~~text
exercise::catalog
exercise::domain-model
workout::routine-api
workout::insight
body::insight
nutrition::insight
~~~

서비스 구현체, Repository Out Port, Infrastructure는 모듈 외부에 공개하지 않는다.

---

## 15. Spring Modulith와 ArchUnit 역할

두 도구의 책임을 나눈다.

### Spring Modulith

모듈 간 경계를 검증한다.

- application module 탐지
- 순환 의존
- allowedDependencies
- Named Interface 외 내부 패키지 접근
- 공개 API 범위

핵심 검증:

~~~java
ApplicationModules.of(MyFitnessAppApplication.class)
        .verify();
~~~

### ArchUnit

모듈 내부 계층 규칙을 검증한다.

- Domain → Application/Presentation/Infrastructure 금지
- Application → Presentation/Infrastructure 금지
- Presentation → Infrastructure 금지
- Presentation → Application Service 구현체 직접 의존 금지
- Presentation → JPA Entity 직접 의존 금지
- Application In Port → JPA Entity 직접 의존 금지
- Application Result field → JPA Entity 노출 금지
- JpaRepository는 Infrastructure에만 위치
- Repository Out Port는 Application에 위치하며 Spring Data 비의존
- Entity는 domain.model
- Service는 application.service
- RestController는 presentation.controller
- Repository Adapter는 Infrastructure

Spring Modulith는 module ↔ module,
ArchUnit은 module 내부 layer를 담당한다.

---

## 16. package-info.java

각 구현 모듈은 다음 문서를 유지한다.

~~~text
<module>/package-info.java
<module>/presentation/package-info.java
<module>/application/package-info.java
<module>/application/port/package-info.java
<module>/application/port/in/package-info.java
<module>/application/port/out/package-info.java
<module>/domain/package-info.java
<module>/infrastructure/package-info.java
~~~

외부에 공개할 세부 In Port에는 NamedInterface를 선언한다.

~~~java
@NamedInterface("insight")
package com.myfitness.workout.application.port.in.insight;
~~~

Javadoc은 사람을 위한 설명이고, Spring Modulith annotation은 실제 모듈 검증 입력이다.

---

## 17. AI Coach 구조

Phase 6 AI Coach도 동일한 In/Out Port 규칙을 사용한다.

~~~text
전역 AI FAB / REST
        │
        ▼
AiCoachController
        │
        ▼
AiCoachUseCase                         IN PORT
        ▲
        │ implements
AiCoachService
        │
        ├── AiQueryRouter
        ├── AiHistorySelector
        ├── AiProviderExecutor
        ├── AiContextBuilder
        │      ├── AiWorkoutContextBuilder   → WorkoutInsightQuery
        │      ├── AiBodyContextBuilder      → BodyInsightQuery
        │      └── AiNutritionContextBuilder → NutritionInsightQuery
        │
        ├── AiConversationRepositoryPort
        ├── AiMessageRepositoryPort
        ├── AiRequestLogRepositoryPort
        │          ▲
        │          │ implements
        │      Persistence Adapters
        │
        └── AiChatGateway                OUT PORT
                   ▲
                   │ implements
            SpringAiChatGateway
                   │
                   ▼
            Spring AI ChatModel
              ├── OpenAI
              └── Ollama
~~~

AI는 다른 기능 모듈의 Repository Out Port나 Infrastructure를 직접 조회하지 않는다.
질문에 필요한 기록은 각 모듈의 읽기 전용 Insight Named Interface로만 가져온다.

정확한 계산은 Java에서 처리한다.

- 최근 7일/30일 운동 횟수
- 기간별 Volume과 직전 기간 대비 변화율
- 종목 최고 중량과 Epley 추정 1RM
- 최신 신체 기록과 직전 기록 대비 변화
- 영양 목표 대비 섭취량과 남은 macro

LLM에는 위 계산 결과와 질문에 필요한 최소 Context만 전달한다.

AI Context를 만들기 위해 기능 모듈의 전체 이력을 무조건 조회하지 않는다.

- Body: 최신/직전 비교에 필요한 최근 2건만 조회
- Workout: 7일/30일 집계에 필요한 최근 30일 완료 Workout만 조회
- Nutrition: 선택 날짜 식단과 목표를 조회하고, 음식 추천용 사용 이력은 최근 50건으로 제한

Dashboard처럼 전체 기간 Read Model이 필요한 소비자는 기존 전체 Insight API를 유지하고, AI는 별도의 bounded Insight Query를 사용한다.

Conversation 전체를 매 호출마다 보내지 않는다. 현재 기본값은 최근 8개 Message, 최대 4,000자로 제한한다. 단일 영역 질문은 같은 영역 또는 COMPOSITE History만 전달하고 OUT_OF_SCOPE 및 다른 영역 대화는 제외한다.
질문은 Java Router에서 WORKOUT / NUTRITION / BODY / GENERAL_FITNESS / COMPOSITE / OUT_OF_SCOPE로 분류하며 명백한 범위 밖 질문은 Provider를 호출하지 않는다. COMPOSITE는 AiContextSelector가 질문 키워드를 다시 확인해 실제 필요한 영역만 Context로 구성하고, 영역이 드러나지 않는 Dashboard 종합 질문에서만 Workout/Body/Nutrition 전체 요약을 사용한다.

AI Provider는 Spring AI ChatModel 뒤에 격리한다.

~~~text
AI_PROVIDER=none    # AI 호출 비활성화, 나머지 앱 정상 동작
AI_PROVIDER=openai  # 초기 운영 Provider
AI_PROVIDER=ollama  # 로컬 모델 전환
~~~

Provider와 모델은 환경 설정으로 선택하며 Application/REST/Frontend 코드는 변경하지 않는다.
raw prompt/completion observability logging은 활성화하지 않고 RequestLog에는 provider/model/token/latency/context type 같은 메타데이터만 저장한다.

채팅 저장은 긴 Provider 호출보다 먼저 수행한다. User Message와 Conversation 갱신이 완료된 뒤 Provider를 호출하고, 응답 성공 시 Assistant Message를 저장한다. 따라서 서버가 요청을 수신한 이후 Provider 실패나 클라이언트 UI 닫힘이 발생해도 User Message는 보존된다. Frontend의 AI POST는 page unload 상황을 보강하기 위해 fetch keepalive를 사용한다.

AI가 직접 접근하지 않는 것:

- 다른 모듈 Repository Out Port
- 다른 모듈 Infrastructure
- Spring Data Repository
- DB
- OpenAI/Ollama SDK를 Application에서 직접 사용

---

## 18. 의존성 규칙 요약

### 모듈 내부

~~~text
Presentation
      │
      ▼
   IN PORT
      ▲
      │ implements
Application Service
      │
      ├────────► Domain
      │
      ▼
   OUT PORT
      ▲
      │ implements
Infrastructure Adapter
~~~

### 모듈 간

~~~text
Module A Application/Adapter
        │
        ▼
Module B Named In Port
~~~

원칙:

- 다른 모듈의 Service 구현체를 직접 호출하지 않는다.
- 다른 모듈의 Repository Out Port를 직접 호출하지 않는다.
- 다른 모듈의 Infrastructure를 직접 호출하지 않는다.
- 필요한 기능은 상대 모듈의 Named In Port로 공개한다.
- Exercise Domain Model은 작은 shared vocabulary로 명시적으로 공개한다.

---

## 19. DB와 배포

현재는 단일 배포 Modular Monolith다.

- Spring Boot JAR 1개
- Next.js static export 포함
- PostgreSQL
- Flyway schema history
- AI Provider는 Spring AI를 통해 OpenAI 또는 Ollama를 설정으로 선택

현재 요구사항에 불필요한 MSA, Kafka, Redis, Kubernetes는 도입하지 않는다.

---

## 20. 변경 체크리스트

새 기능 또는 리팩토링 시 다음을 확인한다.

1. Controller가 In Port를 호출하는가?
2. Application이 기술 구현이 아니라 Out Port를 호출하는가?
3. Persistence Adapter에만 @Repository가 붙어 있는가?
4. 일반 Adapter는 역할에 맞는 @Component 등을 사용하는가?
5. 다른 모듈 Service/Repository/Infrastructure를 직접 참조하지 않는가?
6. 모듈 간 호출이 Named In Port를 통하는가?
7. Domain이 외부 계층을 참조하지 않는가?
8. In Port와 Presentation이 JPA Entity를 직접 노출/참조하지 않는가?
9. Application Result field에 JPA Entity가 포함되지 않는가?
10. Entity → Result 변환이 Application transaction 안에서 끝나는가?
11. Spring Modulith verify가 통과하는가?
12. ArchUnit 테스트가 통과하는가?
13. DB 변경은 Flyway migration으로만 수행되는가?

구조를 단순히 디렉터리 모양으로 유지하는 것이 아니라 의존성 방향이 코드와 테스트에서 실제로 강제되는 상태를 유지한다.
