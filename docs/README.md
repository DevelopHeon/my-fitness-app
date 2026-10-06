# 문서 안내

현재 동작은 Reference, 실행 방법은 Guides, 당시 설계·검증은 Changes, 정량 실험은 Evaluations에서 찾는다.
과거 스펙의 계획·수치가 현재 코드나 운영 배포 상태를 대신하지 않는다.

## 현재 기준과 실행 방법

| 알고 싶은 것 | 문서 |
| --- | --- |
| 어떤 기능과 입력 규칙이 있는가? | [제품 기능](reference/product.md) |
| 모듈·계층·DTO·transaction·검사는 어떤 경계를 지키는가? | [Architecture](reference/architecture.md) |
| AI 정책과 사진 분석은 어떤 계약으로 처리하는가? | [AI](reference/ai.md) |
| AWS·환경 변수·Parameter Store는 무엇을 정의하는가? | [Infrastructure](reference/infrastructure.md) |
| 로컬 실행·정적 PWA 빌드는 어떻게 하는가? | [Development](guides/development.md) |
| 어떤 검사를 어떻게 실행하는가? | [Testing](guides/testing.md) |
| 앱·인프라 배포와 schema 복구는 어떻게 하는가? | [Deployment](guides/deployment.md) |
| 접속·로그·장애 원인은 어디서 확인하는가? | [Troubleshooting](guides/troubleshooting.md) |
| 로컬·운영 지표와 Slack 알림은 어떻게 사용하는가? | [Monitoring](guides/monitoring.md) |

## 변경 기록

폴더 이름은 당시 날짜와 주제다. 상태가 완료돼도 경로를 바꾸지 않는다.
별도 결과가 없는 초기 스펙은 본문에 당시 검증을 포함한다.

| 날짜 | 주제와 당시 근거 |
| --- | --- |
| 2026-09-18 | [프로젝트 시작](changes/2026/2026-09-18-project-bootstrap/spec.md), [운동 기반](changes/2026/2026-09-18-workout-foundation/spec.md), [루틴](changes/2026/2026-09-18-routine/spec.md), [운동 카탈로그](changes/2026/2026-09-18-exercise-catalog/spec.md) |
| 2026-09-18 | [신체 기록](changes/2026/2026-09-18-body-record/spec.md), [운동 달력](changes/2026/2026-09-18-workout-calendar/spec.md), [운동 편집·이동](changes/2026/2026-09-18-workout-editing-navigation/spec.md), [대시보드](changes/2026/2026-09-18-dashboard/spec.md), [당시 식단 설계](changes/2026/2026-09-18-nutrition/spec.md) |
| 2026-09-22 | [AI Coach](changes/2026/2026-09-22-ai-coach/spec.md) |
| 2026-09-23 | [Google 로그인](changes/2026/2026-09-23-google-oauth2-auth/spec.md), [AWS·CI/CD](changes/2026/2026-09-23-aws-cdk-ecr-cicd/spec.md) |
| 2026-09-28 | 정책 평가 [스펙](changes/2026/2026-09-28-ai-policy-validation/spec.md) · [독립 계획](changes/2026/2026-09-28-ai-policy-validation/plan.md) · [검증](changes/2026/2026-09-28-ai-policy-validation/validation.md) |
| 2026-09-29 | JEV 단일 경로 [스펙·계획](changes/2026/2026-09-29-jev-single-path/spec.md) · [검증](changes/2026/2026-09-29-jev-single-path/validation.md), [아키텍처·패키지 정리 검증](changes/2026/2026-09-29-architecture-package-cleanup/validation.md) |
| 2026-10-01 | [AI 책임 분리](changes/2026/2026-10-01-ai-responsibility-refactoring/spec.md), [사진과 직접 식단 기록](changes/2026/2026-10-01-food-photo-meal-recording/spec.md) |
| 2026-10-04 | 로컬 관측 [스펙](changes/2026/2026-10-04-local-observability/spec.md) · [검증](changes/2026/2026-10-04-local-observability/validation.md) |
| 2026-10-06 | 단일 EC2 운영 모니터링 [스펙·계획](changes/2026/2026-10-06-single-ec2-monitoring/spec.md) · [검증](changes/2026/2026-10-06-single-ec2-monitoring/validation.md) — 구현·로컬 검증, 운영 자원 인수는 별도 |

## 정량 평가

- [Router seed](evaluations/ai-policy/router-seed-v0/report.md): 초기 입력과 판정.
- [변경 전 1,000건](evaluations/ai-policy/baseline-1000-v1/report.md): 기존 판정·manifest·metrics·cases.
- [JEV 1,000건 실호출](evaluations/ai-policy/jev-live-1000-v1/report.md): 같은 ID 비교와 표본·실패 한계.
- [저장 응답 규칙 재평가](evaluations/ai-policy/policy-rule-replay/report.md): 재호출 없는 replay, 모델 재실험과 구분.

원본 JSON/JSONL은 당시 결과다. 후속 평가로 덮어쓰지 않는다. 실행 방법은 [Testing](guides/testing.md#ai-정책-평가)에 둔다.

## 작성과 갱신 규칙

- 현재 계약의 소유 문서는 하나만 둔다. 다른 문서는 요약과 링크로 연결한다.
- 실행 절차에는 작업 위치·전제 조건·명령을, 결과에는 실행 환경·대상·실패·미검증 범위를 적는다.
- 구현 완료 시 Reference·Guides를 갱신한다. 당시 선택 이유와 결과는 Changes에 보존한다.
- 작은 수정은 기존 기준 문서를 고친다. 의미 있는 설계 변경에만 Changes를 추가한다.
- 짧은 계획은 spec.md에 포함한다. 별도 관리가 필요한 긴 계획만 plan.md로 분리한다.
- templates에는 반복 사용하는 [수동 검사 양식](templates/manual-test.md)을 둔다.
- 문서 이동 시 링크·앵커·실행 경로를 함께 수정하고 평가 원본 hash를 유지한다.
- 루트 README는 프로젝트 소개, docs/README는 문서 탐색을 담당한다. 하위 코드 디렉터리에 중복 README를 만들지 않는다.

이 구조의 세부 주제는 이 프로젝트의 선택이다. 공통 작성 스킬은 역할·중복·갱신·이력 보존 원칙만 정의한다.
