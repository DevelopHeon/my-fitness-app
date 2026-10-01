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

## AI 정책 설정과 승격 보류 (PR-012)

일반 텍스트 AI 질문은 JEV 평가를 반드시 거친다. `scripts/deploy-ec2.sh`는 `/my-fitness/prod/` Parameter Store에서 아래 값을 읽어 권한 600의 runtime.env에 기록한다. AWS parameter는 사용자가 등록하며 main 푸시 후 CI가 성공하면 Deploy App이 자동으로 실행된다. 수동 실행도 지원한다.

| 전체 parameter 이름 | 타입 | 환경 변수 | 입력값 / 기본값 |
| --- | --- | --- | --- |
| /my-fitness/prod/typesafe-api-key | SecureString | TYPESAFE_API_KEY | TypeSafe 계정에서 발급한 실제 JEV API key, 필수 |
| /my-fitness/prod/ai-policy-model | String | AI_POLICY_MODEL | jev-1.13.0, 미등록 시 같은 기본값 |
| /my-fitness/prod/ai-policy-version | String | AI_POLICY_VERSION | fitness-policy-v1, 미등록 시 같은 기본값 |

키 누락·빈 값이나 SSM 접근 오류는 환경 파일과 실행 중 container를 교체하기 전에 배포를 중단한다. 모델/버전의 ParameterNotFound만 기본값으로 처리한다. 신규 코드에서 AI_POLICY_MODE와 TYPESAFE_MODEL을 읽지 않으며 이전 ai-policy-mode parameter가 남아 있어도 경로 선택에 사용하지 않는다. key와 runtime.env를 로그에 출력하지 않는다. 고객 관리 KMS 키를 쓰면 EC2 role의 해당 키 decrypt 권한을 확인한다.

로컬 서버에도 TYPESAFE_API_KEY를 주입한다. JEV API key가 없는 서버는 텍스트 AI 요청에서 AI_POLICY_UNAVAILABLE / 503을 반환하며 keyword 판정이나 답변 생성으로 우회하지 않는다. timeout은 1500ms, review/action/topic 임계값은 0.35/0.70/0.60이다. 자동 재시도와 다른 provider failover는 없다.

실제 key로 모델 접근·응답 계약을 확인하고, 독립 holdout·실제 모델 3회 평가·120개 E2E 사람 검토·전송/보존 조건·budget·staging 1000회·PostgreSQL migration을 확인한다. 로컬 대역 통과가 모델 품질 검증을 뜻하지 않는다. rollback은 검증한 이전 이미지와 모델/정책 설정을 함께 복원한다. version 문자열 변경만으로 규칙이 복원되지는 않는다. 이전 legacy/shadow 이미지로 되돌리면 안전 정책 경로도 바뀌므로 기록한다. 이번 작업에서 실제 배포와 staging rollback은 실행하지 않았다.

배포 설정 로컬 검증: `bash -n scripts/deploy-ec2.sh` 및 `python3 scripts/test-ai-policy-deploy.py`. 실제 AWS/Docker 호출을 하지 않는 stub 검사다. [최신 계약](../spec/2026-09-29-tue-pr-012-jev-single-path.md)과 [검증 기록](../testing/ai-policy/2026-09-29-jev-single-path-results.md)을 참조한다.


## 음식 사진과 직접 식단 기록 배포

현재 코드의 사진 분석은 기존 OpenAI 설정을 재사용합니다. S3, 이미지 보관 테이블, CDN, 별도 사진 API 키 또는 새 CDK 리소스를 추가하지 않았습니다. 다음 값은 기존 Parameter Store 경로를 사용합니다.

| Parameter | 타입 | 값/역할 |
| --- | --- | --- |
| `/my-fitness/prod/ai-provider` | String | `openai`: 사진 분석 사용에 필요 |
| `/my-fitness/prod/openai-api-key` | SecureString | 이미지 입력과 Structured Outputs를 지원하는 모델을 호출할 OpenAI 키 |
| `/my-fitness/prod/typesafe-api-key` | SecureString | 일반 텍스트 대화의 JEV 키, 계속 필요 |

