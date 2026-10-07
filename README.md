# My Fitness

운동·신체·식단 기록과 기록 기반 AI Coach를 제공하는 개인용 피트니스 앱이다.
Spring Boot의 Modular Monolith와 Next.js PWA를 사용한다. 정적 프론트 자산은 같은 JAR에 포함한다.

- Backend: Java 21, Spring Boot, JPA, Spring Security, Spring Modulith, PostgreSQL, Flyway.
- Frontend: Next.js, TypeScript, Tailwind CSS, PWA.
- AI: 텍스트는 JEV 정책 평가 뒤 OpenAI/Ollama 생성, 음식 사진은 OpenAI 분석.
- Infrastructure: AWS CDK, EC2, RDS, ECR, SSM, GitHub Actions OIDC.

## 문서 찾기

| 작업·질문 | 먼저 읽을 문서 |
| --- | --- |
| 백엔드 실행·환경 설정·소스 탐색 | [Backend](docs/backend/README.md) |
| 화면 개발·정적 PWA 빌드 | [Frontend](frontend/README.md) |
| AWS·Parameter Store·배포·접속·복구 | [Infrastructure](infra/README.md) |
| Prometheus·Grafana·Slack·자원 인수 | [Monitoring](monitoring/README.md) |
| 전체 C4·모듈·계층·DTO·transaction 경계 | [Architecture](docs/architecture.md) |
| 현재 사용자·데이터 규칙 | [Product](docs/product.md) |
| JEV·답변 생성·사진 분석 계약 | [AI](docs/ai.md) |
| 검사 명령·테스트 작성·평가 실행 | [Testing](docs/testing.md) |

현재 동작과 절차는 위 문서를 기준으로 한다. README는 각 영역의 진입점이며 같은 계약을 여러 문서에 복제하지 않는다.
AI 작업 규칙과 읽기 경로는 [AGENTS.md](AGENTS.md)에 둔다.

## 아키텍처 결정

ADR은 하나의 중요한 선택과 그 이유·대안·불이익을 기록한다. 현재 계약의 상세 내용은 기준 문서로 연결한다.

- [0001 · 아키텍처·소스 규칙의 자동 검사](docs/adr/0001-architecture-guards.md)
- [0002 · JEV 단일 정책 경로와 장애 차단](docs/adr/0002-jev-policy-gate.md)
- [0003 · 음식 사진 분석과 사용자 확인 후 기록](docs/adr/0003-food-photo-recording.md)
- [0004 · 단일 EC2 모니터링과 자원 인수](docs/adr/0004-single-ec2-monitoring.md)

## 진행 중 스펙

독립적인 설계 관리가 필요한 작업에만 `docs/specs/YYYY-MM-DD-topic.md`를 만든다.
현재 진행 중 스펙은 없다. 구현 완료와 실제 공급자·운영 검증 완료는 구분한다.
모니터링의 남은 24시간 관측·부하·Slack 확인은 [현재 인수 항목](monitoring/README.md#운영-인수)에서 추적한다.

## 과거 스펙

완료된 스펙·독립 계획·당시 검증은 [docs/archive/specs](docs/archive/specs/)에 보존한다.
파일명은 기존 날짜와 주제를 유지한 `YYYY-MM-DD-topic-spec.md`, `-plan.md`, `-validation.md`다.
날짜·주제별 디렉터리와 별도 archive 목차는 만들지 않는다. 해당 주제의 ADR에서 관련 기록으로 연결한다.
과거 문서에 적힌 계획·운영 수치·검사 건수는 현재 코드나 최신 배포 상태를 대신하지 않는다.

## 정량 평가

- [Router seed](docs/evaluations/ai-policy/router-seed-v0/report.md): 변경 전 소규모 진단.
- [변경 전 1,000건](docs/evaluations/ai-policy/baseline-1000-v1/report.md): 저장 기준선과 재현 자료.
- [JEV 실호출 1,000건](docs/evaluations/ai-policy/jev-live-1000-v1/report.md): 동일 입력 비교·실패·표본 한계.
- [저장 응답 replay](docs/evaluations/ai-policy/policy-rule-replay/report.md): 모델 재호출 없이 판정 순서 재평가.

평가 원본 JSON/JSONL·manifest·hash·당시 조건은 유지한다. 실행 방법은 [Testing](docs/testing.md#ai-정책-평가)을 따른다.

## 문서 관리

- 현재 규칙·설정·절차는 소유 문서 하나에서 갱신하고 다른 문서에는 링크를 둔다.
- 작은 수정은 기존 문서를 갱신한다. 매 작업마다 spec·plan·validation을 새로 만들지 않는다.
- 완료 스펙의 유효 계약은 현재 문서에 반영하고 중요한 선택만 ADR로 추린 뒤 archive로 옮긴다.
- 남은 검증은 현재 영역 문서에서 추적한다. archive의 당시 사실을 최신 상태로 덮어쓰지 않는다.
- 구조·배포 경계 변경 시 C4와 해당 영역 문서를 함께 갱신한다. 링크·앵커·평가 원본을 확인한다.
- 생성된 테스트 결과·Modulith 그림은 build 아래에 둔다. 채택한 실측 근거만 Evaluations에 보존한다.
