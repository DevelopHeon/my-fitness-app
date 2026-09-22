# My Fitness - Product & Technical Specification

> 상태: Draft v0.1  
> 목적: 1~2명이 사용하는 개인용 헬스 기록, 식단 관리, 개인화 AI 코치 애플리케이션

## 1. 프로젝트 목표

My Fitness는 운동 기록과 식단/신체 데이터를 한 곳에 축적하고, 통계와 AI Coach를 이용해 사용자가 자신의 변화를 쉽게 이해할 수 있도록 하는 개인용 피트니스 애플리케이션이다.

초기 목표는 범용 상용 서비스가 아니라 **본인이 매일 실제로 사용할 수 있는 기록 앱**을 완성하는 것이다. 사용자는 최대 1~2명을 가정하며, 대규모 트래픽이나 분산 시스템은 고려하지 않는다.

핵심 가치는 다음과 같다.

- 운동 기록 입력이 빠르고 반복 사용하기 쉬울 것
- 이전 기록과 현재 기록을 쉽게 비교할 수 있을 것
- 체중, 체지방, 골격근, 운동량의 변화를 한 화면에서 확인할 수 있을 것
- 식단의 칼로리 및 탄수화물/단백질/지방을 기록할 수 있을 것
- AI Coach가 사용자 기록을 바탕으로 질의 응답과 간단한 분석을 제공할 것
- 외부 AI Provider 사용 시 질문에 필요한 최소한의 집계 Context만 전달하고 식별 정보와 불필요한 기록은 전달하지 않을 것
- Spring AI Provider 추상화를 사용해 향후 Ollama 등 로컬 모델로 전환할 수 있을 것

## 2. 기술 방향

프로젝트는 하나의 저장소와 하나의 Spring Boot 배포 단위를 사용한다.

- Backend: Java + Spring Boot
- Persistence: Spring Data JPA + PostgreSQL + Flyway
- Frontend: Next.js + TypeScript
- UI: Tailwind CSS
- Client: PWA
- AI Provider: OpenAI 초기 운영, Ollama 등 로컬 Provider 전환 가능
- AI Integration: Spring AI ChatModel / ChatClient
- Chart: Recharts 또는 동급의 경량 차트 라이브러리
- Deployment: Docker 기반 단일 애플리케이션 배포
- Initial Infra: PostgreSQL + Spring Boot + external AI provider

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
- 동일 중량/횟수의 수행을 중량 + 횟수 + 세트 수 묶음 로우로 입력
- 세트 묶음 로우 추가/개별 삭제
- 저장 시 묶음의 세트 수만큼 WorkoutSet으로 확장
- 세트 일괄 추가/수정/삭제
- 세트별 중량, 횟수 기록을 기본으로 하고 운동 시간 데이터는 호환 유지
- 운동별 메모
- 운동 완료 처리
- 완료된 운동 기록 재오픈 / 수정 / 재완료
- 기록 중 이전 화면 이동 및 진행 중 기록 이어쓰기
- 운동 종목 카드별 접기/펼치기
- 완료 전 입력값 자동 임시 저장 및 명시적 임시 저장
- 화면 재진입 시 저장 전 묶음 입력 복원
- 저장하지 않은 입력이 있으면 완료 방지
- 동일 종목의 이전 기록 조회
- 이전 기록은 참고용으로 표시하고 새 입력값은 초기화
- 선택 날짜의 운동 기록 조회
- Workout 내부 월간 캘린더에서 운동 날짜와 수행 종목 요약 조회

주요 화면:
- 오늘/선택 날짜 운동
- 카테고리 탭 기반 운동 종목 선택 + 기본 닫힘 상태의 커스텀 운동 추가 토글
- 세트 묶음 입력
- 이전 기록
- 운동 완료 요약
- 월간 운동 캘린더

