# My Fitness Documentation

이 디렉터리는 현재 시스템을 이해하기 위한 문서와 구현 이력을 분리해서 관리합니다.

## 처음 읽을 때

아래 순서만 읽으면 현재 시스템의 전체 구조를 이해할 수 있습니다.

1. [Architecture](architecture/README.md)
   - My Fitness가 어떤 시스템인지
   - C4 Context / Container / Component
   - Spring Modulith 모듈 경계
   - 트랜잭션과 Port/Adapter 규칙

2. [Infrastructure](infra/README.md)
   - AWS에 어떻게 배포되어 있는지
   - EC2, RDS, ECR, SSM, GitHub Actions의 관계
   - 보안 경계와 설정 관리

3. [Operations](infra/OPERATIONS.md)
   - 서버 접속
   - RDS 로컬 연결
   - 로그 확인
   - 배포 및 장애 확인 순서

4. [Product Spec](spec/PRODUCT_SPEC.md)
   - 기존 사용자 기능과 제품 규칙
   - 음식 사진·직접 식단 기록 변경은 [PR-013 계약](spec/2026-10-01-thu-pr-013-food-photo-meal-recording.md)과 현재 Architecture/Frontend README를 함께 확인

5. [Testing](testing/README.md)
   - 자동 테스트와 수동 테스트 기준

## 문서 역할

| 문서 | 역할 | 최신 상태 기준 |
| --- | --- | --- |
| architecture/README.md | 현재 소프트웨어 구조 | 예 |
| infra/README.md | 현재 운영 인프라 | 예 |
| infra/OPERATIONS.md | 현재 운영 절차 | 예 |
| spec/PRODUCT_SPEC.md | 기존 제품 요구사항 기준; 이번 변경은 PR-013 계약 참고 | 이번 작업에서는 갱신 제외 |
| testing/ | 테스트 정책 | 예 |
| spec/YYYY-...-pr-*.md | 구현 당시 판단과 작업 기록 | 아니오 |

구현 PR 스펙은 당시의 설계 의사결정을 남기는 이력입니다. 이후 리팩토링으로 구조가 바뀔 수 있으므로 현재 상태를 확인할 때는 Architecture / Infrastructure / Product Spec을 우선합니다.

## 문서 갱신 원칙

- 모듈 경계가 바뀌면 Architecture를 갱신합니다.
- AWS 리소스나 배포 방식이 바뀌면 Infrastructure와 Operations를 갱신합니다.
- 사용자 기능이나 제품 규칙이 바뀌면 Product Spec을 갱신합니다.
- 중요한 구현 단위는 spec 하위 PR 문서로 변경 이유를 남깁니다.
- 실제 코드와 문서가 다르면 코드를 기준으로 문서를 수정합니다.


## 음식 사진·직접 식단 기록 변경 문서

[PR-013](spec/2026-10-01-thu-pr-013-food-photo-meal-recording.md)은 음식 카탈로그 제거, 사진 후보의 입력 초안, 한국 시간 기본값, 선택 탄단지와 실패 계약을 정리한 스펙입니다. 구현 이후 현재 동작은 [Architecture](architecture/README.md), [루트 README](../README.md), [Frontend README](../frontend/README.md), [Operations](infra/OPERATIONS.md#음식-사진과-직접-식단-기록-배포)에 반영했습니다.

새 스펙과 탐색 링크를 추가했고, 사용자 요청에 따라 구현 후 최신화에서는 과거 스펙·테스트 문서와 평가 산출물을 제외했습니다. 해당 문서의 음식 카탈로그·회분 내용은 이번 구현과 다를 수 있습니다. 실제 모델 품질 평가와 운영 배포는 코드·일반 빌드 검증 완료와 별도입니다.
