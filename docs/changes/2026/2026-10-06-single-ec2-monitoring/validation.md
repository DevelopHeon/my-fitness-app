# 단일 EC2 모니터링 구현 검증

검증일: 2026-10-06. 기준은 `61a71e9` 이후 이번 main 변경이다.
환경은 macOS ARM64, Java 21, Node 22, Docker Desktop Linux VM이다.
AWS 운영에 직접 접속·변경하지 않았고 실제 JEV/OpenAI·Slack을 호출하지 않았다.

## 구현한 계약

- 기존 t4g.micro·20GiB를 유지한다. RAM/swap·JVM·로그 한도를 명시하고 운영 수집은 60초·3일/1GB로 분리했다.
- 기존 swap을 재포맷하지 않고 활성화·권한·fstab을 확인한다. Compose ARM64 고정 바이너리는 checksum 확인 후 설치한다.
- `prod,monitoring`과 전용 Basic 암호를 전달한다. Caddy는 올바른 Basic 값이 있어도 health 외 공개 actuator를 차단한다.
- node_exporter·Prometheus self job과 Host 섹션을 추가했다. 대시보드 전체 사본을 Git에 추가하지 않고 운영 refresh만 전달 시 변환한다.
- Prometheus 판정과 Alertmanager의 Slack 그룹·해제·silence 책임을 분리했다. credential volume도 서비스마다 분리했다.
- 검증 SHA archive·checksum·preflight를 사용하며 앱의 이전 환경/자원을 복구한다. 모니터링 실패는 정상 앱을 다시 배포하지 않는다.
- 기존 Checkstyle·ArchUnit·Modulith 규칙과 정책/생성·transaction·DB 계약은 완화하지 않았다.

## 재현한 실패와 보완

작은 heap의 입력 계약을 확인하기 위해 8000×4000 RGBA PNG를 생성했다. 파일 크기는 124,526 bytes이며
5MiB·3,200만 픽셀 제한 안에 있다. 256MiB heap에서 64MiB를 예약하고 두 장을 동시에 전처리하자
PNG reader의 `OutOfMemoryError`가 `IIOException`에 감싸져 기존 INVALID 사진 오류가 됐다.

같은 조건을 child JVM으로 고정한 회귀 테스트를 추가해 실패를 확인했다. 원본 치수·형식 검증 뒤
표준 ImageReadParam source subsampling을 사용하고 기존 1600 edge JPEG 정규화를 유지했다.
변경 후 두 장이 모두 1600×800으로 처리됐다. 한계 초과 원본은 sampling 전에 거절한다.
이 시험은 전처리 component의 heap 계약이며 전체 앱 RSS·모델 인식 품질 시험이 아니다.

공백 암호는 앱 startup에서 실패할 수 있으므로 배포 대역에 실패 조건을 추가했다.
기존 코드의 잘못된 성공을 확인한 뒤 secret 준비와 앱 preflight에서 공백 포함 값을 거절했다.
모니터링 stop은 dashboard 생성보다 먼저 처리해 디스크 부족 중에도 중단을 시도할 수 있게 했다.

## 실행 명령과 결과

| 명령 | 결과와 범위 |
| --- | --- |
| Java 21 `./gradlew build --no-daemon` | backend 236건, frontend Node 20건 통과. Checkstyle, ArchUnit, Modulith·convention 포함 |
| `python3 scripts/test-ai-policy-deploy.py` | 10건 통과. 필수 값 실패 시 앱 보존, preflight, 환경/자원 rollback, 기존 swap 멱등 준비, release 순서 |
| `bash scripts/verify-monitoring.sh` | 운영 config·8개 규칙·기존 6개 fixture의 15/60초 평가·host pending/firing/recovery·route 통과 |
| 같은 native helper의 credential 검사 | Prometheus/Grafana/Alertmanager UID 읽기와 다른 서비스 secret 접근 거절 통과 |
| 같은 helper의 Caddy/로컬 receiver 검사 | health 200, 공개 actuator 404, 2개 경보 그룹 1회, 해제 알림, silence 억제 통과 |
| `bash -n` 관련 setup/deploy/verify shell | 구문 통과 |
| `cd frontend && npm run lint` | 통과 |
| `cd infra && npm run build && npm test -- --runInBand` | TypeScript build·CDK 4건 통과 |
| `cd infra && npx cdk synth --region ap-northeast-2 --no-lookups --quiet` | synth 통과. AWS deploy/diff는 실행하지 않음 |
| workflow YAML·SSM archive 로컬 round trip | JSON 28,592 bytes, shell 구문·checksum·압축 해제 통과. macOS에서는 shasum으로 checksum 검사 |

