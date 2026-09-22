# Phase 6 - AI Coach 구현 계획

> 상태: 구현 전 설계 확정
> 작성일: 2026-09-22
> 대상 Phase: Phase 6
> 기본 운영 Provider: OpenAI / gpt-4o-mini
> 전환 Provider: Ollama 등 Spring AI 지원 ChatModel Provider
> 핵심 원칙: 사용자 기록 기반, Read-only, Provider 교체 가능, 최소 Context, 비용 추적

## 1. 목표

Phase 6의 목표는 단순한 범용 챗봇을 추가하는 것이 아니라,
사용자가 My Fitness에 직접 기록한 Workout / Body / Nutrition 데이터를 근거로
질문에 답하고 기록을 해석하는 개인 AI Coach를 제공하는 것이다.

AI Coach는 다음 경험을 제공해야 한다.

- 모든 주요 화면에서 바로 AI Coach를 열 수 있다.
- 사용자는 여러 개의 Conversation을 만들고 과거 대화를 다시 열 수 있다.
- 최근 운동, 신체 변화, 식단과 영양 목표를 바탕으로 개인화된 답변을 받을 수 있다.
- 앱과 무관한 질문은 가능한 한 모델 호출 전에 차단한다.
- AI가 사용하는 수치와 통계는 Java에서 계산한다.
- AI는 기존 기록을 수정하지 않는 Read-only 기능으로 시작한다.
- 운영 Provider는 OpenAI를 사용하되 Spring AI 추상화로 Ollama 등 로컬 모델로 쉽게 전환할 수 있어야 한다.
- 질문, 모델 사용량, 응답 시간, 성공/실패를 기록해 향후 품질과 비용을 분석할 수 있어야 한다.

## 2. Phase 6 범위

### 포함

1. 전역 AI Floating Action Button
2. 모바일 Bottom Sheet / 데스크톱 Side Panel 형태의 AI UI
3. 사용자별 여러 Conversation 생성 및 조회
4. Conversation별 User / Assistant Message 저장
5. Fitness 범위 질문 분류 및 명백한 비관련 질문 차단
6. 질문 유형별 최소 Context 선택
7. Workout / Body / Nutrition 기록 기반 개인화 Context
8. 최근 Conversation Message 일부를 활용한 후속 질문
9. Spring AI ChatModel / ChatClient 기반 Provider 추상화
10. OpenAI Provider 초기 연동
11. Ollama Provider로 설정 전환 가능한 구조
12. 모델 사용량, latency, 성공/실패 로그
13. Quick Prompt
14. 실제 기록을 근거로 한 답변
15. 의료 진단 및 위험한 운동/식단 조언 제한

### 제외

Phase 6 초기에는 다음을 구현하지 않는다.

- RAG
- Vector DB
- Embedding
- 인터넷 검색
- 자유로운 LLM Tool Calling
- AI에 의한 Workout / Routine / Meal 자동 저장
- 이미지 식단 분석
- 장기 대화 전체를 매 요청마다 전달
- AI가 userId를 결정하거나 생성하는 구조
- Provider 자동 failover
- AI가 임의 SQL 또는 Repository를 직접 호출하는 구조

Tool Calling은 사용자가 자연어로 과거 기록을 자유롭게 탐색해야 하는 요구가 명확해진 이후 확장한다.

## 3. 사용자 경험

### 3.1 전역 AI 버튼

AI Coach 진입 버튼은 특정 AI 페이지에만 두지 않고 App Shell에 둔다.

~~~text
Dashboard ─┐
Workout   ─┤
Routine   ─┤
Body      ─┤── 우측 하단 AI Floating Button
Nutrition ─┘
~~~

따라서 화면 전환과 상관없이 같은 위치에서 AI Coach를 열 수 있다.

기본 위치:

- 우측 하단 fixed
- 모바일 safe-area 고려
- 주요 입력 버튼과 겹치지 않도록 하단 여백 확보
- AI Panel이 열리면 FAB는 닫기 동작 또는 숨김 처리

### 3.2 반응형 AI Panel

모바일:

