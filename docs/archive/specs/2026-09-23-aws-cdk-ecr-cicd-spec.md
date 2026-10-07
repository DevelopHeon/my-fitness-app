# PR-011 AWS CDK / ECR 기반 CI/CD 및 운영 배포

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.


작성일: 2026-09-23 (수)  
상태: 구현 예정  
선행 작업: PR-010 Google OAuth2 인증

## 1. 배경 및 목적

AWS 인프라를 코드화하고 main branch 변경을 자동 검증·배포한다.

현재 서비스는 개인/소수 사용자 중심이므로 고가용성보다 낮은 월 비용, 운영 단순성, 재현 가능한 인프라, 안전한 rollback을 우선한다.

기준:

- Region: `ap-northeast-2`
- EC2: `t4g.micro`
- RDS PostgreSQL: `db.t4g.micro`
- Registry: ECR
- IaC: AWS CDK v2 / TypeScript
- CI/CD: GitHub Actions
- AWS 인증: GitHub OIDC
- 원격 배포: SSM Run Command
- HTTPS: Caddy
- DNS: Route 53
- NAT Gateway / ALB / CodeDeploy / ECS / EKS 없음

## 2. ECR 사용 결정

ECR은 현재 구조에 적합하다.

- commit SHA 단위 immutable artifact
- CI/운영 실행 환경 차이 축소
- EC2에서 build하지 않고 pull/restart
- 이전 SHA로 rollback 가능
- 추후 ECS 전환 시 registry 재사용 가능

단일 EC2에서 CodeDeploy까지 추가하지 않는다.

배포 흐름:

```text
GitHub Actions
      │
      ▼
ECR image:<git-sha>
      │
      ▼
SSM Run Command
      │
      ▼
EC2 docker pull / compose up
      │
      ├─ health success → 완료
      └─ health fail    → previous SHA rollback
```

## 3. 목표 인프라

```text
Internet
   │
   ▼
Route 53
   │
   ▼
Elastic IP / Public IPv4
   │
   ▼
EC2 t4g.micro
Public Subnet / AZ-A
┌─────────────────────────────┐
│ Caddy :80/:443              │
│ Docker                      │
│  └─ my-fitness container    │
│      ├─ Spring Boot         │
│      ├─ Next.js static      │
│      └─ Spring Session JDBC │
└──────────────┬──────────────┘
               │ private 5432
               ▼
RDS PostgreSQL 17
db.t4g.micro / Single-AZ
Isolated Subnet / AZ-A
20 GiB gp3
```

## 4. EC2

```text
t4g.micro
2 vCPU
1 GiB RAM
AWS Graviton2 / arm64
Amazon Linux 2023 ARM64
root EBS gp3 20 GiB
```

micro부터 시작하되 1 GiB RAM은 여유가 크지 않으므로 메모리 제한을 필수로 둔다.

JVM 초기값:

```text
-Xms128m
-Xmx512m
-XX:+UseG1GC
```

운영 원칙:

- app container memory limit 약 650~700 MiB
- Docker/frontend build는 EC2에서 금지
- 1~2 GiB swap file 구성
- Caddy는 Docker 밖의 host systemd service로 실행
- EC2에서 상시 실행하는 application container는 1개로 유지
- Actuator health 사용

`t4g.small` 승격 조건:

- OOM 반복
- 메모리 장시간 80% 이상
- swap 지속 사용
- 배포 health check 메모리 부족 실패
- AI + 일반 API 동시 요청 latency 급증
- CPU credit 부족 반복

Instance type은 CDK config 한 곳에서 변경 가능하게 한다.

## 5. RDS

```text
PostgreSQL 17
db.t4g.micro
Single-AZ
gp3 20 GiB
autoscaling max 50 GiB
publiclyAccessible=false
backup retention=7 days
deletion protection=true
final snapshot required
storage encryption enabled
```

RDS SG는 EC2 application SG에서 오는 5432만 허용한다.

