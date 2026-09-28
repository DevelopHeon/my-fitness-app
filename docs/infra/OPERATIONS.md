# My Fitness Operations

production 환경을 확인하거나 장애를 조사할 때 사용하는 최소 운영 절차입니다.

---

## 1. 평소 확인할 곳

AWS Console에서 주로 확인할 영역:

| 서비스 | 확인 내용 |
| --- | --- |
| EC2 | Instance Running, status check, CPU |
| RDS | Available, CPU, FreeableMemory, FreeStorageSpace, DatabaseConnections |
| ECR | 최신 Git SHA image 존재 여부 |
| Systems Manager | Managed node 상태, Run Command history, Session history |
| Route 53 | dev-heon.com A record |
| CloudFormation | CDK Stack 상태 |
| Secrets Manager | RDS secret 상태 |
| Parameter Store | 운영 parameter 존재 여부 |
| CloudWatch | EC2 / RDS metrics와 RDS log |

---

## 2. Session Manager와 Run Command

둘은 목적이 다릅니다.

| 기능 | 사용 시점 | Active Session에 표시 |
| --- | --- | --- |
| Session Manager shell | 사람이 EC2 터미널에 접속 | 예 |
| SSM port forwarding | IntelliJ 등 로컬 → RDS | 예 |
| SSM Run Command | 배포 / 원격 명령 실행 | 아니오 |

따라서 Session Manager에 진행 중인 session이 없고 과거 Terminated 기록만 보여도 정상입니다.

GitHub Actions 배포는 Session Manager가 아니라 Run Command를 사용합니다.

---

## 3. EC2 접속

SSH 22를 열지 않고 SSM Session Manager를 사용합니다.

~~~bash
INSTANCE_ID=$(aws ssm get-parameter   --name /my-fitness/prod/instance-id   --region ap-northeast-2   --query 'Parameter.Value'   --output text)

aws ssm start-session   --region ap-northeast-2   --target "$INSTANCE_ID"
~~~

SessionManagerPlugin 오류가 나면 macOS에서 다음을 설치합니다.

~~~bash
brew install --cask session-manager-plugin
~~~

---

## 4. RDS를 IntelliJ에서 확인

RDS는 private이므로 직접 public 접속하지 않습니다.

먼저 EC2를 통해 port forwarding session을 엽니다.

~~~bash
INSTANCE_ID=$(aws ssm get-parameter   --name /my-fitness/prod/instance-id   --region ap-northeast-2   --query 'Parameter.Value'   --output text)

DB_HOST=$(aws ssm get-parameter   --name /my-fitness/prod/db-host   --region ap-northeast-2   --query 'Parameter.Value'   --output text)

aws ssm start-session   --target "$INSTANCE_ID"   --region ap-northeast-2   --document-name AWS-StartPortForwardingSessionToRemoteHost   --parameters "host=$DB_HOST,portNumber=5432,localPortNumber=15432"
~~~

IntelliJ Data Source:

~~~text
Host: localhost
Port: 15432
Database: my_fitness
User: RDS secret username
Password: RDS secret password
~~~

운영 DB는 가능하면 IntelliJ에서 read-only로 사용합니다.

---

## 5. Application 상태 확인

public health:

~~~bash
curl https://dev-heon.com/actuator/health
~~~

EC2 접속 후:

~~~bash
sudo docker ps
curl http://127.0.0.1:8080/actuator/health
~~~

정상적인 핵심 container:

~~~text
my-fitness-app
my-fitness-caddy
~~~

현재 배포 SHA 확인:

~~~bash
cat /opt/my-fitness/current-image
~~~

---

## 6. 로그 확인

Spring Boot:

~~~bash
sudo docker logs --tail 200 my-fitness-app
sudo docker logs -f --tail 100 my-fitness-app
sudo docker logs my-fitness-app 2>&1 | grep -Ei "error|exception|failed"
~~~

Caddy:

~~~bash
sudo docker logs --tail 200 my-fitness-caddy
sudo docker logs -f --tail 100 my-fitness-caddy
~~~

현재 application / Caddy 로그의 기본 source는 Docker stdout/stderr입니다.

---

## 7. 배포 확인

GitHub Actions에서 먼저 다음 순서로 봅니다.

~~~text
CI
 ↓ success
Deploy App
 ↓
Check image tag in ECR
 ↓
Build / Push if needed
 ↓
Deploy image through SSM
 ↓
Health check
~~~

배포 스크립트는 새 container가 health check에 실패하면 이전 image로 rollback을 시도합니다.

ECR에서 Git commit SHA와 같은 tag가 있는지 확인하면 build/push 여부를 판단하기 쉽습니다.

---

## 8. 장애 확인 순서

### 사이트 전체가 열리지 않을 때

~~~text
1. Route 53 DNS
2. EC2 Running / Status check
3. my-fitness-caddy container
4. my-fitness-app container
5. localhost:8080 health
6. public /actuator/health
~~~

### API 500

~~~text
1. my-fitness-app logs
2. exception stack trace
3. RDS status
4. DB connection / transaction 여부
5. 최근 배포 commit
~~~

### DB 연결 문제

~~~text
1. RDS Available
2. DB Security Group
3. EC2 → RDS 5432
4. Secrets Manager RDS secret
5. /my-fitness/prod/db-host
6. Spring Boot logs
~~~

### 배포 문제

~~~text
1. GitHub CI
2. Deploy App
3. ECR image
4. SSM Run Command history
5. EC2 docker logs
6. /opt/my-fitness/current-image
~~~

---

## 9. 운영에서 하지 않는 것

특별한 이유 없이 다음 설정을 바꾸지 않습니다.

- EC2 SSH 22를 인터넷에 개방
- RDS Publicly Accessible 활성화
- PostgreSQL 5432를 0.0.0.0/0에 개방
- Spring Boot 8080을 외부에 직접 공개
- GitHub 배포용 장기 AWS Access Key 생성
- 확인 없이 cdk deploy --all 실행

---

## 10. 정기적으로 확인할 항목

개인 프로젝트 기준으로 매일 확인할 필요는 없지만, 변경이나 장애가 있을 때는 다음을 확인합니다.

- 최신 Git SHA와 EC2 current-image가 일치하는가
- EC2 / RDS CPU가 지속적으로 높지 않은가
- RDS FreeableMemory와 FreeStorageSpace가 부족하지 않은가
- DB connection이 비정상적으로 증가하지 않는가
- 최근 배포에서 rollback이 발생하지 않았는가
- ECR lifecycle이 정상적으로 오래된 image를 정리하는가
- AWS 비용이 예상 범위에 있는가
