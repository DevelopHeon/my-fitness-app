# My Fitness - Product & Technical Specification

> 상태: Draft v0.1  
> 목적: 1~2명이 사용하는 개인용 헬스 기록, 식단 관리, 로컬 AI 코치 애플리케이션

## 1. 프로젝트 목표

My Fitness는 운동 기록과 식단/신체 데이터를 한 곳에 축적하고, 통계와 로컬 LLM을 이용해 사용자가 자신의 변화를 쉽게 이해할 수 있도록 하는 개인용 피트니스 애플리케이션이다.

초기 목표는 범용 상용 서비스가 아니라 **본인이 매일 실제로 사용할 수 있는 기록 앱**을 완성하는 것이다. 사용자는 최대 1~2명을 가정하며, 대규모 트래픽이나 분산 시스템은 고려하지 않는다.

핵심 가치는 다음과 같다.

- 운동 기록 입력이 빠르고 반복 사용하기 쉬울 것
- 이전 기록과 현재 기록을 쉽게 비교할 수 있을 것
- 체중, 체지방, 골격근, 운동량의 변화를 한 화면에서 확인할 수 있을 것
- 식단의 칼로리 및 탄수화물/단백질/지방을 기록할 수 있을 것
- 로컬 LLM이 사용자 기록을 바탕으로 질의 응답과 간단한 분석을 제공할 것
- 개인 운동/식단 데이터를 외부 LLM API에 보내지 않는 구성을 우선할 것

## 2. 기술 방향

프로젝트는 하나의 저장소와 하나의 Spring Boot 배포 단위를 사용한다.

- Backend: Java + Spring Boot
- Persistence: Spring Data JPA + PostgreSQL + Flyway
- Frontend: Next.js + TypeScript
- UI: Tailwind CSS
- Client: PWA
- Local AI: Ollama
- AI Integration: Spring AI
- Chart: Recharts 또는 동급의 경량 차트 라이브러리
- Deployment: Docker 기반 단일 애플리케이션 배포
- Initial Infra: PostgreSQL + Spring Boot + Ollama

Redis, Kafka, Elasticsearch, Kubernetes, MSA, CQRS, Event Sourcing은 초기 범위에서 제외한다.

## 3. 단일 모듈 / 단일 배포 구조

Next.js는 독립적인 서비스로 운영하지 않고 프론트엔드 빌드 결과물을 Spring Boot의 정적 리소스로 포함시키는 방식을 우선한다.

```text
my-fitness/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/          # frontend build 결과
│   └── test/
├── frontend/
│   ├── package.json
│   ├── next.config.*
│   ├── app/
│   ├── public/
│   └── ...
├── docs/
│   └── SPEC.md
└── docker/
```

빌드 흐름은 다음과 같이 구성한다.

```text
Next.js PWA build
        ↓
frontend/out
        ↓
Spring resources/static 복사
        ↓
Spring Boot build
        ↓
단일 실행 JAR / Docker Image
```

프론트에서 필요한 서버 기능은 Spring REST API를 사용하고, Next.js 자체 서버 기능에는 의존하지 않는 것을 기본 원칙으로 한다.
## 4. 개발 단계

기능은 실제 사용 가능성을 우선하여 아래 순서로 개발한다.

### Phase 1. Workout 기록

목표: 운동을 시작하고 세트 단위로 기록하며, 이전 운동 기록을 확인할 수 있어야 한다.

필수 기능:
- 운동 세션 생성
- 운동 종목 추가
- 세트 추가/수정/삭제
- 세트별 중량, 횟수, 운동 시간 기록
- 운동별 메모
- 운동 완료 처리
- 동일 종목의 이전 기록 조회
- 최근 운동 기록 조회

주요 화면:
- 오늘 운동
- 운동 종목 선택
- 세트 입력
- 이전 기록
- 운동 완료 요약

완료 기준:
- 사용자가 헬스장에서 스마트폰으로 실제 운동 1회를 처음부터 끝까지 기록할 수 있다.
- 동일 운동을 다시 수행할 때 직전 세트 기록을 확인할 수 있다.
### Phase 2. Routine