DB subnet group은 2개 AZ isolated subnet으로 구성하되 RDS primary와 EC2는 가능하면 같은 AZ에 두어 cross-AZ 비용을 줄인다.

micro DB에서 불필요하게 많은 connection을 만들지 않도록 production Hikari pool은 작은 값으로 시작한다.

초기 기준:

```text
maximum-pool-size=5
minimum-idle=1
```

실제 동시 사용자와 RDS `DatabaseConnections` 지표를 확인한 뒤에만 늘린다. Spring Session JDBC도 같은 DataSource를 사용하므로 session 저장 부하까지 포함해 관찰한다.

## 6. VPC

```text
VPC
├─ AZ-A
│  ├─ Public Subnet
│  │  └─ EC2
│  └─ Isolated DB Subnet
│     └─ RDS primary
└─ AZ-B
   └─ Isolated DB Subnet
      └─ DB subnet group
```

NAT Gateway와 초기 VPC Interface Endpoint는 만들지 않는다.

EC2는 public subnet의 IGW를 통해 ECR/SSM/Google/OpenAI에 outbound한다.

## 7. HTTPS / DNS

- Route 53 Public Hosted Zone
- EC2 Elastic IP
- A record → Elastic IP
- Caddy 80/443
- Let's Encrypt 자동 인증서
- Spring Boot 8080은 인터넷에 직접 노출하지 않음
- SSH 22 inbound 없음
- 관리 작업은 SSM 사용

Google callback:

```text
https://<domain>/login/oauth2/code/google
```

## 8. ECR

Private repository: `my-fitness`

Tag:

```text
<git-commit-sha>
latest
```

배포 기준은 `latest`가 아니라 commit SHA다.

Lifecycle:

- untagged 조기 삭제
- 최근 10개 SHA image 유지
- 오래된 image expire

동일 Seoul Region ECR → EC2 image transfer는 별도 ECR data transfer 비용이 없다.

## 9. Docker

GitHub Actions에서 ARM64 image를 만든다.

```text
docker buildx build
--platform linux/arm64
--tag <ecr>/my-fitness:<sha>
--push
```

원칙:

- Java 21 ARM64 runtime
- non-root user
- source/cache를 runtime image에 포함하지 않음
- 기존 `bootJar`에 frontend static 포함
- EC2에서 `docker build` 금지

## 10. Runtime compose

EC2의 `/opt/my-fitness`에 운영 compose/config를 둔다.

```yaml
services:
  app:
    image: <ecr>/my-fitness:${IMAGE_TAG}
    restart: unless-stopped
    mem_limit: 700m
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JAVA_TOOL_OPTIONS: "-Xms128m -Xmx512m"
```

secret은 compose에 평문 저장하지 않는다.

## 11. Secret 관리

Secrets Manager:

- RDS credential 1개

SSM Parameter Store Standard SecureString:

- Google client id
- Google client secret
- OpenAI API key

SSM String:

- APP_BASE_URL
- AI provider/model
- 기타 비민감 설정

RDS credential은 CDK generated secret을 사용한다.

EC2 Instance Role에 특정 secret/parameter read만 허용한다.

deploy script가 시작 시 secret/parameter를 읽어 root 전용 runtime env 파일을 구성하고 Docker에 주입한다. 이 과정에서 `set -x` 또는 secret echo를 사용하지 않아 SSM command output/CloudWatch log에 비밀 값이 남지 않게 한다.

GitHub Actions에는 DB/Google/OpenAI secret을 저장하지 않는다.

## 12. IAM

EC2 Role:

- SSM Managed Instance Core
- ECR pull
- CloudWatch logs/metrics
- 특정 Secrets Manager read
- `/my-fitness/prod/*` SSM read

GitHub deploy role:

- ECR push
- 특정 EC2에 대한 SSM SendCommand
- 필요한 describe 최소 권한

static AWS access key는 EC2/GitHub 어디에도 저장하지 않는다.

## 13. GitHub OIDC

