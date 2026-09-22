# My Fitness Architecture

이 문서는 프로젝트의 현재 구조와 주요 책임, 그리고 계층 간 의존성 규칙을 정의한다.
구조 변경은 코드와 같은 작업 단위에서 이 문서를 함께 갱신한다.

## 1. 전체 구조

\`\`\`text
┌──────────────────────────────┐
│       Next.js PWA            │
│ workout / routine / body     │
│ dashboard / nutrition / ai   │
└──────────────┬───────────────┘
               │ REST
               ▼
┌──────────────────────────────────────────┐
│               Spring Boot                │
│                                          │
│ exercise / workout / routine / body      │
│ nutrition / dashboard / user / ai        │
│                                          │
│ presentation → application → domain      │
│                    ▲             ▲       │
│                    └ infrastructure ┘     │
└─────────────────┬────────────────────────┘
                  │
                  ▼
             PostgreSQL
                  │
             Flyway V1~V5

Phase 6:
AI Application Port
      ▲
      │
AI Infrastructure Adapter
      │
Spring AI / external model provider
\`\`\`

## 2. 현재 기술 기준

- Java: 21
- Spring Boot: 4.1.1
- Database: PostgreSQL 17 + Flyway
- Persistence: Spring Data JPA
- Main runtime DDL: Hibernate \`ddl-auto: none\`
- Test DDL: 테스트 전용 H2 환경에서 \`create-drop\`
- Frontend: Next.js 16 + TypeScript + PWA
- Architecture test: ArchUnit
- AI: Phase 6에서 Spring AI 기반 외부 provider adapter 연결 예정

테이블, 인덱스, 제약조건, seed 변경은 Flyway migration으로만 관리한다.
현재 구조 리팩토링에서는 V1~V5 schema와 REST API를 변경하지 않는다.

## 3. 배포 단위

현재는 Modular Monolith 한 개를 유지한다.

- Git 저장소 하나
- Spring Boot 배포 단위 하나
- Next.js는 static export 후 Spring Boot JAR에 포함
- PostgreSQL은 별도 프로세스 또는 별도 DB 서비스
- 사용자 규모가 작으므로 불필요한 MSA, Kafka, Redis를 추가하지 않는다.
- AI 모델 호출은 Phase 6에서 Infrastructure Adapter로 격리한다.

## 4. 백엔드 모듈

기능 기준 최상위 모듈:

\`\`\`text
com.myfitness
├── exercise
├── workout
├── routine
├── body
├── nutrition
├── dashboard
├── user
├── ai
└── common
\`\`\`

Exercise는 Workout 내부 개념이 아니라 독립 도메인이다.

\`\`\`text
Workout ─────┐
             ├──> Exercise
Routine ─────┘
AI (future) ─┘
\`\`\`

기본 운동과 사용자 커스텀 운동의 PK는 겹칠 수 있으므로
운동 참조 키는 항상 \`ExerciseType + exerciseId\`를 함께 사용한다.

## 5. 도메인 내부 4계층

각 기능 모듈은 아래 구조를 기준으로 한다.

\`\`\`text
<domain>/
├── presentation
│   ├── controller
│   └── dto
│       ├── request
│       └── response
│
├── application
│   ├── service
│   ├── command
│   ├── result
│   ├── port
│   └── exception
│
├── domain
│   ├── model
│   ├── repository
│   └── exception
│
└── infrastructure
    ├── persistence
    ├── query
    ├── bootstrap
    └── external
\`\`\`

도메인마다 필요하지 않은 하위 패키지는 만들지 않는다.
예를 들어 Dashboard는 쓰기 Aggregate가 없으므로 \`domain.model\` 대신 계산 서비스와 Query Port 중심으로 구성한다.

### Presentation

HTTP와 외부 입력/출력 형식에 대한 책임만 가진다.

- REST Controller
- Bean Validation
- request / response DTO
- Domain/Application Result → JSON response 변환
- HTTP 상태 및 공통 오류 응답

Presentation DTO는 Application에서 import하지 않는다.

### Application

Use Case와 여러 Domain을 조합하는 orchestration을 담당한다.

- Application Service
- Command / Result
- 외부 기능을 위한 Port
- 소유권/조회 실패 등 Use Case 수준 예외
- Transaction boundary

Application은 Domain에 의존할 수 있다.
Application은 Presentation DTO와 Infrastructure 구현을 직접 알지 않는다.

Routine으로 Workout을 시작하는 것처럼 여러 도메인을 연결해야 하는 흐름은
Domain Entity가 아니라 Application에서 조합한다.

### Domain

비즈니스 규칙과 핵심 모델을 가진다.

- Entity / Value Object / Enum
- 비즈니스 규칙
- Domain Exception
- Repository Port

Domain은 Application, Presentation, Infrastructure에 의존하지 않는다.

### Infrastructure

기술 구현을 담당하는 Adapter 계층이다.

- Spring Data JPA Repository
- Domain Repository Port 구현체
- Dashboard Query Adapter
- 기본 운동 seed bootstrap
- Phase 6 AI provider client
- 향후 외부 API client

Infrastructure는 Application Port 또는 Domain Repository Port를 구현한다.

## 6. 의존성 방향

허용하는 기본 방향:

\`\`\`text
Presentation ───────► Application ───────► Domain
      │                                      ▲
      └──────────────────────────────────────┘
                                             │
Infrastructure ─────► Application Port ──────┤
Infrastructure ───────────────► Domain Port ─┘
\`\`\`

금지:

\`\`\`text
Domain         -X-> Application
Domain         -X-> Presentation
Domain         -X-> Infrastructure

Application    -X-> Presentation
Application    -X-> Infrastructure

Presentation   -X-> Infrastructure
\`\`\`

이 규칙은 \`LayerArchitectureTest\`의 ArchUnit 테스트로 검증한다.

## 7. JPA와 Domain 모델

현재 프로젝트는 Domain Entity와 JPA Entity를 별도로 복제하지 않는다.

예:

\`\`\`java
package com.myfitness.workout.domain.model;

@Entity
public class Workout {
    ...
}
\`\`\`

즉 Domain은 \`jakarta.persistence\` annotation 사용을 허용한다.

이 선택의 이유:

- 현재는 개인용 Modular Monolith이다.
- JPA를 교체할 요구가 없다.
- Domain/Persistence 모델을 별도 생성하면 Entity와 Mapper가 거의 두 배가 된다.
- 핵심 목표는 Framework 완전 제거가 아니라 의존성 방향과 업무 경계를 명확히 하는 것이다.

따라서 **Practical Clean Architecture**를 사용한다.

Spring \`@Service\`, \`JpaRepository\`, Controller DTO 같은 외부 계층 요소는 Domain에서 금지하지만,
JPA mapping annotation은 허용한다.

## 8. Repository Port / Adapter

Domain Repository는 Spring Data를 알지 않는다.

\`\`\`text
application.service.WorkoutService
              │
              ▼
domain.repository.WorkoutRepository
              ▲
              │ implements
infrastructure.persistence.WorkoutRepositoryAdapter
              │
              ▼
infrastructure.persistence.SpringDataWorkoutRepository
              │
              ▼
          PostgreSQL
\`\`\`

예시:

\`\`\`java
public interface WorkoutRepository {
    Workout save(Workout workout);
    Optional<Workout> findById(Long id);
    List<Workout> findByUserIdAndDateRange(...);
}
\`\`\`

\`JpaRepository\`를 상속하는 인터페이스는 Infrastructure 안에만 둔다.

## 9. 도메인별 구조

### Exercise

\`\`\`text
exercise
├── presentation
│   └── ExerciseController
├── application
│   └── ExerciseService
├── domain
│   ├── Exercise
│   ├── CustomExercise
│   ├── ExerciseReference
│   ├── ExerciseType
│   ├── ExerciseCategory
│   └── repository ports
└── infrastructure
    ├── persistence adapters
    └── default exercise bootstrap
\`\`\`

공용 기본 Exercise와 사용자 CustomExercise를 함께 제공한다.

### Workout

Workout Aggregate:

\`\`\`text
Workout
  └── WorkoutExercise
        └── WorkoutSet
\`\`\`

Workout은 Exercise 자체를 소유하지 않고
\`ExerciseReference\`를 받아 기록 시점의 이름/카테고리를 snapshot으로 저장한다.

HTTP DTO는 Presentation에만 있으며,
Application은 \`WorkoutSetCommand\`, Domain 객체, \`WorkoutCalendarDayResult\`를 사용한다.

### Routine

Routine Domain은 반복 운동 템플릿만 책임진다.
Routine → Workout 시작은 Application orchestration이다.

\`\`\`text
RoutineController
       ↓
RoutineApplicationService
  ├── RoutineService
  ├── ExerciseService
  └── WorkoutService
\`\`\`

Routine Domain이 Workout Application/Infrastructure를 직접 참조하지 않는다.

### Body

BodyRecord Domain은 측정값 규칙을 담당한다.
Trend 조합은 \`BodyTrendResult\`를 만드는 Application 책임이다.

### Nutrition

Food / Meal / MealFood / NutritionGoal을 Domain에 둔다.

- MealFood는 Food 영양정보 snapshot 유지
- 최근/자주 먹는 음식 계산은 Application
- HTTP Response 조립은 Presentation

### Dashboard

Dashboard는 쓰기 Aggregate가 아니라 Read Model이다.

\`\`\`text
DashboardController
       ↓
DashboardService
       ↓
DashboardQueryPort
       ▲
       │
DashboardQueryAdapter
  ├── WorkoutRepository Port
  └── BodyRecordRepository Port
\`\`\`

Application이 다른 도메인의 Persistence 구현을 직접 참조하지 않는다.
Volume, 1RM, 변화율 계산은 Dashboard Domain Service에서 수행한다.

### AI / User

아직 본 기능 구현 전이지만 동일한 4계층 package boundary를 미리 유지한다.

Phase 6에서는 예를 들어:

\`\`\`text
ai.presentation
    ↓
ai.application
    ├── Workout/Body/Nutrition Query Port
    └── AiClientPort
              ▲
              │
ai.infrastructure
    └── OpenAI/Spring AI Adapter
\`\`\`

형태로 확장한다.

## 10. 예외 배치

예외 역시 성격에 따라 계층을 나눈다.

Domain 규칙 위반:

\`\`\`text
workout.domain.exception.WorkoutRuleException
routine.domain.exception.RoutineRuleException
nutrition.domain.exception.NutritionRuleException
\`\`\`

Use Case 조회/권한 오류:

\`\`\`text
workout.application.exception.WorkoutNotFoundException
workout.application.exception.WorkoutAccessException
\`\`\`

HTTP 변환:

\`\`\`text
common.presentation.exception.GlobalExceptionHandler
\`\`\`

## 11. 프론트엔드 책임

Next.js PWA가 담당한다.

- 모바일 우선 UI
- Workout 기록과 임시 저장
- Routine 조회 / 등록·수정
- BodyRecord 관리
- Dashboard 추이 시각화
- Nutrition 요약·목표 / 식단 기록 / 음식 관리
- Spring REST API 호출
- PWA 설치와 기본 캐싱
- Phase 6 AI 화면

Next.js 서버 기능에 의존하지 않고 Spring REST API를 runtime backend로 사용한다.

## 12. 데이터 흐름

일반 쓰기/조회:

\`\`\`text
PWA
 ↓
Presentation
 ↓
Application
 ↓
Domain Repository Port
 ↑
Infrastructure Adapter
 ↓
Spring Data JPA
 ↓
PostgreSQL
\`\`\`

Dashboard:

\`\`\`text
Presentation
 ↓
Dashboard Application
 ↓
DashboardQueryPort
 ↑
Dashboard Infrastructure Adapter
 ↓
Workout / Body Domain Repository Ports
\`\`\`

Phase 6 AI:

\`\`\`text
PWA
 ↓
AI Presentation
 ↓
AI Application
 ├── 정확한 수치/기록 조회
 └── AiClientPort
          ▲
          │
   Infrastructure Adapter
          ↓
      AI Provider
\`\`\`

수치 계산과 데이터 필터링은 Java에서 수행하고,
LLM은 설명과 추천 문장 생성에 집중한다.

## 13. 단계별 확장

1. Workout — 완료
2. Routine — 완료
3. BodyRecord — 완료
4. Dashboard — 완료
5. Nutrition — 완료
6. AI Coach — 다음 단계
7. 이후 확장 — 사진, 알림, AI Insight, RAG

## 14. 변경 원칙

- 구조 변경은 architecture 문서와 같은 작업 단위에서 반영한다.
- DB schema 변경은 Flyway migration으로만 수행한다.
- REST contract 변경은 별도 기능 요구가 있을 때만 수행한다.
- Domain은 외부 계층에 의존하지 않는다.
- Application은 Infrastructure 구현을 직접 참조하지 않는다.
- 새 Repository는 Domain Port + Infrastructure Adapter 형태를 우선한다.
- Architecture rule 위반은 ArchUnit 테스트 실패로 차단한다.
- 현재 사용자 규모에 불필요한 분산 구성은 도입하지 않는다.