완료 기준:
- 사용자가 헬스장에서 스마트폰으로 실제 운동 1회를 처음부터 끝까지 기록할 수 있다.
- 동일 운동을 다시 수행할 때 직전 세트 기록을 확인할 수 있다.
### Phase 2. Routine

> 구현 상태: 완료

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
- Routine 화면뿐 아니라 Workout 시작 화면에서도 저장된 루틴을 선택해 별도의 운동 종목 검색 없이 바로 운동 기록을 시작할 수 있다.

### Phase 3. BodyRecord

> 구현 상태: 완료

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

> 구현 상태: 완료

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

구현 기준:
- 운동 횟수와 Volume 비교는 최근 7일 / 직전 7일, 최근 30일 / 직전 30일 rolling period를 사용한다.
- Dashboard 운동 통계에는 COMPLETED Workout만 포함한다.
- 최근 30일 일별 Volume을 차트로 제공하고 전체 또는 운동 카테고리별로 필터링한다.
- 그래프 데이터는 등록 순서가 아니라 실제 measuredAt / workoutDate 기준으로 오름차순 정렬한다.
- Body의 Instant 날짜는 프론트에서 로컬 날짜로 변환해 입력 날짜와 일치시킨다.
- Body는 최근 90일 기록의 최신값 / 이전 측정 대비 변화 / 추이를 제공한다.
- 종목 PR은 완료된 전체 Workout을 기준으로 최고 중량과 Epley 추정 1RM을 계산한다.
- 종목별 최근 8회 수행의 최고 중량 / 추정 1RM / Volume 추이를 제공한다.
- 이전 기간 Volume이 0이면 증감률은 null로 제공해 무한대 표현을 피한다.
- Dashboard는 앱의 기본 진입 화면으로 사용한다.

완료 기준:
- Workout과 BodyRecord 데이터만으로 핵심 변화와 종목 PR을 한 화면에서 확인할 수 있다.
- 통계 계산은 Spring Boot에서 수행되고 프론트는 결과를 시각화한다.

### Phase 5. Nutrition

> 구현 상태: 완료

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

구현 기준:
- Food는 1회 제공량과 단위(g/ml/개), 칼로리, 탄수화물, 단백질, 지방을 저장한다.
- Meal은 날짜와 아침/점심/저녁/간식 구분을 가지며 같은 구분의 여러 음식은 MealFood로 기록한다.
- MealFood에는 기록 시점의 음식 이름/제공량/영양값을 스냅샷으로 저장해 Food 수정/삭제가 과거 식단을 바꾸지 않게 한다.
- 섭취 회분(servings)을 곱해 실제 섭취 영양량을 계산한다.
- 최근 사용 음식과 누적 사용 빈도가 높은 음식을 각각 최대 5개 제공한다.
- 일별 응답은 4개 식사 구분과 각 합계, 하루 총합, 목표 대비 남은 양을 함께 제공한다.
- 사용자당 현재 NutritionGoal 하나를 유지한다.
- 미래 날짜 식단 입력은 허용하지 않는다.
- 메인 스키마는 Flyway V5로 생성하고 ddl-auto: none을 유지한다.

완료 기준:
- 자주 먹는 음식은 재입력 없이 선택하여 식단에 추가할 수 있다.
- 하루 섭취량과 목표량 차이를 즉시 확인할 수 있다.
- 음식 정보를 수정하거나 삭제해도 이미 기록된 과거 식단 값은 유지된다.

### Phase 6. AI Coach

목표: 사용자가 기록한 Workout / Body / Nutrition 데이터를 바탕으로 개인화된 질의응답과 기록 해석을 제공한다.

상세 구현 기준은 [2026-09-22-tue-pr-009-ai-coach.md](./2026-09-22-tue-pr-009-ai-coach.md)를 기준으로 한다.

