# 프로젝트 작업 지침

## 먼저 읽을 문서

루트 [README](README.md#문서-찾기)에서 작업에 해당하는 문서만 선택한다.
archive와 평가 원본은 과거 판단·실험의 재현이 필요한 경우에 읽는다.

| 작업 | 기준 문서 |
| --- | --- |
| 백엔드 | [Backend](docs/backend/README.md), [Architecture](docs/architecture.md) |
| 프론트엔드 | [Frontend](frontend/README.md), [Product](docs/product.md) |
| AI | [AI](docs/ai.md), [Architecture](docs/architecture.md) |
| 배포·AWS | [Infrastructure](infra/README.md) |
| 지표·알림 | [Monitoring](monitoring/README.md) |
| 검사 | [Testing](docs/testing.md) |

## 변경과 검증

- 변경 전 가정·범위·검증 기준을 짧게 밝힌다. 저장소를 확인하고 불명확한 요구만 질문한다.
- 요청 범위만 수정한다. 새로운 계층·인터페이스·설정은 실제 필요가 있을 때 추가한다.
- 독립적으로 변경되는 책임은 의미 있는 메서드나 구체 클래스로 분리한다. 한 줄에 선언·대입을 압축하지 않는다.
- 모듈·계층·DTO·transaction 정책과 Java var 금지는 Architecture와 기존 자동 검사를 따른다.
- 코드 변경 후 Testing의 아키텍처·컨벤션 검사와 필요한 동작 검사를 실행한다.
  실패 원인을 수정하고 재실행한다. 통과를 위해 검사·허용 범위·기준선을 임의로 완화하지 않는다.
- 실행한 명령·결과·미검증 범위를 보고한다. 테스트 대역과 실공급자 평가·운영 확인을 구분한다.
- main에서 작업한다. 사용자 요청 없이 worktree를 만들지 않는다. commit 메시지는 한국어로 작성한다.
- 비밀값·질문 원문·사진·모델 원문·runtime.env를 문서나 검증 출력에 넣지 않는다.

## 문서 갱신

현재 계약과 절차는 해당 영역의 소유 문서에서 갱신한다. 문서 수를 늘리기 전에 기존 소유 문서를 찾는다.
독립 스펙이 필요하면 날짜·주제를 파일명에 넣는다. 완료 스펙은 유효 내용과 남은 검증을 현재 문서에 반영한 뒤
`docs/archive/specs/`로 이동한다. 중요한 선택만 ADR로 기록하고 이전 결정과 대체 관계를 보존한다.
문서 이동 시 링크·앵커·실행 소비 경로를 함께 수정한다. 평가 원본·manifest·hash를 변경하지 않는다.
문서 정리에서 평가 재실행·운영 변경을 하지 않는다. commit/push는 해당 작업의 사용자 지시를 따른다.
