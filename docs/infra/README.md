# My Fitness Infrastructure

이 문서는 현재 production 환경의 AWS 구성을 설명합니다.

CDK 구현 세부사항보다 "요청이 어디로 들어오고, 데이터가 어디에 있고, 배포가 어떻게 되는가"를 이해하는 데 초점을 둡니다.

운영 명령과 장애 대응은 [OPERATIONS.md](OPERATIONS.md)를 참고합니다.

---

## 1. Production Topology

Region: ap-northeast-2 (Seoul)

~~~mermaid
flowchart LR
    User["사용자"]
    DNS["Route 53<br/>dev-heon.com"]
    EIP["Elastic IP"]

    subgraph EC2["EC2 t4g.micro / ARM64"]
        Caddy["Caddy<br/>80 / 443"]
        App["my-fitness-app<br/>127.0.0.1:8080"]
    end

    DB[("RDS PostgreSQL 17<br/>db.t4g.micro")]
    ECR["Amazon ECR<br/>my-fitness"]
    SSM["AWS Systems Manager"]
    GitHub["GitHub Actions"]
    IAM["GitHub OIDC IAM Role"]
    Google["Google OIDC"]
    OpenAI["OpenAI API"]

    User -->|"HTTPS"| DNS
    DNS --> EIP
    EIP --> Caddy
    Caddy --> App
    App -->|"5432"| DB
    App --> Google
    App --> OpenAI

    GitHub -->|"OIDC"| IAM
    IAM --> ECR
    IAM -->|"Run Command"| SSM
    SSM --> EC2
    ECR --> EC2
~~~

---

## 2. AWS Resources

| 영역 | 현재 구성 |
| --- | --- |
| Region | ap-northeast-2 |
| VPC | 10.20.0.0/16, 2 AZ |
| Subnet | Public + Private Isolated Database |
| NAT Gateway | 없음 |
| EC2 | t4g.micro, ARM64, Amazon Linux 2023 |
| EC2 root volume | gp3 20 GiB, encrypted |
| EC2 swap | 2 GiB |
| RDS | PostgreSQL 17.9, db.t4g.micro, Single-AZ |
| RDS storage | gp3 20 GiB, autoscaling max 50 GiB, encrypted |
| RDS backup | 7일 |
| RDS public access | 비활성 |
| ECR | my-fitness, immutable tag, scan on push, 최근 10개 유지 |
| Deployment | GitHub Actions OIDC + ECR + SSM Run Command |
| HTTPS | Caddy + Let's Encrypt |
| DNS | Route 53 |
| SSH | 사용하지 않음 |

현재 개인 프로젝트 규모에 맞춰 ALB, ECS, EKS, Auto Scaling Group, NAT Gateway를 사용하지 않습니다.

---

## 3. Network / Security

~~~text
Internet
   │
   ├── 80  ─┐
   └── 443 ─┴─> EC2 Application Security Group
                    │
                    └── Caddy
                         │
                         └── 127.0.0.1:8080 Spring Boot

EC2 Application Security Group
   │
   └── 5432
        ↓
RDS Database Security Group
~~~

보안 기준:

- EC2 ingress는 80 / 443만 허용합니다.
- SSH 22는 열지 않습니다.
- Spring Boot 8080은 localhost에만 bind합니다.
- RDS는 public access를 사용하지 않습니다.
- RDS 5432는 EC2 Security Group에서만 접근 가능합니다.
- EC2 관리는 SSM을 사용합니다.

---

## 4. CDK Stack

infra 디렉터리는 AWS CDK v2 / TypeScript로 구성되어 있습니다.

| Stack | 역할 |
| --- | --- |
| MyFitnessNetwork | VPC, subnet, Application / Database Security Group |
| MyFitnessDatabase | RDS PostgreSQL과 DB generated secret |
| MyFitnessApplication | EC2, Elastic IP, ECR, EC2 IAM Role, runtime SSM parameter |
| MyFitnessCicd | GitHub OIDC Provider와 deploy IAM Role |

의존 순서:

~~~text
MyFitnessNetwork
      ↓
MyFitnessDatabase
      ↓
MyFitnessApplication
      ↓
MyFitnessCicd
~~~

Route 53 record와 Caddy 실행 자체는 현재 CDK Stack에 포함되어 있지 않습니다.