~~~text
┌─────────────────────────────┐
│ AI Coach             새 대화 │
│─────────────────────────────│
│ Conversation messages       │
│                             │
│─────────────────────────────│
│ Quick prompts               │
│ [질문 입력...]        [전송] │
└─────────────────────────────┘
~~~

Bottom Sheet 또는 전체 화면에 가까운 Sheet로 표시한다.

권장 높이:

- 약 80~90dvh
- 키보드 노출 시 입력창이 가려지지 않아야 한다.

데스크톱:

- 우측 Side Drawer
- 약 420~480px 폭
- 기존 화면을 완전히 떠나지 않고 질의 가능

### 3.3 Conversation UX

상단에 다음 기능을 둔다.

- 현재 Conversation 제목
- Conversation 목록 열기
- 새 대화
- 선택 Conversation 삭제
- 필요 시 제목 수정

새 Conversation의 초기 제목은 별도 AI 호출로 생성하지 않는다.
첫 사용자 질문을 서버에서 잘라 제목으로 사용한다.

~~~text
"최근 한 달 벤치프레스 얼마나 늘었어?"
→ "최근 한 달 벤치프레스 얼마나 늘었어?"
~~~

긴 질문은 최대 길이까지만 사용한다.

### 3.4 Quick Prompt

빈 Conversation 또는 입력창 상단에 빠른 질문을 제공한다.

기본 후보:

- 오늘 뭐 운동할까?
- 최근 운동량 분석해줘
- 다음 중량 어떻게 잡을까?
- 오늘 식단 평가해줘
- 남은 영양 목표에 맞는 식단 추천해줘
- 최근 체중과 운동 퍼포먼스 변화를 같이 봐줘

가능하면 현재 화면에 따라 Quick Prompt 우선순위를 바꾼다.

## 4. 현재 화면 Context

AI 버튼은 모든 화면에 존재하므로 현재 화면 정보를 질문과 함께 전달할 수 있다.

단, 프론트에서 실제 건강/운동 데이터를 AI Context로 직접 구성하지 않는다.
프론트는 서버가 데이터를 다시 조회하기 위한 hint만 전달한다.

~~~json
{
  "screen": "NUTRITION",
  "selectedDate": "2026-09-22"
}
~~~

또는:

~~~json
{
  "screen": "WORKOUT",
  "selectedDate": "2026-09-22",
  "resourceId": 123
}
~~~

원칙:

- userId는 Client Context에서 받지 않는다.
- resourceId / selectedDate는 서버에서 소유권과 범위를 재검증한다.
- Client Context가 없어도 질문은 가능해야 한다.

## 5. Conversation / Message 데이터 모델

### 5.1 AiConversation

사용자별 여러 대화를 지원한다.

필드 후보:

~~~text
id
userId
title
createdAt
updatedAt
~~~

규칙:

- Conversation은 반드시 userId 소유다.
- 다른 사용자의 Conversation을 조회/수정/삭제할 수 없다.
- Message가 추가될 때 updatedAt을 갱신한다.
- 목록은 updatedAt 내림차순으로 제공한다.

### 5.2 AiMessage

Conversation 안의 실제 대화 내용이다.

~~~text
id
conversationId
role
queryType
content
createdAt
~~~

role:

~~~text
USER
ASSISTANT
~~~

queryType:

~~~text
WORKOUT
NUTRITION
BODY
GENERAL_FITNESS
COMPOSITE
OUT_OF_SCOPE
~~~

queryType은 USER Message를 중심으로 사용한다.

### 5.3 AiRequestLog

모델 호출 및 비용 분석용 로그다.

~~~text
id
userId
conversationId
userMessageId
assistantMessageId

queryType
provider
model
promptVersion

inputTokens
outputTokens
totalTokens

latencyMs
status
errorCode

contextTypes
createdAt
~~~

status 후보:

~~~text
SUCCESS
FAILED
REJECTED_OUT_OF_SCOPE
~~~

contextTypes 예:

~~~text
["WORKOUT_SUMMARY", "EXERCISE_HISTORY"]
["BODY_TREND", "WORKOUT_SUMMARY"]
["NUTRITION_TODAY", "NUTRITION_GOAL"]
~~~

중요:

- 실제 System Prompt 전체를 request log에 저장하지 않는다.
- Body / Workout / Nutrition Context 전체 snapshot도 로그에 중복 저장하지 않는다.
- promptVersion과 어떤 Context를 사용했는지만 기록한다.
- 사용자 질문과 Assistant 답변은 AiMessage가 원본이다.
- Provider가 사용량을 제공하면 Spring AI ChatResponse metadata에서 token usage를 기록한다.
- Provider가 일부 usage를 제공하지 않으면 nullable로 허용한다.

## 6. Flyway

Phase 6에서는 V6 migration을 추가한다.

~~~text
V6__create_ai_coach_tables.sql
~~~

생성 대상:

~~~text
ai_conversations
ai_messages
ai_request_logs
~~~

인덱스 후보:

~~~text
ai_conversations(user_id, updated_at desc)
ai_messages(conversation_id, created_at, id)
ai_request_logs(user_id, created_at desc)
ai_request_logs(query_type, created_at desc)
~~~

메인 runtime의 다음 정책은 유지한다.

~~~text
spring.jpa.hibernate.ddl-auto: none
~~~

AI schema도 Flyway로만 관리한다.

## 7. 질문 범위

AI Coach는 Fitness 앱과 관련된 질문만 처리한다.

허용 범위:

- 운동
- 웨이트 트레이닝
- Workout 기록 해석
- 운동 루틴 일반 질문
- 세트 / 반복 / 중량
- 운동 빈도 / Volume / 1RM
- 체중 / 체지방 / 골격근 변화 해석
- 일반적인 식단 관리
- 칼로리 / 탄수화물 / 단백질 / 지방
- NutritionGoal 대비 섭취량
- 기록된 음식을 고려한 식사 후보
- 운동과 영양을 함께 보는 일반 Fitness 질문

명백한 비허용 예:

~~~text
"자바 트랜잭션 설명해줘"
"비트코인 전망 알려줘"
"오늘 날씨 어때?"
~~~

이 경우 Provider를 호출하지 않고 서버에서 바로 범위 안내 응답을 반환한다.

## 8. Intent / Scope Router

토큰을 아끼기 위해 모든 질문을 먼저 LLM으로 분류하지 않는다.
초기 구현은 Java 기반 Router를 사용한다.

~~~text
User Question
      ↓
AiQueryRouter
      ↓
┌────────────────────────┐
│ WORKOUT                │
│ NUTRITION              │
│ BODY                   │
│ GENERAL_FITNESS        │
│ COMPOSITE              │
│ OUT_OF_SCOPE           │
└────────────────────────┘
~~~

판단 요소:

1. 질문 키워드
2. 현재 Client Context screen
3. 직전 Conversation의 queryType
4. 등록된 Exercise 이름과의 매칭
5. 명백한 비관련 키워드

정책:

- 명백한 OUT_OF_SCOPE는 모델 호출 없이 차단
- 명백한 Fitness 질문은 바로 허용
- 애매하지만 Fitness 맥락이 존재하면 최소 Context로 허용
- Router가 확신하지 못한다는 이유만으로 과도하게 차단하지 않는다.

향후 정확도가 부족하면 작은 모델 기반 structured classification을 별도 검토한다.

## 9. Context Selection

모든 질문에 모든 사용자 데이터를 넣지 않는다.
질문 유형별로 필요한 Context만 선택한다.

### WORKOUT

- 최근 7일 / 30일 운동 횟수
- 최근 7일 / 직전 7일 Volume
- 최근 30일 / 직전 30일 Volume
- 카테고리별 Volume
- 최근 Workout 요약
- 질문에 포함된 특정 Exercise의 최근 기록
- 최고 중량
- 추정 1RM
- 최근 수행 추이

### BODY

- 최신 체중
- 최신 체지방률
- 최신 골격근량
- 이전 기록 대비 변화
- 최근 30일 또는 90일 변화

### NUTRITION

- 선택 날짜 또는 오늘 섭취량
- NutritionGoal
- 남은 칼로리
- 남은 탄수화물
- 남은 단백질
- 남은 지방
- 최근 먹은 음식
- 자주 먹는 음식

### COMPOSITE

질문에 필요한 두 개 이상의 Context만 조합한다.

~~~text
"체중은 줄었는데 운동 퍼포먼스는 어때?"