목표: 자주 사용하는 운동 조합을 저장하고 반복 입력을 최소화한다.

필수 기능:
- 루틴 생성/수정/삭제
- 루틴에 운동 종목 및 순서 저장
- Push / Pull / Legs 등 사용자 정의 루틴 지원
- 저장된 루틴으로 새로운 Workout 시작
- 시작 시 직전 기록을 함께 노출
- 루틴 수행 중 운동 추가/삭제 허용

주요 화면:
- 루틴 목록
- 루틴 편집
- 루틴으로 운동 시작

완료 기준:
- 루틴 선택 후 별도의 운동 종목 검색 없이 바로 운동 기록을 시작할 수 있다.

### Phase 3. BodyRecord

목표: 신체 변화 데이터를 날짜 기준으로 축적한다.

필수 기록:
- 체중
- 체지방률
- 골격근량
- 기록 일시
- 선택 메모
추가 원칙:
- 하루 여러 번 기록할 수 있도록 데이터 모델은 제한하지 않는다.
- Dashboard에서는 최신 기록 또는 일 단위 대표값을 사용한다.
- 측정 단위는 초기에는 kg / %로 고정한다.

완료 기준:
- 날짜별 신체 기록을 추가/수정/삭제하고 최근 변화 추이를 조회할 수 있다.

### Phase 4. Dashboard

목표: 기록된 데이터를 숫자와 차트로 빠르게 이해할 수 있게 한다.

필수 지표:
- 주/월 운동 횟수
- 총 운동 Volume
- 이전 기간 대비 Volume 변화
- 체중 변화
- 체지방률 및 골격근량 변화
- 종목별 최고 중량
- 종목별 최고 추정 1RM
- 종목별 최근 기록 추이

Volume 기본 계산식:

```text
set volume = weight × reps
workout volume = Σ set volume
```

추정 1RM은 초기에는 Epley 공식을 사용한다.

```text
estimated 1RM = weight × (1 + reps / 30)
```

통계와 수치 계산은 서버에서 수행하며 LLM에게 산술 계산을 위임하지 않는다.
### Phase 5. Nutrition

목표: 자주 먹는 음식을 재사용하면서 하루 식단과 영양 섭취량을 기록한다.

필수 기능:
- 사용자 음식 등록/수정/삭제
- 1회 제공량 기준 칼로리 저장
- 탄수화물/단백질/지방 저장
- 아침/점심/저녁/간식 식사 기록
- 음식 검색 및 최근/자주 먹는 음식 재사용
- 일별 총 칼로리 및 탄단지 합계
- 사용자 목표 칼로리 및 탄단지와 비교

초기 범위에서 대규모 외부 음식 DB 연동은 제외한다.

완료 기준:
- 자주 먹는 음식은 재입력 없이 선택하여 식단에 추가할 수 있다.
- 하루 섭취량과 목표량 차이를 즉시 확인할 수 있다.

### Phase 6. Local AI Coach - Ollama

목표: 로컬 LLM이 사용자의 운동/식단 데이터를 조회하여 간단한 질의응답, 기록 해석, 식단 제안을 제공한다.

초기 기능:
- 일반적인 운동/식단 질의응답
- 최근 운동 빈도/볼륨 변화 설명
- 특정 운동의 최근 기록 및 성장 추이 설명
- 최근 체중/체성분 변화 요약
- 최근 섭취량과 영양 목표 비교
- 남은 칼로리/탄단지를 고려한 간단한 식단 후보 제안
AI 처리 원칙:
- LLM은 DB에 직접 접근하지 않는다.
- 사용자 데이터 조회는 Spring AI Tool Calling을 통해 애플리케이션 서비스가 수행한다.
- Volume, 평균, 증감률, 목표 대비 차이, 1RM 등 정확성이 필요한 계산은 Java 코드에서 수행한다.
- LLM은 계산 결과를 자연어로 설명하고 선택지를 제안하는 역할에 집중한다.
- 데이터가 부족하면 추측하지 않고 기록이 부족하다고 답한다.
- 의료 진단, 치료 판단, 질병 관련 식단 처방은 범위에서 제외한다.
- 사용자의 userId는 모델 입력에 의존하지 않고 서버 요청 컨텍스트에서 강제한다.