`AI_OPENAI_MODEL` 기본값은 `gpt-4o-mini`, `AI_REQUEST_TIMEOUT`은 `30s`입니다. 배포 스크립트에는 이 두 값의 추가 SSM 조회가 없으며 위 기본값을 사용합니다. 변경하려면 환경 파일 생성 계약을 함께 수정해야 합니다. JEV의 ai-policy-model/version 설정은 그대로 유지합니다. 키를 runtime.env나 로그에서 출력하지 않습니다. main 푸시 후 CI 성공 시 자동 배포하며, Parameter Store 값은 배포 전에 등록합니다.

사진의 multipart part는 `image` 한 장이며 JPEG/PNG 5 MiB 이하, 전체 요청 제한 6 MiB, 1600만 픽셀 이하입니다. 프론트엔드는 원본 제한 확인 후 긴 변 최대 1600px로 재인코딩하고, 서버도 metadata를 제거한 JPEG로 정규화합니다. 업로드 임시 파일은 처리 종료 시 정리되며 원본을 앱 DB나 객체 저장소에 보관하지 않습니다. 공급자 자체의 보관 정책까지 삭제를 보장하는 계약은 아닙니다.

진단은 사진 request_kind, 모델·food-photo-v1·결과 상태·오류 코드·지연·토큰 사용량을 확인합니다. 내부 실패 코드는 CONFIGURATION_ERROR / TIMEOUT / TRANSPORT_ERROR / HTTP_ERROR / INVALID_RESPONSE / MODEL_REFUSAL / INCOMPLETE_RESPONSE를 구분하며 공개 응답은 같은 503 계약을 유지합니다. 파일 검증 실패는 대화와 요청 로그를 저장하기 전에 반환합니다. 원본 질문·사진·모델 응답·키를 출력하는 방식으로 확인하지 않습니다.

- 400 `INVALID_FOOD_PHOTO`: 읽을 수 없는 이미지 또는 픽셀 제한 초과.
- 413 `PAYLOAD_TOO_LARGE`: 파일/전체 요청 제한 초과.
- 415 `UNSUPPORTED_MEDIA_TYPE`: JPEG/PNG 외 포맷 또는 실제 포맷과 MIME 불일치.
- 200 NOT_FOOD/UNCERTAIN: 정상 판별 결과이며 음식 후보·기록 액션 없음.
- 503 `AI_PROVIDER_UNAVAILABLE`: provider/키 미설정, timeout·통신·응답 오류·refusal·미완성. 자동 재시도·provider fallback 없음.

### V11 schema와 복구

`V11__direct_meals_and_food_photos.sql`은 현재 사용 중인 식단 데이터가 없다는 승인에 따라 foods와 이전 meal_foods를 제거하고 직접 입력 구조를 만듭니다. meals/nutrition_goals와 과거 AI 메시지·정책 로그를 보존하고, 기존 V1~V10을 수정하지 않습니다. 이 변경 파일은 배포 시 Flyway가 적용합니다. 코드 작업에서는 운영 DB를 초기화하지 않았습니다.

이전 이미지의 카탈로그/회분 API는 새 schema와 호환되지 않습니다. 기존 배포 스크립트의 previous image rollback만으로 V11을 되돌릴 수 없으며 이전 이미지의 Flyway 검증도 실패할 수 있습니다. 배포 전 복구 가능한 DB snapshot을 확보하고, 문제가 있으면 새 schema를 지원하는 수정 이미지로 복구하거나 DB snapshot과 호환 이미지의 복원을 함께 수행합니다. 운영 중인 데이터가 생긴 뒤 이 drop migration을 재사용하지 않습니다.

로컬 검증은 전체 Java/컨벤션/아키텍처 검사, 프론트엔드 Node/lint/export/PWA, 실제 Spring AI SDK의 로컬 HTTP 계약, PostgreSQL 17의 신규/후속 DDL·과거 AI 데이터 보존과 운영 Docker JRE의 실제 Flyway V1~V11 적용·기동을 포함합니다. 실제 OpenAI 사진 호출·영양 정확도·휴대폰의 카메라/PWA 상호작용·운영 RDS와 원격 CI/CD는 별도 검증 대상입니다.