GitHub Actions는 OIDC로 AWS IAM Role을 Assume한다.

Trust 범위:

- repository: `DevelopHeon/my-fitness-app`
- branch: `main`
- audience: `sts.amazonaws.com`

일반 CI job에는 AWS 권한이 없다. deploy job만 단기 credential을 얻는다.

## 14. Workflow

### ci.yml

Trigger:

- pull_request
- main push

검증:

```text
./gradlew test
cd frontend && npm ci && npm run lint && npm run build
./gradlew build
```

### deploy-app.yml

main CI 성공 후:

1. checkout
2. Java/Node 설정
3. `./gradlew bootJar`
4. OIDC role assume
5. ECR login
6. Buildx ARM64 build
7. SHA tag push
8. SSM SendCommand
9. EC2 new image pull
10. 이전 SHA 기록
11. compose restart
12. `/actuator/health` polling
13. 성공 시 current SHA 갱신
14. 실패 시 previous SHA rollback

### deploy-infra.yml

- PR: CDK test/synth/diff
- 실제 deploy: `workflow_dispatch` 우선
- app main push마다 `cdk deploy`하지 않는다.

## 15. Rollback / DB migration

단일 EC2이므로 strict zero-downtime은 목표가 아니다. container 교체 중 수 초 502는 초기 허용 범위다.

EC2에는 current/previous SHA를 저장한다.

health 실패 시 previous image를 재기동한다.

Flyway는 rollback 호환성을 고려한다.

- destructive schema + 코드 제거를 한 release에 같이 하지 않음
- expand/contract 우선
- PostgreSQL transactional migration 활용

## 16. CDK 구조

```text
infra/
├── bin/my-fitness.ts
├── lib/
│   ├── network-stack.ts
│   ├── database-stack.ts
│   ├── application-stack.ts
│   └── cicd-stack.ts
├── test/
├── cdk.json
├── package.json
└── tsconfig.json
```

AWS CDK v2 + TypeScript를 사용하고 project dependency로 version pinning한다.

## 17. Stack 책임

NetworkStack:

- VPC
- public/isolated subnet
- route
- EC2/RDS SG

DatabaseStack:

- PostgreSQL 17
- db.t4g.micro
- 20 GiB gp3
- backup/encryption/deletion
- generated secret

ApplicationStack:

- ECR
- EC2 t4g.micro ARM64
- gp3 root
- Elastic IP
- IAM instance role
- Route 53
- bootstrap UserData
- CloudWatch

CicdStack:

- GitHub OIDC provider
- app deploy role
- optional infra deploy role

## 18. EC2 bootstrap

UserData:

- Docker/Compose
- Caddy
- SSM Agent
- `/opt/my-fitness`
- swap
- log directory
- deploy/rollback script

application image는 UserData에 고정하지 않는다.

## 19. 모니터링

초기:

- EC2 기본 metrics
- CloudWatch Agent memory/disk
- application stdout/stderr Logs
- Caddy log retention 7~14일
- Actuator health

Alarm 후보:

- EC2 StatusCheckFailed
- memory > 80%
- disk > 80%
- CPU high / credit 부족
- RDS CPU high
- FreeStorageSpace low
- DatabaseConnections 이상 증가

Container Insights는 사용하지 않는다.

## 20. 비용 최적화

초기 제외:

- NAT Gateway
- ALB
- ECS/Fargate
- EKS
- ElastiCache
- Multi-AZ RDS
- bastion
- 다수 Interface Endpoint
- WAF
- CodeDeploy

측정 결과가 필요성을 보여줄 때만 추가한다.

## 21. 예상 월 비용

기준:

- Seoul ap-northeast-2
- 730시간
- On-Demand
- EC2/RDS 24시간
- EC2 gp3 20 GB
- RDS gp3 20 GB
- ECR 1~2 GB
- internet outbound 100 GB 미만
- CloudWatch log 5 GB 미만
- 신규 계정 credit 제외
- 세금/도메인/OpenAI 제외