초기 Tool 후보:
- getRecentWorkouts
- getWorkoutSummary
- getExerciseHistory
- getExerciseProgress
- getPersonalRecords
- getBodyTrend
- getNutritionSummary
- getNutritionGoal
- getRecentMeals
- getRemainingNutritionTarget

예시 질의:
- "최근 한 달 동안 벤치프레스 얼마나 늘었어?"
- "이번 주 운동량이 지난주보다 줄었어?"
- "최근 2주 단백질 평균이 목표에 비해 어때?"
- "오늘 남은 칼로리와 단백질을 기준으로 저녁 식단 추천해줘."
- "최근 체중은 줄었는데 운동 퍼포먼스는 어떻게 변했어?"

RAG는 Phase 6에 포함하지 않는다. 운동/영양 문서 검색 기반의 RAG는 실제 기록 기반 Tool Calling이 안정화된 이후 확장 기능으로 검토한다.
### Phase 7. 이후 확장

Phase 1~6이 실제 사용 가능한 수준으로 안정화된 후 검토한다.

후보 기능:
- 식단 사진 업로드
- 이미지 기반 음식 후보 인식
- AI 식단 분석 보조
- 운동 일정/기록 알림
- 목표 달성 알림
- 주간/월간 AI Insight 자동 생성
- 운동/영양 지식 RAG
- 데이터 Export / Backup
- 필요 시 외부 음식 데이터 소스 연동

## 5. 애플리케이션 아키텍처

```text
┌─────────────────────────────────────┐
│           Next.js PWA               │
│ workout / routine / body / meal     │
│ dashboard / ai chat                 │
└────────────────┬────────────────────┘
                 │ REST / SSE
                 ▼
┌─────────────────────────────────────┐
│            Spring Boot              │
│                                     │
│ user                                │
│ workout     routine                 │
│ body        nutrition               │
│ dashboard   ai                      │
└───────────┬───────────────┬─────────┘
            │               │
      PostgreSQL         Spring AI
                            │
                            ▼
                          Ollama
```
Spring Boot는 기능 단위 패키지를 기본으로 하고, 각 기능 내부는 실용적인 역할 단위로 분리한다.

```text
com.myfitness
├── user
├── workout
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── service
│   ├── domain
│   ├── repository
│   └── exception
├── routine
├── body
├── nutrition
├── dashboard
├── ai
└── common
    ├── config
    └── exception
```

Controller는 여러 하위 Service를 직접 조합하지 않고 해당 도메인의 Application Service를 단일 진입점으로 사용한다. Application Service가 필요한 Service와 Domain을 조합하고 트랜잭션 경계를 가진다.

엄격한 헥사고날/DDD 계층을 그대로 적용하기보다, 패키지 책임이 명확하고 테스트하기 쉬운 정도로만 분리한다. 마이크로서비스 분리는 고려하지 않는다.

AI 패키지는 다음 책임을 가진다.

```text
ai
├── presentation     # chat API, streaming endpoint
├── application
│   ├── AiChatService
│   └── AiCoachService
├── tool             # Spring AI Tool 정의
├── prompt           # system prompt / prompt template
└── config           # Ollama / ChatClient 설정
```

도메인 데이터 조회는 AI 전용 Repository를 새로 만들기보다 기존 application service 또는 전용 query service를 통해 수행한다.

## 6. 핵심 도메인 모델

초기 엔티티 후보:

- User
- Exercise
- Workout
- WorkoutExercise
- WorkoutSet
- Routine
- RoutineExercise
- BodyRecord
- Food
- Meal
- MealFood
- NutritionGoal
핵심 관계:

