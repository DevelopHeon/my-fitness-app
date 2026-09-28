# My Fitness AWS CDK

production AWS 인프라를 정의하는 CDK v2 / TypeScript 프로젝트입니다.

전체 인프라 구조와 운영 방법은 아래 문서를 먼저 참고합니다.

- [Infrastructure Overview](../docs/infra/README.md)
- [Operations](../docs/infra/OPERATIONS.md)

## Stacks

- MyFitnessNetwork: VPC, public / isolated subnet, security group
- MyFitnessDatabase: PostgreSQL RDS와 generated secret
- MyFitnessApplication: EC2, Elastic IP, ECR, EC2 IAM Role, runtime SSM parameter
- MyFitnessCicd: GitHub Actions OIDC와 deploy IAM Role

Region은 ap-northeast-2를 기본으로 사용합니다.

## Verify

~~~bash
npm ci
npm run build
npm test -- --runInBand
npx cdk synth
npx cdk diff
~~~

## Deploy

항상 cdk diff를 먼저 확인하고 필요한 Stack만 배포합니다.

~~~bash
npx cdk deploy <StackName> --exclusively
~~~

EC2 UserData나 네트워크 변경은 resource replacement로 이어질 수 있으므로 전체 Stack을 습관적으로 배포하지 않습니다.

## Application Release

애플리케이션 release는 CDK deploy와 별개입니다.

GitHub Actions가 다음 순서로 처리합니다.

~~~text
CI
 → ARM64 Docker image
 → ECR
 → SSM Run Command
 → scripts/deploy-ec2.sh
 → EC2 health check
 → success 또는 previous image rollback
~~~

EC2에서 애플리케이션을 직접 build하지 않습니다.

SSH도 사용하지 않습니다.

## HTTPS

production DNS는 Route 53의 dev-heon.com을 사용하고, EC2의 Caddy가 TLS와 reverse proxy를 담당합니다.

Caddy 설정 스크립트:

~~~bash
scripts/setup-caddy.sh dev-heon.com
~~~

Spring Boot container는 외부에 8080을 노출하지 않고 127.0.0.1:8080에만 bind합니다.

## Bootstrap

Amazon Linux 2023 bootstrap은 다음을 준비합니다.

- Docker
- jq
- AWS CLI
- /opt/my-fitness
- 2 GiB swap
- region file

Amazon Linux 2023의 curl-minimal과 충돌할 수 있으므로 bootstrap package에 full curl을 별도로 설치하지 않습니다.
