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
   - 사용자 기능과 제품 규칙

5. [Testing](testing/README.md)
   - 자동 테스트와 수동 테스트 기준

## 문서 역할

| 문서 | 역할 | 최신 상태 기준 |
| --- | --- | --- |
| architecture/README.md | 현재 소프트웨어 구조 | 예 |
| infra/README.md | 현재 운영 인프라 | 예 |
| infra/OPERATIONS.md | 현재 운영 절차 | 예 |
| spec/PRODUCT_SPEC.md | 현재 제품 요구사항 | 예 |
| testing/ | 테스트 정책 | 예 |
| spec/YYYY-...-pr-*.md | 구현 당시 판단과 작업 기록 | 아니오 |

구현 PR 스펙은 당시의 설계 의사결정을 남기는 이력입니다. 이후 리팩토링으로 구조가 바뀔 수 있으므로 현재 상태를 확인할 때는 Architecture / Infrastructure / Product Spec을 우선합니다.

## 문서 갱신 원칙

- 모듈 경계가 바뀌면 Architecture를 갱신합니다.
- AWS 리소스나 배포 방식이 바뀌면 Infrastructure와 Operations를 갱신합니다.
- 사용자 기능이나 제품 규칙이 바뀌면 Product Spec을 갱신합니다.
- 중요한 구현 단위는 spec 하위 PR 문서로 변경 이유를 남깁니다.
- 실제 코드와 문서가 다르면 코드를 기준으로 문서를 수정합니다.