```text
User
├── Workout
│   └── WorkoutExercise
│       └── WorkoutSet
├── Routine
│   └── RoutineExercise
├── BodyRecord
├── Food
├── Meal
│   └── MealFood
└── NutritionGoal

Exercise
├── WorkoutExercise
└── RoutineExercise
```

설계 원칙:
- 모든 사용자 데이터는 User 소유 관계를 명확히 가진다.
- Exercise는 운동 종목의 정의이고 WorkoutExercise는 특정 운동 세션에서 수행된 운동 기록이다.
- Routine은 템플릿이며 실제 수행 결과는 Workout에 별도로 저장한다.
- MealFood에는 기록 시점의 영양 정보를 스냅샷으로 저장해 이후 Food 수정으로 과거 기록이 변하지 않게 한다.
- WorkoutSet 역시 당시 수행 값을 그대로 보존한다.
- 삭제 정책은 초기에는 물리 삭제를 기본으로 하되, 실제 사용 중 복구 요구가 생기면 soft delete를 도입한다.

## 7. API 기본 정책

Base path는 `/api`를 사용한다.

초기 API 그룹:
- `/api/workouts`
- `/api/exercises`
- `/api/routines`
- `/api/body-records`
- `/api/foods`
- `/api/meals`
- `/api/nutrition-goals`
- `/api/dashboard`
- `/api/ai/chat`

응답은 화면 중심 DTO를 사용하며 JPA Entity를 API 응답으로 직접 노출하지 않는다.
## 8. 사용자 및 인증

초기 사용자는 최대 1~2명으로 가정한다.

- 사용자별 데이터는 반드시 분리한다.
- 인증은 이메일/비밀번호 기반의 단순한 Spring Security 구성을 우선한다.
- 외부 OAuth 로그인은 초기 범위에서 제외한다.
- API 요청에서 인증된 사용자 기준으로 데이터 범위를 제한한다.
- AI Tool 호출 시에도 userId를 모델이 생성한 인자에서 받지 않고 서버 인증 컨텍스트에서 결정한다.

## 9. PWA 및 화면 요구사항

목표는 모바일 브라우저에서 설치 가능한 앱 수준의 사용성을 제공하는 것이다.

화면은 별도의 복잡한 디자인 시스템 구축보다 **깔끔하고 일관된 테마, 충분한 여백, 명확한 정보 계층, 빠른 입력 UX**를 우선한다. 기능 검증과 실제 사용성을 해치지 않는 범위에서 단순한 시각 디자인을 유지한다.

필수 항목:
- Web App Manifest
- 앱 아이콘 및 홈 화면 설치
- 모바일 우선 반응형 UI
- 기본 화면 캐싱
- 네트워크 오류 상태 처리
- 운동 중 화면 입력이 끊기지 않는 UX
- 상대 경로 기반 Spring API 호출

초기에는 완전한 오프라인 쓰기 동기화까지 구현하지 않는다. 네트워크가 끊겼을 때 작성 중인 입력값이 가능한 한 보존되도록 클라이언트 상태 처리부터 적용한다.

## 10. Ollama / AI 상세 정책

Ollama는 Spring Boot와 별도 프로세스로 실행하되 외부 사용자에게 직접 노출하지 않는다.

```text
Browser
  ↓
Spring Boot /api/ai/chat
  ↓
Spring AI
  ↓
Ollama localhost
```

프론트엔드는 Ollama API를 직접 호출하지 않는다.
AI 응답 정책:
- 가능한 경우 Tool 조회 결과를 근거로 답한다.
- 기록이 없거나 조회 기간이 짧으면 그 한계를 명시한다.
- 사용자의 목표와 실제 섭취량을 구분해서 표현한다.
- 식단 추천은 하나의 정답이 아니라 조건에 맞는 후보를 제안하는 방식으로 제공한다.
- 모델 응답에 계산된 수치를 넣을 때 서버에서 생성된 값을 우선한다.
- 대화 기록 저장은 초기에는 최소화하고 필요성이 확인되면 별도 ChatSession 모델을 추가한다.
- 스트리밍 응답이 필요할 경우 SSE를 우선 검토한다.