핵심 기능:
- 모든 주요 화면의 우측 하단 AI Floating Button
- 모바일 Bottom Sheet / 데스크톱 Side Panel
- 사용자별 여러 Conversation 생성/조회
- Conversation별 User / Assistant Message 저장
- 운동/식단/신체 기록 기반 개인화
- 질문 유형별 최소 Context 선택
- 앱과 무관한 명백한 질문은 Provider 호출 전 차단
- 최근 대화 일부를 이용한 후속 질문
- token / model / provider / latency / 성공·실패 로그
- Quick Prompt
- 기록 기반 추천의 근거 제시

AI 처리 원칙:
- AI는 Read-only로 시작하며 Workout / Body / Nutrition 기록을 직접 수정하지 않는다.
- LLM은 DB와 Repository에 직접 접근하지 않는다.
- 각 기능 모듈이 공개하는 Spring Modulith insight Named Interface를 통해 Java가 Context를 구성한다.
- Volume, 평균, 증감률, 목표 대비 차이, 1RM 등 정확성이 필요한 계산은 Java에서 수행한다.
- 모든 사용자 데이터를 항상 Prompt에 넣지 않고 질문 유형에 필요한 Context만 선택한다.
- userId는 모델 입력으로 전달하거나 모델이 생성하지 않고 서버 요청 컨텍스트에서 결정한다.
- 의료 진단, 치료 판단, 질병 식단 처방, 위험한 운동 권장은 범위에서 제외한다.
- 데이터가 부족하면 추측하지 않고 기록이 부족하다고 답한다.

Provider:
- 초기 운영은 OpenAI gpt-4o-mini를 기본 설정으로 사용한다.
- Spring AI의 ChatModel / ChatClient 추상화를 사용한다.
- Provider와 model은 환경 설정으로 관리하며 Application 코드에 하드코딩하지 않는다.
- 이후 Ollama 등 로컬 Provider로 전환할 때 Conversation, Context Builder, REST API, Frontend는 변경하지 않는 것을 목표로 한다.
- 자동 Provider failover는 초기 범위에서 제외한다.

초기에는 Java Router + Context Builder + 단일 모델 호출 구조를 우선한다.
자유로운 Tool Calling, RAG, Vector DB, Embedding, Streaming SSE는 필요성이 확인된 이후 확장한다.

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
                   OpenAI / Ollama
