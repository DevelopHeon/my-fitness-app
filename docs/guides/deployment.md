# 배포와 복구

설정 이름·인프라 값은 [Infrastructure](../reference/infrastructure.md)를 따른다.
아래는 실행 절차이며 현재 운영에 적용됐다는 결과 보고가 아니다.

## 애플리케이션 배포

1. 변경 코드와 검사를 확인한다. [Testing](testing.md)의 일반 검증을 통과한 commit을 대상으로 한다.
2. AWS GitHub OIDC role·ECR·SSM managed node와 필수 Parameter Store를 준비한다.
   GitHub repository variable AWS_DEPLOY_ROLE_ARN과 [설정 계약](../reference/infrastructure.md#설정과-비밀값)을 확인한다.
3. main CI가 성공하면 Deploy App이 실행된다. 필요하면 GitHub Actions에서 Deploy App을 수동 실행한다.
   수동 workflow_dispatch는 선택 ref의 SHA를 사용하므로 검사한 commit인지 확인한다.
4. ECR SHA image → SSM Run Command → 내부 health → public health와 current-image를 확인한다.
   조회 명령은 [Troubleshooting](troubleshooting.md)에 둔다.

이미지는 GitHub runner에서 ARM64로 빌드하며 EC2에서 앱을 빌드하지 않는다.
기존 SHA tag는 재사용한다. health 실패 시 이전 이미지·runtime.env·RAM/swap 한도를 함께 복원한다.
설정 archive는 같은 SHA의 `/opt/my-fitness/monitoring/releases/<sha>`로 전달하고 checksum을 확인한다.
workflow는 `scripts/deploy-ssm.sh <sha>`를 실행한다. 이 스크립트가 `git archive`로 해당 SHA의
운영 파일만 묶고 크기를 확인한 뒤 SSM에 전달·결과를 확인한다. 작업 디렉터리의 미커밋 변경·비밀값·
local 설정·검증 소스는 포함하지 않는다. 조회 권한 오류를 실행 대기로 처리하지 않는다.
secret·자원·모니터링 설정·앱 parameter/image preflight 뒤 Caddy가 공개 지표를 차단한다.
이전 모니터링을 멈춘 뒤 앱을 교체하고, 앱 health 성공 후 모니터링을 시작한다.
모니터링 실패는 workflow 실패로 표시하되 정상 앱을 다시 교체하지 않는다.
모델/정책 version 문자열만 바꿔 코드의 판정 규칙을 되돌릴 수는 없다. legacy/shadow 시점의 이미지 복원은 정책 경로도 바꾼다.
로컬 stub 통과·원격 CI 통과·실공급자 검증·AWS 배포 확인을 각각 기록한다.

## 인프라 변경

저장소 루트에서 시작한다. Node 22와 AWS/CDK 인증이 필요하다.

```bash
cd infra
npm ci
npm run build
npm test -- --runInBand
npx cdk synth
npx cdk diff
```

위 검사 뒤 diff를 검토하고 필요한 StackName을 선택한다.

```bash
npx cdk deploy <StackName> --exclusively
```

`<StackName>`은 실제 stack 이름으로 바꾸는 자리다. EC2 UserData·네트워크 변경은 replacement 여부를 확인한다.
Route 53 record와 Caddy는 별도 관리한다. EC2에 [setup-caddy.sh](../../scripts/setup-caddy.sh)를 배치한 뒤 그 위치에서 실행한다.

```bash
sudo bash setup-caddy.sh dev-heon.com
```

## 음식 사진과 직접 식단 기록 배포

사진 분석은 기존 ai-provider=openai와 openai-api-key를 사용한다. 일반 대화의 typesafe-api-key도 유지한다.
별도 사진 키·S3·CDN·CDK stack은 없다. 파일·응답 제한은 [AI Reference](../reference/ai.md#음식-사진-분석)를 따른다.
AI_OPENAI_MODEL·AI_REQUEST_TIMEOUT에는 추가 SSM 조회가 없으므로 기본값 외 변경은 환경 파일 계약도 수정한다.

### V11 schema와 복구

V11__direct_meals_and_food_photos.sql은 사용 중인 식단 데이터가 없다는 승인 당시의 migration이다.
foods·이전 meal_foods를 제거하고 직접 입력 항목으로 교체한다. meals/nutrition_goals와 과거 AI 로그·V1~V10은 보존한다.
운영 데이터가 생긴 뒤 초기화를 위해 재사용하지 않는다. 이 문서 정리에서는 DB나 migration을 실행하지 않는다.

배포 전 복구 가능한 DB snapshot을 확보한다. 이전 카탈로그 API의 이미지는 새 schema와 호환되지 않으므로
previous image rollback만으로 V11을 되돌릴 수 없다. 새 schema를 지원하는 수정 이미지로 복구하거나 DB snapshot과 호환 이미지 복원을 함께 수행한다.
로컬 PostgreSQL/HTTP 대역 통과가 운영 RDS 적용·실제 음식 인식·모바일 PWA 확인을 대신하지 않는다.

## 모니터링 적용 범위

[Monitoring](monitoring.md#운영-단일-ec2)의 신규 SecureString 3개와 app-base-url을 먼저 준비한다.
CI의 격리된 Compose 검사는 GitHub runner에서만 실행하며, Deploy App의 SSM 단계가 운영 스택을 설치한다.
main 자동 배포와 수동 workflow_dispatch는 모두 이 release 경로를 사용한다.
CI는 `scripts/verify-monitoring.sh`로 local/prod를 함께 검사하며 테스트용 프로젝트만 정리한다.
기존 EC2도 SSM에서 setup-ec2-monitoring.sh가 실행되므로 UserData 업데이트만 기다리지 않는다.
처음에는 micro 시험이며 실제 자원 인수와 Slack 확인은 [모니터링 인수 기준](monitoring.md#운영-인수)을 따른다.

앱은 성공했지만 모니터링만 실패하면 Actions/SSM 상태와 health를 각각 확인한다.
이전 설정 release가 있으면 volume을 보존한 복원을 시도한다. 복원 로그만으로 정상 수집이라 판단하지 않는다.
메모리가 부족하면 현재 release의 모니터링을 먼저 stop하고 앱을 유지한다. prod에서 `down -v`하지 않는다.
축소된 TSDB retention으로 이미 삭제된 데이터는 설정 복원으로 되살아나지 않는다.