| 항목 | 기준 | 월 예상 |
| --- | ---: | ---: |
| EC2 t4g.micro | 약 $0.0104/h | $7.59 |
| EC2 gp3 20 GB | 약 $0.096/GB-month | $1.92 |
| Public IPv4 1개 | $0.005/h | $3.65 |
| RDS PostgreSQL db.t4g.micro | 약 $0.025/h | $18.25 |
| RDS gp3 20 GB | 약 $0.131/GB-month | $2.62 |
| Route 53 Hosted Zone | $0.50/month | $0.50 |
| ECR | $0.10/GB-month | $0.10~0.20 |
| Secrets Manager 1개 | $0.40/month + calls | 약 $0.40 |
| SSM Parameter Store Standard | 추가 저장 비용 없음 | $0 |
| SSM Run Command | 추가 비용 없음 | $0 |
| CloudWatch | 소규모/free allowance 가정 | $0~1 |
| Internet outbound | 100 GB 미만 | $0 |

합계:

```text
약 $35 ~ $36 / month
```

예산 계산을 단순히 `1 USD = 1,500 KRW`로 잡으면 약 52,500~54,000원이다.

세금 10%를 별도 예산으로 더 잡는다면 약 58,000~59,000원 수준을 월 운영 예산으로 본다.

포함하지 않음:

- domain registration
- OpenAI token 비용
- 100 GB 초과 DTO
- RDS 무료 backup allowance 초과분
- 장기 manual snapshot
- GitHub 유료 runner/minute
- EC2/RDS burstable CPU의 지속 초과 사용에 따른 추가 credit 비용
- 장애용 임시 resource

## 22. micro 성능 검증

production-like container로 측정한다.

- idle RSS
- startup peak
- Google login + Dashboard + AI 동시 요청 memory
- GC pause
- p95 latency
- CPU credit
- RDS connections/free memory
- OS + Docker + Caddy 전체 memory

micro에서 OOM/지속 swap이 나타나면 억지 JVM 축소보다 `t4g.small` 승격을 우선한다.

## 23. 보안

- RDS public access 금지
- SSH 22 금지
- static AWS key 금지
- GitHub OIDC
- EC2 Instance Role
- secret git/image 포함 금지
- private ECR
- non-root container
- HTTPS 강제
- production Actuator 최소 노출

## 24. 구현 순서

1. `infra/` CDK TypeScript
2. NetworkStack
3. DatabaseStack
4. ECR / ApplicationStack
5. IAM / SSM / Secrets
6. EC2 Docker/Caddy bootstrap
7. ARM64 Dockerfile
8. local container smoke
9. GitHub OIDC
10. CI workflow
11. ECR push workflow
12. SSM deploy/health
13. rollback
14. Route 53 / HTTPS
15. Google production callback
16. CloudWatch
17. CDK test/synth
18. 실제 deploy/smoke
19. 비용 확인

## 25. 완료 조건

- CDK로 핵심 infra 재현
- RDS private
- SSH port 없음
- GitHub static AWS key 없음
- PR test/lint/build 자동화
- main → ECR SHA image
- SSM 자동 배포
- health 실패 rollback
- HTTPS Google OAuth 정상
- Flyway production RDS 정상
- CloudWatch log 확인
- micro 부하 테스트 OOM 없음
- 실제 월 비용이 예상 범위와 크게 다르지 않음

## 26. 제외 범위

- ECS/Fargate
- EKS
- ALB
- ASG
- Multi-AZ RDS
- NAT Gateway
- Redis/Kafka
- CodeDeploy
- managed blue/green
- CloudFront/WAF
- Terraform
- 상시 staging
- zero-downtime SLA

## 27. 참고