```
Spring Boot는 기능 단위 application module을 기본으로 하고, 각 기능 내부는 동일한 4계층과 Port/Adapter 규칙을 사용한다.

```text
<module>
├── presentation
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
```

Controller와 다른 모듈은 Application Service 구현체가 아니라 `application.port.in`의 Use Case/Query를 호출한다. Application은 DB나 외부 시스템 구현체가 아니라 `application.port.out` 계약에 의존하고 Infrastructure Adapter가 이를 구현한다.

모듈 간 접근은 Spring Modulith Named Interface로 공개한 In Port만 사용하는 것을 기본으로 한다. Exercise의 작은 공통 식별 모델은 명시적인 shared vocabulary로만 예외적으로 공개한다. 상세 규칙은 `docs/architecture/README.md`를 기준으로 한다.

AI 패키지는 기존 4계층과 Spring Modulith 경계를 유지한다.

~~~text
ai
├── presentation     # Conversation / Message API
├── application      # Coach, Router, Context Builder, AiChatGateway Port
├── domain           # Conversation / Message / RequestLog
└── infrastructure   # Persistence + Spring AI Provider Adapter
~~~

AI는 다른 기능 모듈의 Repository나 Entity를 직접 조회하지 않는다.
Workout / Body / Nutrition 모듈이 제공하는 읽기 전용 insight Named Interface를 통해 필요한 집계 데이터를 받는다.

## 6. 핵심 도메인 모델

초기 엔티티 후보:

- User
- Exercise
- CustomExercise
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
- Exercise는 모든 사용자에게 제공되는 기본 운동 종목이며 userId를 가지지 않는다.
- CustomExercise는 사용자가 직접 추가한 운동 종목이며 userId로 소유권을 구분한다.
- WorkoutExercise와 RoutineExercise는 DEFAULT/CUSTOM + exerciseId로 원본을 식별하고, 기록 시점의 이름/카테고리를 스냅샷으로 보존한다.
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
- /api/ai/conversations
- /api/ai/conversations/{conversationId}/messages

응답은 화면 중심 DTO를 사용하며 JPA Entity를 API 응답으로 직접 노출하지 않는다.
## 8. 사용자 및 인증

초기 사용자는 최대 1~2명으로 가정한다.

- 사용자별 데이터는 반드시 분리한다.
- 인증은 이메일/비밀번호 기반의 단순한 Spring Security 구성을 우선한다.
- 외부 OAuth 로그인은 초기 범위에서 제외한다.
- API 요청에서 인증된 사용자 기준으로 데이터 범위를 제한한다.
- AI 요청에서도 userId는 모델 입력이나 Client Context에서 받지 않고 서버 인증 컨텍스트에서 결정한다.

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

## 10. AI Coach 상세 정책

AI 호출은 Browser에서 Provider로 직접 연결하지 않는다.

~~~text
Browser
  ↓
Spring Boot AI Coach API
  ↓
Java Router / Context Builder
  ↓
AiChatGateway
  ↓
Spring AI ChatClient / ChatModel
  ↓
OpenAI 또는 Ollama
~~~

초기 운영은 OpenAI를 사용하고, 로컬 전환 시 Ollama를 선택할 수 있게 Provider 설정을 외부화한다.

AI 응답 정책:
- 사용자의 Conversation과 Message를 DB에 저장한다.
- 여러 Conversation을 지원한다.
- 최근 대화 일부만 Prompt history에 사용한다.
- 기록이 없거나 조회 기간이 짧으면 그 한계를 명시한다.
- 사용자의 목표와 실제 섭취량을 구분한다.
- 식단 추천은 하나의 정답이 아니라 조건에 맞는 후보를 제안한다.
- 모델 응답에 계산된 수치를 넣을 때 서버에서 계산한 값을 우선한다.
- 명백한 비관련 질문은 Provider 호출 전에 차단한다.
- 모델 사용량과 latency는 AiRequestLog에 기록한다.
- prompt/completion 원문 observability logging은 운영에서 활성화하지 않는다.
- 초기 응답은 non-streaming으로 구현하고 필요 시 SSE를 추가한다.

Provider 선택 기준:
- Spring AI ChatModel 구현을 제공할 것
- 한국어 응답 품질이 충분할 것
- 응답 속도가 모바일 사용성을 크게 해치지 않을 것
- token usage metadata를 가능한 범위에서 제공할 것

초기 기본 모델은 gpt-4o-mini이며 모델명은 환경 변수로 관리한다.
로컬 Ollama 모델은 실제 개발 장비에서 비교 후 설정한다.

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
5. AI Router / Context / Conversation / Provider Gateway 테스트
6. Spring Modulith / ArchUnit 아키텍처 테스트
7. OpenAI 및 Ollama Provider smoke test

LLM의 자연어 문장 자체를 정확 문자열로 검증하지 않는다. 대신 Router 분류, 선택 Context, Provider Gateway 호출 여부, token usage 기록과 필수 응답 조건을 검증한다.

AI 없이도 Workout, Routine, BodyRecord, Dashboard, Nutrition의 핵심 기능은 정상 동작해야 한다.

세부 원칙은 `docs/testing/AUTOMATED_TEST_STRATEGY.md`를 기준으로 한다.
## 12. 배포 및 운영

초기 운영은 단순성을 우선한다.

- Spring Boot 애플리케이션 1개
- PostgreSQL 1개
- 초기 AI Provider는 외부 OpenAI API 사용
- 로컬 개발/향후 전환 Provider로 Ollama 지원
- 프론트엔드는 Spring Boot 배포물에 포함
- 환경 변수로 DB, AI Provider, model, API key / base URL 설정을 분리
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