BODY_TREND
+
WORKOUT_SUMMARY
~~~

## 10. AI가 사용하는 공개 Query API

AI가 다른 모듈의 Entity나 Repository를 직접 조회하지 않는다.

기존 Spring Modulith 경계를 유지하기 위해 각 기능 모듈에 읽기 전용 공개 Query Interface를 추가한다.

~~~text
workout
└── application
    └── port
        └── in
            └── insight
                └── WorkoutInsightQuery

body
└── application
    └── port
        └── in
            └── insight
                └── BodyInsightQuery

nutrition
└── application
    └── port
        └── in
            └── insight
                └── NutritionInsightQuery
~~~

각 insight package는 Named Interface로 공개한다.

~~~java
@NamedInterface("insight")
package com.myfitness.workout.application.port.in.insight;
~~~

Phase 6 구현 시 ai module의 allowedDependencies는 최소한으로 연다.

~~~text
ai
├── workout::insight
├── body::insight
└── nutrition::insight
~~~

AI가 다음에 직접 의존하지 않는다.

~~~text
WorkoutRepositoryPort
BodyRecordRepositoryPort
Nutrition 관련 RepositoryPort
다른 모듈 application.port.out
다른 모듈 infrastructure
~~~

즉 AI는 다른 모듈의 Out Port나 Persistence 구현을 우회하지 않고 공개된 Insight In Port만 사용한다.

## 11. 정확한 계산 책임

다음 계산은 반드시 Java에서 수행한다.

- Volume
- 기간별 Workout 횟수
- 이전 기간 대비 증감률
- Epley 추정 1RM
- 최고 중량
- 체중 / 체지방 / 골격근 변화
- 칼로리 합계
- 탄수화물 / 단백질 / 지방 합계
- NutritionGoal 대비 remaining
- 최근 / 자주 먹는 음식 집계

LLM에게 원본 숫자를 주고 계산을 부탁하지 않는다.

LLM은 서버가 계산한 결과를 설명, 비교, 요약하고 선택지를 제안하는 역할만 담당한다.

## 12. Prompt 구성

~~~text
System Policy
+
Prompt Version
+
선택된 User Context
+
최근 Conversation 일부
+
현재 User Question
~~~

System Prompt에는 최소한 다음 규칙을 넣는다.

- My Fitness의 개인 AI Coach 역할
- 운동/식단/신체 기록 범위만 응답
- 서버가 제공한 기록을 우선 근거로 사용
- 없는 기록을 추측하지 않음
- 서버가 계산한 수치를 다시 임의 계산하지 않음
- 데이터가 부족하면 부족하다고 명시
- 의료 진단/치료를 하지 않음
- 심한 통증, 실신, 부상 등은 전문 의료진 확인 권고
- 극단적인 저칼로리 식단이나 위험한 운동을 권하지 않음
- userId 또는 내부 ID를 사용자에게 노출하지 않음
- 가능한 경우 답변에 사용한 데이터 근거를 짧게 설명
- 답변은 한국어를 기본으로 함

Prompt Version 예:

~~~text
fitness-coach-v1
~~~

Prompt 변경 시 version을 올려 AiRequestLog와 연계한다.

## 13. 대화 History 정책

Conversation 전체를 매 요청마다 Provider에 전달하지 않는다.

초기 정책:

- 최근 User/Assistant Message N개만 사용
- Message 개수와 전체 문자 수를 모두 제한
- 현재 질문과 관련성이 낮은 오래된 Message는 제외
- DB에는 Conversation 전체를 유지

설정 예:

~~~text
AI_HISTORY_MESSAGE_LIMIT=8
AI_HISTORY_CHAR_LIMIT=4000
~~~

정확한 기본값은 구현 후 실제 token usage를 보고 조정한다.

장기 Conversation 품질이 필요해지면 향후 Conversation Summary를 도입한다.
Phase 6 v1에는 자동 장기 요약을 넣지 않는다.

## 14. Token 사용량 절감

Provider 호출 전:

- OUT_OF_SCOPE 차단
- 사용자 질문 최대 길이 제한
- 최근 History만 선택
- 질문 유형별 Context 선택
- 원본 Entity 전체 serialization 금지
- 서버 계산 결과만 compact하게 전달