- https://docs.aws.amazon.com/cdk/v2/guide/
- https://docs.aws.amazon.com/cdk/v2/guide/work-with-cdk-typescript.html
- https://aws.amazon.com/ecr/pricing/
- https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html
- https://docs.aws.amazon.com/systems-manager/latest/userguide/run-command.html
- https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws
- https://aws.amazon.com/vpc/pricing/
- https://aws.amazon.com/rds/postgresql/pricing/
- https://aws.amazon.com/route53/pricing/

## 28. 구현 결과 및 운영 상태

### 실제 생성 완료

2026-09-23 기준 Seoul Region에 다음 리소스를 CDK로 생성했다.

- `MyFitnessNetwork`: VPC, 2 AZ subnet, application/database Security Group
- `MyFitnessDatabase`: PostgreSQL 17 `db.t4g.micro`, Single-AZ, private RDS
- `MyFitnessApplication`: `t4g.micro` ARM64 EC2, ECR, Elastic IP, IAM, SSM parameter
- `MyFitnessCicd`: GitHub OIDC provider 및 deploy IAM role
- CDK bootstrap stack

EC2는 SSM Managed Instance로 Online 상태이며 SSH 22 port를 열지 않았다. EC2에서 private RDS의 5432 TCP 연결도 확인했다.

### 첫 CI/CD 실제 검증

실제 GitHub Actions pipeline으로 다음 경로를 끝까지 검증했다.

```text
main CI
  → GitHub OIDC
  → AWS IAM Role
  → ECR ARM64 image push
  → SSM Run Command
  → EC2 docker pull / start
  → RDS 연결
  → Flyway migrate
  → /actuator/health
```

첫 성공 배포에서 애플리케이션 health status는 `UP`이었고 PostgreSQL 17 RDS에 Flyway V1~V9가 모두 적용되었다.

### 구현 중 발견 및 수정

Amazon Linux 2023 기본 `curl-minimal`과 full `curl` package가 충돌해 최초 UserData의 DNF bootstrap이 중단됐다.

- 현재 EC2는 SSM으로 Docker / jq / 2 GiB swap 구성을 복구했다.
- CDK UserData에서는 full `curl` 설치를 제거했다.
- 이후 신규 EC2 생성에는 수정된 bootstrap이 적용된다.

GitHub repository는 immutable OIDC subject를 사용한다. 따라서 IAM trust도 단순 repository name이 아니라 GitHub가 발급하는 immutable repository/owner ID 기반 subject와 main branch 조건을 정확히 사용한다. 이 조건은 CDK test로 검증한다.

### 남은 운영 작업

- 운영 도메인 확정
- Caddy 설치 및 systemd 관리
- Route 53 DNS 연결
- Let's Encrypt HTTPS
- Google production callback 연결
- Google/OpenAI SecureString parameter 입력
- CloudWatch Agent memory/disk/log 구성
- CloudWatch alarm
- micro 실제 메모리/CPU 부하 측정

현재 애플리케이션 container는 `127.0.0.1:8080`에만 bind되어 있으며 외부 HTTP/HTTPS 공개는 reverse proxy 구성 이후 진행한다.

### micro 초기 실측 기준선

첫 배포 직후 idle/낮은 트래픽 상태를 측정했다.

EC2:

- OS memory: 약 916 MiB
- used: 약 559 MiB
- available: 약 276 MiB
- application container: 약 387 MiB / 700 MiB limit
- swap: 약 18 MiB / 2 GiB
- JVM: `-Xms128m -Xmx512m -XX:+UseG1GC`
- load average: 매우 낮은 상태

RDS:

- 최근 CPU: 대략 4~9% 범위
- application 연결 후 DatabaseConnections: 2
- FreeableMemory: 대략 180~200 MB
- SwapUsage: 약 0.5 MiB

현재는 `t4g.micro + db.t4g.micro`를 유지한다. 다만 두 리소스 모두 메모리 여유가 크지 않으므로 실제 로그인/AI 동시 요청과 부하 테스트 후 다시 판단한다. CPU credit은 생성 직후 값이므로 충분한 운영 시간이 지난 후 추세로 판단한다.