SSM parameter JSON 크기는 이 검증 시점 수치다. 이후 설정 증가에 대비해 archive base64 48,000자 제한을 둔다.
실제 SSM 전달·IAM·도구 설치·외부 provider 권한을 이 로컬 round trip으로 입증하지 않는다.
Linux CI에서 non-root promtool이 fixture 디렉터리를 읽도록 임시 상위 디렉터리에 755를 명시했다.
dummy secret 하위 디렉터리 700·파일 600은 유지하며 각 서비스의 실제 UID도 검사한다.
첫 [원격 CI](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37445348068)는
`/tests/alerts.test.yml: permission denied`로 실패했다. 임시 디렉터리 접근을 보완했고 검증 범위·규칙은 유지했다.
알림 시험의 1초 group_wait에서 간헐적으로 첫 알림만 전송되는 실패를 확인해 운영의 30초를 그대로 검사한다.
silence도 최초 대기보다 긴 35초를 관측한다. group_interval·repeat_interval만 시험 시간을 줄이며
운영의 5분·4시간 값은 amtool 설정 검증으로 확인한다.

## 정리와 미검증

시험용 Compose 프로젝트·receiver/Caddy/Alertmanager 컨테이너와 임시 credential/data volume을 제거했다.
생성한 PNG·child JVM 소스·로그·압축 archive·검증 JSON·테스트 보고서와 CDK synth 산출물을 삭제했다.
회귀 검사용 Java/Node 소스와 promtool fixture는 유지한다. 사용자 실행 중 로컬 앱·volume·기존 secret은 변경하지 않았다.
CDK synth가 생성한 추가 region cache는 이 변경에 포함하지 않는다. 과거 Evaluations 원본도 보존한다.