Provider 요청:

- System Prompt를 불필요하게 길게 만들지 않는다.
- temperature를 낮게 유지한다.
- 최대 출력 token을 제한한다.
- 답변 길이를 짧고 근거 중심으로 유도한다.

Provider 응답 후:

- Spring AI ChatResponse usage metadata에서 input/output/total token을 기록한다.
- Spring AI observability의 prompt/completion 원문 로그는 운영에서 켜지 않는다.

## 15. Provider 추상화

### 15.1 구조

AI Application 계층은 OpenAI SDK, Ollama API, Spring AI 타입을 직접 사용하지 않는다.

~~~text
ai.application
      │
      ▼
AiChatGateway
      ▲
      │ implements
ai.infrastructure.springai
      │
      ▼
Spring AI ChatClient / ChatModel
      │
      ├── OpenAI
      └── Ollama
~~~

예정 Port:

~~~java
public interface AiChatGateway {
    AiModelResponse chat(AiModelRequest request);
}
~~~

Application 모델은 Spring AI 타입을 포함하지 않는다.

AiModelResponse에는 최소 다음을 포함한다.

~~~text
content
provider
model
inputTokens
outputTokens
totalTokens
~~~

### 15.2 Spring AI Provider

Spring AI의 공통 ChatModel / ChatClient API를 사용한다.

운영 초기:

~~~text
spring.ai.model.chat=openai
~~~

로컬 전환:

~~~text
spring.ai.model.chat=ollama
~~~

Provider starter와 설정을 변경해도 다음 코드는 변경하지 않는 것을 목표로 한다.

- AiCoachService
- AiQueryRouter
- AiContextBuilder
- Conversation 저장
- REST API
- Frontend

초기 구현 시 Spring AI stable 2.0.x를 BOM으로 관리한다.
작성 시점 기준 stable reference는 2.0.1이며 exact patch는 구현 시점에 다시 확인한다.

Provider starter 후보:

~~~text
org.springframework.ai:spring-ai-starter-model-openai
org.springframework.ai:spring-ai-starter-model-ollama
~~~

두 Provider를 classpath에 둘 경우 활성 Chat Provider는 다음 설정으로 선택한다.

~~~yaml
spring:
  ai:
    model:
      chat: ${AI_PROVIDER:openai}
~~~

OpenAI와 Ollama 모두 동일한 Spring AI ChatModel contract 뒤에 위치시키고,
Provider 전환을 위해 Application Service 분기 코드를 작성하지 않는다.

### 15.3 설정

모델명은 코드에 하드코딩하지 않는다.

~~~text
AI_PROVIDER=openai
OPENAI_API_KEY=...
AI_OPENAI_MODEL=gpt-4o-mini

OLLAMA_BASE_URL=http://localhost:11434
AI_OLLAMA_MODEL=...

AI_TEMPERATURE=0.2
AI_MAX_OUTPUT_TOKENS=...
AI_REQUEST_TIMEOUT=...
~~~

초기 운영은 OpenAI gpt-4o-mini를 기본 설정으로 사용한다.
로컬 모델 전환 시 모델명은 실제 Ollama 환경에서 별도로 선정한다.

자동 Provider failover는 Phase 6에 포함하지 않는다.

## 16. 외부 Provider와 개인정보 최소화

초기 운영 Provider가 OpenAI인 경우 질문에 필요한 Fitness Context는 외부 AI Provider로 전달된다.

따라서:

- userId를 Prompt에 넣지 않는다.
- 이메일, 인증 정보 등 식별 정보는 Prompt에 넣지 않는다.
- 질문에 필요하지 않은 데이터를 넣지 않는다.
- 전체 DB record를 그대로 serialization하지 않는다.
- Context는 집계/요약 중심으로 생성한다.
- raw prompt / raw completion을 observability 로그에 저장하지 않는다.
- Request Log에는 token / provider / context type 등 운영 메타데이터만 추가 저장한다.

향후 Ollama로 전환하면 같은 Application 구조를 유지하면서 외부 전송을 제거할 수 있어야 한다.

## 17. 안전 정책

AI Coach는 의료 서비스가 아니다.

다음은 하지 않는다.

