# My Fitness Architecture

이 문서는 프로젝트의 현재 구조와 주요 책임을 사람이 빠르게 이해할 수 있도록 유지한다.
기능 단위 구현이 끝날 때마다 구조에 영향을 주는 변경 사항을 반영한다.

## 1. 전체 구조

```text
┌──────────────────────────────┐
│       Next.js PWA            │
│ workout / routine / body     │
│ dashboard / nutrition / ai   │
└──────────────┬───────────────┘
               │ REST / SSE
               ▼
┌──────────────────────────────┐
│        Spring Boot           │
│                              │
│ user / workout / routine     │
│ body / nutrition / dashboard │
│ ai / common                  │
└───────────┬───────────┬──────┘
            │           │
            ▼           ▼
       PostgreSQL    Spring AI
                         │
                         ▼
                       Ollama
```

## 2. 현재 기술 기준

- Java: 21 타깃
- Spring Boot: 4.1.1
- Database: PostgreSQL 17
- Frontend: Next.js + TypeScript + PWA
- AI: Spring AI 2.0.x + Ollama (Phase 6에서 연결)

## 3. 배포 단위

- Git 저장소는 하나만 사용한다.
- Next.js는 정적 export가 가능한 클라이언트 애플리케이션으로 운영한다.
- production 빌드 시 Next.js 산출물을 Spring Boot static 리소스에 포함한다.
- 최종 서비스 진입점은 Spring Boot 하나를 우선한다.
- PostgreSQL과 Ollama는 별도 프로세스/컨테이너로 운영한다.

## 4. 백엔드 책임

Spring Boot가 다음을 담당한다.

- 인증된 사용자 식별 및 데이터 접근 범위 통제
- Workout/Routine/Body/Nutrition 비즈니스 규칙
- Dashboard 통계 계산
- PostgreSQL 영속화
- AI Tool Calling을 위한 정확한 데이터 조회와 계산
- 정적 프론트엔드 제공

기능 패키지는 도메인 기준으로 나누고, 서비스 분리는 하지 않는다.

## 5. 프론트엔드 책임

화면은 모바일 우선의 깔끔한 테마를 유지한다. 별도 디자인 시스템 구축보다 기록 속도, 가독성, 일관된 간격과 입력 경험을 우선하며 과도한 시각 효과는 피한다.

Next.js PWA가 다음을 담당한다.

- 모바일 우선 UI
- 운동 중 빠른 세트 입력
- Dashboard 시각화
- API 호출 및 사용자 피드백
- PWA 설치와 기본 캐싱
- AI 채팅 화면

Next.js 서버 기능에 의존하지 않고 Spring REST API를 기준으로 한다.

## 6. 데이터 흐름

일반 기능:

```text
PWA → Spring REST API → Application Service → JPA → PostgreSQL
```

AI 기능:

```text
PWA → /api/ai/chat → Spring AI → Ollama
                         ↓
                    Tool Calling
                         ↓
              Domain/Query Service
                         ↓
                    PostgreSQL
```

수치 계산은 Java에서 수행하고 LLM은 해석과 자연어 응답에 집중한다.

## 7. 단계별 확장

1. Workout: 운동/세트 기록과 이전 기록 조회
2. Routine: 반복 운동 템플릿
3. BodyRecord: 체중/체지방/골격근
4. Dashboard: 운동량과 신체 변화 시각화
5. Nutrition: 음식/식단/탄단지
6. Local AI Coach: Ollama 기반 질의/분석/식단 후보 제안
7. 이후 확장: 사진, 알림, AI Insight, RAG

## 8. 변경 원칙

- 구조가 바뀌면 코드보다 먼저 또는 같은 PR에서 이 문서를 갱신한다.
- 클래스/메서드 수준 설명은 넣지 않는다.
- 새 인프라를 도입할 때는 왜 필요한지와 어떤 문제를 해결하는지만 기록한다.
- 현재 사용자 규모(1~2명)에 불필요한 분산 구성은 도입하지 않는다.