실제 EC2의 loopback 연결·host root 지표 대조·재부팅·swap·Grafana 로그인·volume 보존·최초 Slack 전송은 미실행이다.
각 컨테이너 한도에서 전체 스택 cold start·WAL 복구·전체 앱 최대 사진/AI 부하·3일 쿼리·24시간 안정성을
측정하지 않았다. 3일 보관 만료·compaction도 별도 관측이 필요하다.
micro 상시 운영을 보장하지 않으며 [운영 인수 기준](../../../guides/monitoring.md#운영-인수) 실패 시 모니터링을 먼저 멈춘다.

설계와 현재 절차: [스펙](spec.md), [Monitoring](../../../guides/monitoring.md),
[Infrastructure](../../../reference/infrastructure.md), [Deployment](../../../guides/deployment.md).

## 후속 정리: 환경 이름·CI·release 전달

기준은 `fcd4087` 이후 main 변경이다. `.local.yml`·`.prod.yml`로 환경 설정을 구분하고
같은 dashboard provider는 하나로 합쳤다. 로컬 helper는 `setup-monitoring.local.sh`,
배포 검사는 `test-deployment.py`로 이름을 바꿨다. project·volume·datasource·dashboard UID는 유지한다.
fixture는 `monitoring/tests/`에 보존하며 두 환경의 실행 mount와 운영 archive에서 제외했다.

CI의 local/prod 반복 검사를 verify-monitoring.sh로 통합했다. 암호 준비도 임시 경로에서 수행해
실행 중인 `.local` 파일을 지우지 않는다. `bash -n 파일1 파일2 ...`가 첫 파일만 검사하던
문제는 파일별 반복으로 수정했다. Deploy App은 deploy-ssm.sh 한 명령으로 release를 전달한다.
helper는 검증 Git object에서 운영 파일 12개만 archive로 만들고 크기·checksum·SSM 성공을 확인한다.
InvocationDoesNotExist만 초기 전파 지연으로 처리하고 조회 권한 오류는 즉시 실패한다.
preflight·복구·900초 실행 계약과 main CI 성공 후 자동 배포는 유지했다.

| 실행 | 결과 |
| --- | --- |
| `for script in scripts/*.sh; do bash -n "$script"; done` | 모든 shell 구문 통과 |
| `python3 scripts/test-deployment.py` | 앱·host·release·local 암호·SSM 전달 검사 16건 통과 |
| `bash scripts/verify-monitoring.sh` | local/prod config, 서비스 UID·secret 격리, 15/60초 경보 fixture, route, Caddy 공개 차단, 그룹·해제·silence 통과 |
| 격리된 local Grafana 기동·HTTP API 확인 | 단일 datasource와 공통 dashboard 자동 등록 통과, 테스트 컨테이너·volume 제거 |
| Java 21 `./gradlew build --no-daemon` | backend 236건·frontend 20건 통과, Checkstyle·ArchUnit·Modulith·convention 포함 |
| 두 workflow YAML parser·문서 링크 검사 | YAML 구문, Markdown 40개·로컬 링크 216개 통과, 평가 원본 8개 변경 없음 |
| AWS 대역 release archive | 미커밋 변경·private 파일·local 설정·tests 제외, commit 파일·checksum 일치. 당시 JSON 21,360 bytes·base64 20,508자 |

새 SSM helper 구현 전 정상 전달 검사의 실패를 확인한 뒤 통과시켰다.
최초 미존재→실행 중→성공 상태 전이, 실패·시간 초과·권한 거절·계속 실행 중,
잘못된/없는 SHA와 48,000자 초과 archive를 검사했다. 실제 AWS CLI·SSM 전달은 대역으로 바꿨다.
실제 EC2 적용·Slack 전송·운영 자원 인수는 이번에도 실행하지 않았다.
이번 검증의 임시 Git fixture·archive·Compose 프로젝트·Grafana volume과 생성한
Gradle test/Checkstyle 보고서·빌드 로그를 제거했다. 테스트 소스와 과거 평가 결과는 유지했다.
한국어 commit만 작성하고 push하지 않는다. 운영 준비와 이후 push는 사용자가 수행한다.

## 최초 운영 배포의 credential mount 실패 수정

`4a7e7ea`의 [Deploy App 실패 로그](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37475880503)에서
secret 준비와 모든 이미지 pull은 완료됐지만 Prometheus preflight의 container 생성이 실패했다.
빈 credential volume을 읽기 전용으로 mount한 상태에서 `/credentials/metrics_password`에
추가 파일 mount를 만들려다 `read-only file system`이 발생했다. 앱 교체 전 단계다.
기존 native 검사는 secret-init을 먼저 실행해 mount 지점이 이미 존재하는 상태만 검사했다.

실제 deploy-monitoring.sh의 preflight를 빈 volume에서 실행해 동일 실패를 재현했다.
수정 후 서비스별 임시 암호 디렉터리를 통째로 읽기 전용 mount하고 EXIT에서 삭제한다.
호스트 암호 권한과 운영 credential volume을 유지하며 preflight에서 secret-init을 실행하지 않는다.

- `bash scripts/verify-monitoring.sh`: 최초 빈 volume·기존 암호가 있는 volume 모두 preflight 통과.
  기존 암호 미변경·임시 암호 정리와 기존 config·규칙·UID·route·Caddy·알림 검사도 통과했다.
  실제 스크립트를 실행하되 host 경로·project 이름·memory 입력만 격리용 값으로 바꿨다.
- `python3 scripts/test-deployment.py`: 16건 통과.
- Java 21 `./gradlew build --no-daemon`: backend 236건·frontend 20건, Checkstyle·ArchUnit·Modulith·convention 통과.
- 파일별 `bash -n`, `git diff --check`, 문서 링크 검사 통과. 생성한 로그·보고서·임시 리소스는 정리했다.

GitHub 실패 로그 조회와 로컬 재현·검증을 수행했다. 실제 EC2 재배포·Slack 전송·운영 자원 인수는
실행하지 않았다. 한국어 commit만 작성하며 수정 SHA의 push·배포는 사용자가 수행한다.


## Push 이후 운영 확인과 복구 조건 보완

사용자가 push를 승인한 뒤 `749b361`을 main에 전달했다.
[CI](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37477874479)는 통과했다.
[Deploy App](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37478772735)에서는
최초 credential mount 문제가 해결됐고 앱 배포도 성공했다. 이후 네 모니터링 컨테이너가
생성됐지만 Grafana health 준비 제한에 걸려 전체 모니터링은 중지됐다.

GNU `readlink -f`는 마지막 경로가 없어도 정상화된 경로를 반환할 수 있다.
최초 배포에서는 이 값만으로 이전 release가 있다고 판단해 없는 Compose 파일로 복구를 시도했다.
이전 Compose 파일의 실제 존재 여부를 확인하도록 수정했다. 실제 배포 스크립트를 대역 명령으로
실행해 최초 실패의 불필요한 복구와 정상 release 복구를 구분하고,
네 health 검사 후에만 current를 갱신하는 계약도 검사한다.

동일 EC2에서 앱을 유지하고 Grafana만 기존 128MiB 한도로 기동했다.
90회, 이어 초기 DB 변환 이후 150회(2초 간격)의 준비 검사를 수행했지만 두 번 모두 실패해 중지했다.
검사 중 Grafana는 약 127MiB를 사용했고 호스트 MemAvailable은 147~157MiB였다.
Docker OOMKilled는 false였고 커널 OOM 기록도 발견되지 않았다. CPUCreditBalance는 약 288로 충분했다.
메모리 압박이 의심되지만 이 관측만으로 내부 정지 위치나 원인을 확정하지 않는다.
준비 시간만 늘리는 수정은 채택하지 않았다. 인스턴스 크기·요금·메모리 한도도 변경하지 않았다.

[Grafana 공식 설치 문서](https://grafana.com/docs/grafana/latest/setup-grafana/installation/)는
최소 권장 메모리를 512MB로 제시한다. 기존 128MiB 설정은 운영 적합성이 확인되지 않았으며,
이 구성의 CI 설정 검사 통과는 실제 운영 기동·안정성을 의미하지 않는다.
Grafana 자원 구성을 다시 정한 뒤 전체 스택 인수 검증이 필요하다.
현재 앱 health는 UP이며 모니터링은 중지 상태다. 실제 Slack 전송·대시보드 로그인·24시간 안정성은 미검증이다.

- Java 21 `./gradlew build --no-daemon`: backend 236건, frontend 20건,
  Checkstyle·ArchUnit·Modulith·convention 통과.
- `bash scripts/verify-monitoring.sh`: 최초/기존 volume preflight, 암호 격리,
  설정·규칙·route·Caddy·알림 그룹/해제/silence 검사 통과.
- `python3 scripts/test-deployment.py`: 기존 16건과 모니터링 release 회귀 3건, 총 19건 통과.
- 파일별 `bash -n`, `git diff --check`, 문서 40개·로컬 링크 217개 검사 통과.
  생성한 테스트 보고서·로그와 격리된 임시 리소스는 정리했다. 평가 원본 8개는 변경하지 않았다.

위 로컬 대역·격리 검사 결과와 실제 EC2에서 실패한 Grafana 기동 결과를 구분한다.
후속 복구 조건 수정도 한국어 commit 후 main에 push한다.


## 후속 작업: small 증설과 실제 모니터링 기동 확인

사용자가 단일 t4g.small 증설과 실제 운영 배포·모니터링 확인을 승인했다.
backend 236건·frontend 20건과 Checkstyle·ArchUnit·Modulith·convention,
배포 대역 19건, CDK build·4건, native monitoring 검사 모두 통과했다.
CDK의 small 및 고정 AMI 기대를 먼저 실패시킨 후 구현해 통과를 확인했다.
운영 기동·지표 확인 결과는 아래와 같다.

최초 small 변경 커밋의 CI는 기능·컨벤션·모니터링·CDK 테스트까지 통과했으나,
마지막 CDK synth가 서울 AMI를 us-east-1에서 조회하려다 실패했다.
CDK CLI가 `CDK_DEFAULT_REGION`을 자체 리전 결정 결과로 덮어쓰므로,
CI synth에 `--region ap-northeast-2`를 명시하고 불필요한 환경변수 설정을 제거했다.
검사 범위나 AMI 고정 기준은 유지했다. 자격 증명이 없는 CI 조건에서도 synth가 통과했다.

### 실제 AWS 변경과 배포

2026-10-07 KST 기준 `MyFitnessApplication` CloudFormation change set을 검토한 뒤 실행해
UPDATE_COMPLETE를 확인했다. 기존 EC2 `i-0722d8cac2c3a9c21`을 t4g.small로 증설했으며,
AMI `ami-093fb7e528aec34e5`와 루트 EBS `vol-0501efff42c184c6f`·20GiB 용량·Elastic IP는 유지했다.
최신 AMI의 자동 조회를 기존 AMI 고정으로 변경해 이번 증설이 AMI 교체로 이어지지 않게 했다.
별도 인스턴스·볼륨·공인 IP·RDS 변경은 없다.

운영 설정 변경은 `fce6457`, CI 리전 수정은 `1025e3f`에 포함했다.
`1025e3f12e6d8839ff3883236a257f1d70099c95`의
[CI](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37486042944)와
[Deploy App](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37486986645)는 모두 성공했다.
EC2의 current-image와 monitoring/current가 같은 검증 SHA를 가리키는 것도 확인했다.
앱 JVM·swap·수집 주기·보관 기간은 기존 운영 설정을 유지했다.

### 실제 모니터링 확인

SSM의 읽기 전용 명령으로 00:26 KST에 확인했다. 암호는 호스트 파일에서 읽어
curl 표준 입력의 인증 헤더로 전달했고, 키·암호·요청 원문은 출력하지 않았다.

- 앱 health UP, Prometheus·Alertmanager ready, Grafana DB health OK, node_exporter 응답 정상.
- Prometheus 수집 대상 앱·Prometheus·node_exporter 3개 모두 UP, lastError 없음.
- JVM heap 약 60.4MiB, Hikari 최대 연결 5개, HTTP 요청 counter와 호스트 MemAvailable의 실제 시계열 조회 성공.
- Grafana admin 인증과 Prometheus 데이터소스 health OK.
  운영 대시보드의 48개 패널 설정 조회 및 Grafana 데이터소스 경유 PromQL `up{job="my-fitness"}` 결과 1 확인.
- 비인증 앱 지표 접근 401, 공개 HTTPS health UP 및 공개 지표 접근 404.
- 첫 관측에서 호스트 MemAvailable 776MiB, 루트 여유 9.2GiB, swap 사용 512KiB, swappiness 10.
  Grafana 약 356.5MiB/512MiB, Prometheus 50MiB/256MiB, 앱 363.1MiB/448MiB.
  여섯 컨테이너 모두 OOMKilled=false, restart 0회. 활성 경보 없음.

00:28 KST 재검사에서도 동일한 health·인증·쿼리가 통과했다.
Grafana HTTP 준비는 컨테이너 시작 약 8초 뒤였으며,
`min_over_time(up[3m])`는 세 수집 대상 모두 1이었다.
재관측 MemAvailable은 763MiB, Grafana 약 359.9MiB, Prometheus 52.4MiB,
앱 363.6MiB였고 여섯 컨테이너의 OOM·재시작은 여전히 0회였다.

이 검증은 실제 운영 기동·인증·지표 조회 확인이다. 브라우저 화면의 시각 검수,
실제 Slack 알림 전송, 유료 JEV/OpenAI 호출, 최대·동시 사진 부하, 24시간 안정성,
3일 보관 만료·compaction은 검증하지 않았다. 단일 EC2 장애 시 앱과 알림도 함께 중단되는 구조는 유지된다.

검증 과정에서 만든 로컬 보고서·임시 명령 파일·로그와 CDK 산출물은 정리했다.
테스트 소스·규칙 fixture·평가 원본은 유지했다. 운영 결과 기록만 반영하는 후속 문서 커밋에는
`[skip ci]`를 사용해 동일 운영 설정의 불필요한 재배포를 피한다.
코드·배포 설정 변경 커밋은 일반 CI와 자동 CD를 그대로 거쳤다.

## small 기동 이후 Grafana 흰 화면과 응답 정지

2026-10-07 KST 사용자가 SSM 접속 화면의 흰 화면·계속 대기 증상을 보고했다.
로컬 13001 터널은 연결 중이었지만 health·login 요청이 10초 내 응답하지 않았다.
EC2 내부 3001 health도 시간 초과였으며 앱·Prometheus는 즉시 200을 반환했다.
앞선 기동 직후 API 확인만으로 이 후속 문제를 발견하지 못했다.

Grafana는 Docker 표시 약 507.5MiB/512MiB였고 cgroup memory.current는 536,805,376 bytes였다.
memory.events의 max는 269,251회, oom·oom_kill은 0이었다.
메모리 회수 full 압력 avg10은 6.99였고 로그에 context deadline exceeded와 SQLITE_BUSY가 확인됐다.
즉 OOMKilled=false·restart=0만으로 정상 응답을 보장할 수 없었다.

컨테이너 한도·인스턴스 크기·DB 설정은 유지한 채 Grafana의 Go 메모리 관리 기준으로
`GOMEMLIMIT=384MiB`를 임시 override에 넣어 Grafana만 재생성했다. volume과 암호는 보존했다.
health가 복구됐고 기존 SSM 터널 health 약 30ms·login 약 106ms 응답 및 실제 브라우저의
로그인 폼 렌더링을 확인했다. 다만 이후에도 cgroup 메모리가 상한에 가까워,
Go heap 할당 약 225.6MiB·Go sys 약 370.1MiB와 별도 plugin 프로세스·전체 RSS를 대조했다.
Go 외 메모리와 캐시 여유를 더 확보하도록 최종 운영 Compose에는 `GOMEMLIMIT=320MiB`를 반영했다.
이 값은 RSS를 강제 제한하지 않는 soft limit이며 [Go GC 문서](https://go.dev/doc/gc-guide#Memory_limit)에 따라
컨테이너 상한보다 여유를 두었다. 재시작 자체와 메모리 기준의 효과를 구분하기 위해 이후 응답·회수 압력을 계속 관측한다.

- Java 21 `./gradlew build --no-daemon`: backend 236건·frontend 20건,
  Checkstyle·ArchUnit·Modulith·convention 통과.
- `bash scripts/verify-monitoring.sh`: 설정·규칙·credential 격리·Caddy·알림 그룹/해제/silence 통과.
- `python3 scripts/test-deployment.py`: 19건 통과.
- `git diff --check`·문서 40개/로컬 링크 217개 검사 통과. 평가 원본 8개는 변경하지 않았다.

영구 설정의 CI/CD와 후속 운영 관측 결과는 아래에 기록한다.

320MiB 임시 적용 후 인증 사용자 조회·login을 20회 반복해 모두 통과했다.
frontend 설정·운영 대시보드 48개 패널 설정·Grafana 경유 PromQL 조회도 통과했다.
첫 관측 Grafana 328.8MiB/512MiB, 호스트 MemAvailable 644MiB였다.
최종 설정으로 native monitoring 검사를 다시 실행해 통과했다.
기동 직후 결과와 시간 경과 후 관측을 구분하며, 초기 재시작만으로 장기 해결을 확정하지 않는다.

영구 설정 커밋 `002828899ebcf557708de12a59549c709eb2bacd`의
[CI](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37492039181)와
[Deploy App](https://github.com/DevelopHeon/my-fitness-app/actions/runs/37492937161)는 성공했다.
EC2 current-image와 monitoring/current가 해당 SHA이며,
실행 중 Grafana의 `GOMEMLIMIT=320MiB`와 컨테이너 512MiB 한도를 확인했다.
임시 override에 의존하지 않는 저장소 배포 후에도 실제 브라우저 로그인 폼이 렌더링됐다.

### 시간 경과 후 운영 검증

01:09:16~01:29:17 KST, 20분 1초 동안 1분 간격으로 21회 관측했다.
이는 이전 첫 오류가 나타난 기동 약 18분 시점을 넘긴 검사다.

- health·login·인증 사용자 API 각각 21회, 총 63회 모두 HTTP 200.
  EC2 내부 최대 응답은 각각 8.0ms·6.8ms·21.2ms였다. SSM 터널을 포함한 응답 시간과 구분한다.
- Grafana 경유 PromQL 21회 모두 성공했고 앱·Prometheus·node_exporter 3개 모두 UP이었다.
- 관측 중 최소 호스트 MemAvailable 639.4MiB, OOM·OOM kill 0회.
  마지막 메모리 회수 full 압력 avg10·avg60·avg300은 모두 0.00이었다.
- 종료 전 인증·login 20회 반복과 frontend 설정·48개 패널 설정·PromQL 조회도 다시 통과했다.
  새 Grafana 컨테이너의 오류 로그·SQLITE_BUSY·settings 오류 모두 0건,
  여섯 컨테이너의 OOMKilled=false·restart=0을 확인했다.
  그 시점 Grafana 276.6MiB/512MiB, 호스트 MemAvailable 779MiB, 앱 health UP이었다.
- 종료 시 기존 13001 터널을 통한 브라우저 새로고침에서도 실제 로그인 폼이 표시됐다.

현재 증상은 위 검사에서 재현되지 않았다. 초기 재시작 효과와 메모리 기준의 영향을
완전히 분리한 대조 실험이나 24시간 안정성·전체 동시 부하 검증은 아니다.
Grafana의 전체 RSS·cgroup 사용량은 Go soft limit보다 높을 수 있고, 파일 캐시도 포함하므로
한 숫자만으로 응답 정지나 안정성을 판단하지 않는다. 실제 Slack·JEV/OpenAI 호출은 실행하지 않았다.
생성한 로컬 보고서·임시 로그/명령 파일·EC2 샘플 로그는 정리했다. volume·과거 운영 로그·평가 원본은 보존했다.