- 질병 진단
- 치료 판단
- 약물 처방
- 질병에 대한 식단 처방
- 부상 상태를 확정적으로 판단
- 극단적인 칼로리 제한 권장
- 위험한 운동 지속 권장

사용자가 통증, 실신, 심각한 부상 등 위험 신호를 언급한 경우
운동 추천보다 전문 의료진 상담을 우선 안내한다.

## 18. 추천 응답 원칙

개인 기록 기반 추천에는 가능한 한 사용한 근거를 포함한다.

~~~text
추천/요약

근거
- 최근 7일 운동 3회
- 등 운동 마지막 수행 5일 전
- 최근 7일 등 Volume이 직전 기간 대비 감소

선택지
- A
- B
~~~

모든 답변에 강제로 같은 형식을 사용하지는 않는다.

## 19. API 계획

### Conversation

~~~text
GET    /api/ai/conversations
POST   /api/ai/conversations
PATCH  /api/ai/conversations/{conversationId}
DELETE /api/ai/conversations/{conversationId}
~~~

POST는 새 Conversation을 생성한다.
PATCH는 제목 변경을 지원한다.

### Message

~~~text
GET  /api/ai/conversations/{conversationId}/messages
POST /api/ai/conversations/{conversationId}/messages
~~~

POST request 예:

~~~json
{
  "message": "오늘 남은 단백질 기준으로 저녁 추천해줘",
  "clientContext": {
    "screen": "NUTRITION",
    "selectedDate": "2026-09-22"
  }
}
~~~

처리 순서:

~~~text
Ownership 확인
   ↓
User Message 저장
   ↓
Scope / Intent 분류
   ↓
OUT_OF_SCOPE?
 ├─ YES → Provider 호출 없이 안내 답변 저장
 └─ NO
      ↓
Context 선택/조회
      ↓
최근 History 선택
      ↓
Prompt 생성
      ↓
Spring AI Provider 호출
      ↓
Assistant Message 저장
      ↓
AiRequestLog 저장
      ↓
Response
~~~

모든 API는 서버 요청의 사용자 기준으로 소유권을 검사한다.

## 20. Streaming

Phase 6 초기 구현은 non-streaming 응답을 기본으로 한다.

이유:

- 단순 질의의 transaction 흐름이 단순하다.
- token usage와 실패 처리가 단순하다.
- Provider 전환 검증이 쉽다.

실사용에서 대기 시간이 문제라면 다음 단계에서 SSE를 추가한다.
Spring AI ChatClient는 synchronous/streaming 모델을 모두 지원하므로 이후 확장 가능성을 유지한다.

## 21. 실패 처리

Provider 오류가 Conversation 자체를 깨뜨리면 안 된다.

~~~text
User Message 저장 성공
Provider timeout
↓
AiRequestLog = FAILED
Assistant Message는 저장하지 않거나 실패 안내 Message로 구분
~~~

정책:

- Provider timeout 설정
- 무제한 retry 금지
- 초기 retry는 0~1회 수준
- 같은 Message의 중복 Assistant 저장 방지
- API key / provider 내부 오류를 사용자에게 그대로 노출하지 않음

## 22. 분석 가능성

AiRequestLog와 AiMessage를 통해 추후 다음을 분석할 수 있어야 한다.

- 사용자당 AI 사용 횟수
- Conversation 수
- WORKOUT / NUTRITION / BODY 질문 비율
- OUT_OF_SCOPE 차단 비율
- 평균 input/output/total token
- 평균 latency
- Provider 오류율
- 모델별 사용량
- Prompt version별 사용량
- Context 유형별 사용 빈도

초기에는 별도 관리자 Dashboard를 만들지 않는다.

## 23. 추가 권장 기능

### 23.1 답변 Feedback

Phase 6 core 안정화 후 Assistant Message에 다음 feedback을 추가하는 것을 권장한다.

~~~text
HELPFUL
NOT_HELPFUL
~~~

첫 구현의 필수 완료 조건에는 포함하지 않는다.

### 23.2 Screen-aware Quick Prompt

구현 비용이 작으므로 v1에 포함하는 것을 우선 검토한다.

### 23.3 Cost Guard