Caddy는 scripts/setup-caddy.sh로 구성합니다.

---

## 5. Runtime on EC2

EC2에는 두 개의 Docker container가 핵심입니다.

~~~text
my-fitness-caddy
  - host network
  - 80 / 443
  - TLS certificate
  - reverse_proxy 127.0.0.1:8080

my-fitness-app
  - Spring Boot
  - 127.0.0.1:8080
  - memory limit 700m
  - restart unless-stopped
~~~

JVM 기본 운영 옵션:

~~~text
-Xms128m
-Xmx512m
-XX:+UseG1GC
~~~

Hikari:

~~~text
maximum-pool-size = 5
minimum-idle = 1
~~~

---

## 6. Configuration and Secrets

### CDK가 생성하는 Parameter Store

~~~text
/my-fitness/prod/instance-id
/my-fitness/prod/ecr-repository-uri
/my-fitness/prod/db-host
/my-fitness/prod/db-secret-arn
~~~

### 운영에서 별도로 관리하는 Parameter Store

~~~text
/my-fitness/prod/google-client-id
/my-fitness/prod/google-client-secret
/my-fitness/prod/openai-api-key
/my-fitness/prod/app-base-url
/my-fitness/prod/ai-provider
~~~

비밀번호나 API key는 SecureString을 사용합니다.

RDS username/password는 Parameter Store가 아니라 RDS가 생성한 Secrets Manager secret에 있습니다.

배포 스크립트는 이 값들을 읽어 EC2의 다음 파일을 만듭니다.

~~~text
/opt/my-fitness/runtime.env
~~~

파일 권한은 600으로 생성합니다.

---

## 7. Application Deployment

애플리케이션 배포와 인프라 배포는 분리합니다.

~~~mermaid
flowchart LR
    Push["main push"] --> CI["GitHub CI"]
    CI -->|"success"| Deploy["Deploy App"]
    Deploy -->|"OIDC AssumeRole"| AWS["AWS"]
    AWS -->|"image exists?"| ECR["ECR"]
    Deploy -->|"build when needed"| ECR
    Deploy -->|"SSM Run Command"| EC2["EC2"]
    EC2 -->|"docker pull"| ECR
    EC2 --> Health["/actuator/health"]
    Health -->|"success"| Done["new image"]
    Health -->|"failure"| Rollback["previous image rollback"]
~~~

Docker image tag는 Git commit SHA를 사용합니다.

ECR tag는 immutable이므로 같은 SHA를 다시 배포할 때는 이미 존재하는 이미지를 재사용합니다.

---

## 8. Infrastructure Deployment

인프라 변경은 infra 디렉터리에서 검증합니다.

~~~bash
cd infra
npm ci
npm run build
npm test -- --runInBand
npx cdk synth
npx cdk diff
~~~

cdk diff를 확인한 뒤 필요한 Stack만 배포합니다.

~~~bash
npx cdk deploy <StackName> --exclusively
~~~

특히 EC2 UserData, subnet, instance 관련 변경은 replacement 가능성이 있으므로 전체 Stack을 습관적으로 deploy하지 않습니다.

---

## 9. Monitoring Status

현재 확인 가능한 기본 경로:

- EC2 / RDS CloudWatch 기본 metrics
- RDS PostgreSQL log export
- Application health endpoint
- Docker stdout/stderr logs
- GitHub Actions deploy history
- SSM Run Command history

EC2 IAM Role에는 CloudWatch Agent 권한이 있지만, 애플리케이션 Docker 로그의 중앙 수집과 운영 알람은 아직 핵심 운영 구성으로 고정되어 있지 않습니다.

추가할 가치가 높은 항목:

- EC2 CPU / memory / disk alarm
- RDS CPU / FreeableMemory / FreeStorageSpace / DatabaseConnections alarm
- Application health monitoring
- AWS Budget / Cost alert
- Application / Caddy log shipping

---

## 10. 관련 문서

- [Architecture](../architecture/README.md)
- [Operations](OPERATIONS.md)
- [CDK 개발 문서](../../infra/README.md)
- [CI workflow](../../.github/workflows/ci.yml)
- [Deploy workflow](../../.github/workflows/deploy-app.yml)