초기 모델 선정 기준:
- 로컬 장비에서 무리 없이 실행 가능할 것
- 한국어 응답 품질이 충분할 것
- Tool Calling을 안정적으로 지원할 것
- 응답 속도가 모바일 사용성을 크게 해치지 않을 것

특정 모델명은 스펙에 고정하지 않고 실제 Mac 환경에서 2~3개 후보를 비교한 뒤 결정한다.

## 11. 테스트 전략

테스트의 목적은 코드 커버리지 자체가 아니라 **비즈니스 규칙이 깨지지 않는지 검증하는 것**이다.

각 Phase는 다음 순서로 개발한다.

1. 핵심 비즈니스 규칙을 테스트 케이스로 먼저 정의한다.
2. 도메인/애플리케이션 서비스 테스트를 작성한다.
3. 구현 후 테스트를 통과시킨다.
4. 조회/영속화 경계가 중요한 경우 Repository 통합 테스트를 추가한다.
5. 핵심 사용자 흐름은 REST API 통합 테스트로 검증한다.
6. 전체 테스트와 빌드가 통과한 뒤에만 커밋·푸시한다.

우선순위:
1. 운동 상태 변경, 사용자 데이터 소유권, Volume/PR/1RM 등 비즈니스 규칙 단위 테스트
2. Workout / Routine / Nutrition 애플리케이션 서비스 테스트
3. 이전 기록 조회, 기간 집계 등 Repository 통합 테스트
4. 주요 REST API 통합 테스트
5. AI Tool 입출력 테스트
6. Ollama 연동 smoke test

LLM의 자연어 문장 자체를 정확 문자열로 검증하지 않는다. 대신 호출해야 할 Tool, Tool 결과 스키마, 필수 응답 조건을 검증한다.

AI 없이도 Workout, Routine, BodyRecord, Dashboard, Nutrition의 핵심 기능은 정상 동작해야 한다.

세부 원칙은 `docs/testing/AUTOMATED_TEST_STRATEGY.md`를 기준으로 한다.
## 12. 배포 및 운영

초기 운영은 단순성을 우선한다.

- Spring Boot 애플리케이션 1개
- PostgreSQL 1개
- Ollama 1개
- Docker Compose 기반 로컬/개인 서버 구성을 우선
- 프론트엔드는 Spring Boot 배포물에 포함
- 환경 변수로 DB 및 Ollama 연결 정보를 분리
- 주기적인 PostgreSQL 백업 방법을 마련
- 로그는 파일 또는 stdout 기반으로 시작

트래픽 규모가 작으므로 Redis, 메시지 브로커, 별도 API Gateway는 도입하지 않는다.

## 13. MVP 완료 기준

MVP는 Phase 1~6 완료 시점으로 정의한다.

사용자가 다음 흐름을 실제로 수행할 수 있어야 한다.

1. 로그인한다.
2. Routine을 선택해 운동을 시작한다.
3. 이전 기록을 보며 세트별 중량과 횟수를 입력한다.
4. 운동 종료 후 오늘의 Volume과 주요 기록을 확인한다.
5. 체중/체지방/골격근을 기록한다.
6. 하루 식단과 탄단지/칼로리를 기록한다.
7. Dashboard에서 최근 운동 및 신체 변화를 확인한다.
8. AI에게 자신의 최근 운동/식단에 대해 질문한다.
9. AI가 Tool을 통해 실제 기록을 조회하고 설명한다.
10. 남은 영양 목표를 바탕으로 간단한 식단 후보를 받을 수 있다.

## 14. 초기 제외 범위

- 다중 테넌트/조직 기능
- 소셜 피드
- 친구/팔로우
- 실시간 채팅
- 트레이너 회원 관리
- 결제
- 공개 운동 콘텐츠 플랫폼
- 대규모 음식 데이터베이스
- MSA 및 분산 인프라
- RAG
- 의료 진단 및 치료 목적 기능