AiRequestLog token 데이터를 기반으로 향후 사용자별 일/월 token budget을 걸 수 있는 구조를 유지한다.
Phase 6에서는 hard quota를 넣지 않고 측정부터 시작한다.

## 24. 초기 버전에서 덜어내는 기능

### LLM Tool Calling

현재 필요한 정보는 질문 유형에 따라 서버가 미리 결정할 수 있다.

~~~text
Java Router
→ Java Context Builder
→ LLM 1회
~~~

를 우선한다.

Tool Calling은 하나의 질문에 여러 모델 왕복이 생길 수 있으므로 비용과 디버깅 복잡도가 증가한다.

### 전체 Conversation 전달

오래된 대화를 모두 전송하지 않는다.
DB 저장과 LLM Context는 별개로 본다.

### 자동 DB 쓰기

AI 추천 결과를 자동으로 Workout / Meal / Routine에 저장하지 않는다.

향후 적용 기능이 필요하면 사용자 확인 버튼을 통해 일반 Application Use Case를 호출한다.

## 25. Backend 패키지 계획

~~~text
ai
├── presentation
│   ├── controller
│   │   └── AiCoachController
│   └── dto
│       ├── request
│       └── response
│
├── application
│   ├── port
│   │   ├── in
│   │   │   └── AiCoachUseCase
│   │   └── out
│   │       ├── AiChatGateway
│   │       ├── AiConversationRepositoryPort
│   │       ├── AiMessageRepositoryPort
│   │       └── AiRequestLogRepositoryPort
│   ├── service
│   │   └── AiCoachService
│   ├── router
│   │   └── AiQueryRouter
│   ├── context
│   │   └── AiContextBuilder
│   ├── command
│   ├── result
│   └── exception
│
├── domain
│   ├── model
│   │   ├── AiConversation
│   │   ├── AiMessage
│   │   ├── AiMessageRole
│   │   └── AiQueryType
│   └── exception
│
└── infrastructure
    ├── persistence
    │   └── *RepositoryAdapter
    └── springai
        └── SpringAiChatGateway
~~~

Provider별 SDK를 Application에 직접 노출하지 않는다.

## 26. Spring Modulith 변경 계획

현재 ai module은 외부 모듈 의존이 없다.

Phase 6에서는 각 기능 모듈에 일반적인 read/insight API를 Named Interface로 공개한다.

~~~text
workout::insight
body::insight
nutrition::insight
~~~

ai package-info 예정:

~~~java
@ApplicationModule(
    displayName = "AI Coach",
    allowedDependencies = {
        "workout::insight",
        "body::insight",
        "nutrition::insight"
    }
)
package com.myfitness.ai;
~~~

구현 후 다음을 반드시 통과한다.

~~~text
ApplicationModules.verify()
ModulithArchitectureTest
LayerArchitectureTest
PackageDocumentationTest
~~~

## 27. 테스트 계획

### Domain

- Conversation 생성
- 사용자 ID 검증
- Message role 규칙
- Conversation title 규칙
- Conversation updatedAt 갱신

### Application

- Conversation 소유권
- 새 Conversation 생성
- 여러 Conversation 분리
- Message history 순서
- 최근 History limit
- 질문 길이 제한
- WORKOUT / NUTRITION / BODY / COMPOSITE / OUT_OF_SCOPE 분류
- OUT_OF_SCOPE일 때 AiChatGateway가 호출되지 않음
- 질문 유형별 필요한 Context만 조회
- Provider 성공 시 User/Assistant Message와 RequestLog 저장
- Provider 실패 시 FAILED log 기록
- 다른 사용자의 Conversation 접근 차단
- userId가 Prompt input에서 만들어지지 않음

### Repository Integration

- Conversation / Message 저장
- 사용자별 Conversation 목록 정렬
- Conversation별 Message 시간 순 조회
- RequestLog token / latency 저장
- cascade/delete 정책

### API Integration

- Conversation CRUD
- Message 전송
- History 조회
- 사용자 소유권 격리
- OUT_OF_SCOPE 응답
- Provider mock 기반 AI 응답
- Provider 장애 응답

### Provider Adapter

자동 테스트에서는 실제 OpenAI/Ollama 네트워크를 호출하지 않는다.

별도 수동 smoke:

~~~text
OpenAI profile
→ gpt-4o-mini 1회 질문

Ollama profile
→ 설치된 local model 1회 질문
~~~

두 profile에서 Application/API 코드를 변경하지 않고 동작하는지 확인한다.

테스트 메서드는 영문 camelCase,
모든 테스트에는 한국어 @DisplayName을 유지한다.

## 28. 구현 순서

### Step 1. Query / Context 계약

- WorkoutInsightQuery
- BodyInsightQuery
- NutritionInsightQuery
- 관련 Result
- Named Interface
- Spring Modulith dependency test

### Step 2. AI Domain / Persistence

- AiConversation
- AiMessage
- AiRequestLog
- Application Repository Out Port
- Persistence Adapter
- Flyway V6
- Repository 테스트

### Step 3. Router / Context Builder

- AiQueryRouter
- AiContextSelector
- AiContextBuilder
- History limit
- Out-of-scope 처리
- Application 단위 테스트

### Step 4. Spring AI Provider Adapter

- Spring AI BOM/starter
- OpenAI ChatModel
- AiChatGateway
- token usage extraction
- timeout/error handling
- provider/model config

### Step 5. Conversation API

- Conversation CRUD
- Message API
- ownership
- provider failure handling
- API 통합 테스트

### Step 6. Frontend

- App Shell AI FAB
- Bottom Sheet / Side Drawer
- Conversation 목록
- 새 대화
- Message UI
- loading/error
- Quick Prompt
- Client Context

### Step 7. Provider 전환 검증

- OpenAI smoke
- Ollama smoke
- Application/API 코드 변경 없이 provider 설정만 변경되는지 확인

### Step 8. 전체 검증

- 전체 backend test
- Spring Modulith / ArchUnit
- frontend lint
- frontend build
- bootJar
- git diff --check
- ddl-auto: none
- Flyway migration 상태

## 29. 완료 기준

Phase 6는 다음을 모두 만족해야 완료로 본다.

- 어느 주요 화면에서든 AI 버튼을 열 수 있다.
- 모바일/데스크톱에서 기존 화면을 유지한 채 질문할 수 있다.
- 사용자별 여러 Conversation을 생성하고 다시 열 수 있다.
- 사용자별 Conversation/Message 소유권이 분리된다.
- 최근 Conversation 맥락으로 후속 질문을 할 수 있다.
- Workout / Body / Nutrition 기록 기반 개인화 답변을 제공한다.
- 일반 Fitness 질문도 처리할 수 있다.
- 명백한 앱 범위 밖 질문은 Provider 호출 없이 차단한다.
- 정확한 수치 계산은 Java에서 수행한다.
- AI가 DB에 직접 접근하거나 기존 기록을 수정하지 않는다.
- OpenAI Provider로 실제 질문이 동작한다.
- 설정 변경만으로 Ollama Provider smoke test가 가능하다.
- token usage / model / provider / latency / status를 기록한다.
- raw prompt 및 사용자 데이터 observability logging을 하지 않는다.
- 의료 진단이나 위험한 식단/운동을 확정적으로 권하지 않는다.
- Spring Modulith / ArchUnit 규칙이 모두 통과한다.
- Flyway V6로만 schema를 추가하고 ddl-auto: none을 유지한다.

## 30. Phase 6 이후 후보

- Assistant 답변 Helpful / Not Helpful
- Conversation 장기 Summary
- 사용자별 token budget
- 주간/월간 자동 AI Insight
- Tool Calling 기반 자유로운 기록 탐색
- 추천 Routine 적용 버튼
- 추천 식단 기록 적용 버튼
- RAG / 운동·영양 문서 검색
- 이미지 기반 식단 인식
- Streaming SSE

## 31. 구현 결과 및 후속 과제

현재 상태: 설계 확정, 구현 전.

구현 완료 후 이 섹션에 다음 내용을 갱신한다.

- 실제 적용 Spring AI version / starter
- OpenAI 기본 model
- Ollama smoke test model
- Flyway V6 결과
- 구현된 API 목록
- Context limit 실제 기본값
- token usage 측정 결과
- Provider별 smoke test 결과
- 전체 자동 테스트 수
- 후속 개선 과제
