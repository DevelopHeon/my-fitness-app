# Prometheus·Grafana와 Slack 알림

Spring Boot 지표를 Prometheus가 수집하고 Grafana가 시각화합니다. 로컬은 15초·30일/2GB,
운영 구성은 60초·3일/1GB이며 Alertmanager가 Slack 알림을 전달합니다.

[단일 EC2 계획](../changes/2026/2026-10-06-single-ec2-monitoring/spec.md)을 구현했으며,
로컬 검증과 실제 AWS 배포·24시간 자원 인수는 구분합니다. 아래 실행은 기존 로컬 절차입니다.

## 설정 파일의 역할

| 역할 | 파일 |
| --- | --- |
| 환경별 실행 | `monitoring/docker-compose.local.yml`, `monitoring/docker-compose.prod.yml` |
| 수집 주소·주기 | `monitoring/prometheus/prometheus.local.yml`, `prometheus.prod.yml` |
| Grafana datasource 주소 | `monitoring/grafana/provisioning/datasources/prometheus.local.yml`, `prometheus.prod.yml` |
| 공통 dashboard provider·화면 | `monitoring/grafana/provisioning/dashboards/dashboards.yml`, `monitoring/grafana/dashboards/my-fitness.json` |
| 공통 경보 규칙 | `monitoring/prometheus/alerts.yml` |
| 운영 Slack 경로 | `monitoring/alertmanager/alertmanager.prod.yml` |
| 검증 전용 fixture·receiver | `monitoring/tests/` |

각 Compose는 해당 환경의 datasource 한 파일만 마운트합니다. 컨테이너 내부 설정 이름은
`prometheus.yml`로 유지합니다. 테스트 소스·local 설정은 운영 mount와 SSM 배포 archive에 포함하지 않습니다.
기존 `monitoring/docker-compose.yml`과 `scripts/setup-local-monitoring.sh`는 각각
`monitoring/docker-compose.local.yml`과 `scripts/setup-monitoring.local.sh`로 이름을 바꿨습니다.
Compose project·volume 이름은 유지하므로 기존 로컬 데이터와 로그인 암호는 보존됩니다.
경로 변경을 적용하려면 새 Compose 파일로 `up -d`를 실행해 mount를 갱신합니다.
이전 파일의 디렉터리 mount를 사용하는 컨테이너에 단순 `restart`만 수행하지 않습니다.

## 실행

Java 21, Node.js, Docker Compose가 필요합니다. repository root에서 실행합니다.

```bash
bash scripts/setup-monitoring.local.sh
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=monitoring ./gradlew bootRun
```

다른 터미널에서 실행합니다.

```bash
docker compose -f monitoring/docker-compose.local.yml up -d
```

이미 사용 중인 5432 포트·PostgreSQL 컨테이너가 있다면 기존 환경을 지우지 말고 앱의 `DB_URL`을 확인하거나 별도 개발 DB의 포트를 지정하세요. `.env`는 Spring Boot가 자동으로 읽지 않습니다. 기존 AI 환경변수 주입 방식은 유지하며 AI 키를 켜지 않아도 HTTP·JVM·DB 지표를 학습할 수 있습니다.

- Grafana: <http://localhost:3001>, 사용자 `admin`.
- Prometheus: <http://localhost:9090>, Targets·Graph·Alerts 화면.
- 지표: `http://localhost:8080/actuator/prometheus`, HTTP Basic 사용자 `prometheus`.

비밀번호는 `.local/monitoring/secrets/`의 `grafana_admin_password`, `app.monitoring.password` 파일에 생성됩니다. 직접 로컬에서 확인하되 터미널 기록·문서·Git에 복사하지 마세요. helper는 기존 값을 보존하며 디렉터리 700·파일 600 권한을 적용합니다. 앱은 config tree로 읽습니다. `secret-init`이 원본을 읽어 컨테이너 전용 볼륨에 600 권한으로 복사하고 Prometheus(UID 65534)·Grafana(UID 472)에 각각 소유권을 부여합니다. 두 서버는 해당 볼륨을 읽기 전용으로 마운트합니다. Linux 파일 바인드의 UID 불일치를 처리하기 위한 일회성 초기화이며 호스트 비밀값 권한을 낮추지 않습니다.

`monitoring` 프로필은 비밀번호가 없으면 시작하지 않습니다. 지표 체인은 업무 로그인 세션을 신뢰하지 않으며, 지표 계정으로 업무 API에 로그인할 수도 없습니다. 기본·prod 단독에는 Prometheus endpoint를 노출하지 않습니다. 운영 release만 `prod,monitoring`과 전용 암호를 함께 전달합니다.

Prometheus·Grafana 웹 포트는 로컬 loopback에만 공개합니다. 앱의 기존 8080 포트와 기본 서비스 접근 정책은 유지합니다. Docker는 `host.docker.internal:8080`으로 호스트 앱에 접근하고 Grafana datasource는 Docker 내부의 `http://prometheus:9090`을 사용합니다.

## 화면에서 확인할 것

대시보드는 **My Fitness · Local Observability**이며 datasource와 화면 구성이 시작 시 자동 등록됩니다. 시간대는 한국, 기본 범위는 최근 30분입니다. 파일 기반 설정을 기준으로 관리하며 UI에서 수정한 내용을 자동으로 Git에 저장하지 않습니다. 섹션 제목을 눌러 접거나 펼칠 수 있습니다. 별도 패널 플러그인 없이 Grafana 기본 패널을 사용합니다.

| 섹션 | 먼저 볼 것 | 표현과 다음 확인 |
| --- | --- | --- |
| 요약 | 수집 UP, 요청 수, 5xx 건수·비율, 평균 시간, 활성 알림 | 작은 Stat 카드로 확인하고 아래 상세로 이동 |
| API | 요청량·오류·지연의 변화 | 추이 그래프 → 경로별 평균·표본 표 → 현재 알림 표 |
| AI | 정책 결정·주제와 답변·사진 결과 | 건수 가로 막대, 누적 평균 Stat → 오류 막대 → 10분 지연 추이·p95 |
| DB | 풀 사용률과 대기 | Gauge·Stat → 사용·대기 추이와 획득 평균 시간 |
| JVM | heap, CPU와 GC | heap Gauge → heap·CPU·GC·스레드 추이. 가동 시간·GC 건수는 Stat |
| Host | 가용 메모리·루트 디스크·Prometheus RSS·시계열 수 | Stat → 메모리·swap 추이. 로컬에 node/self job이 없으면 N/A |

수집 상태부터 확인한 뒤 API 지연이 증가한 시각을 AI·DB·JVM 그래프에서 비교합니다. DB 대기가 생겼다면 풀 사용 중/최대와 획득 시간을 함께 확인합니다. Heap 증가가 보이면 GC와 API 지연을 함께 봅니다. 같은 시각에 변한 지표는 원인 후보이며 인과관계를 입증하지는 않습니다. 상세 원인은 앱 로그에서 확인합니다.

패널 제목의 **현재**는 조회 종료 시점의 마지막 관측값, **선택 구간**은 우측 시간 범위의 증가 추정, **10분**은 각 시점 직전 10분의 집계, **앱 누적**은 실행 중인 앱의 전체 기록입니다. 비교용 API·AI·DB 평균 추이는 모두 10분이며 GC 시간/초는 JVM 진단용 5분입니다. API 5xx·CPU·heap·풀 사용률은 0〜1 쿼리 값을 0〜100%로 표시합니다. 5xx 추이 축도 이 범위로 고정합니다.

AI를 사용하지 않으면 해당 지표는 N/A입니다. 사용량이 없거나 p95의 최근 10분 표본이 20건 미만인 경우에도 N/A가 정상입니다. HTTP 지표는 API 경로를 대상으로 집계하며 정적 파일·지표 수집 요청은 제외합니다. 인증 필터에서 끝난 요청의 URI가 `UNKNOWN`으로 기록될 수 있으므로 패널의 API 집계가 모든 보안 거절 요청을 포함하는 것은 아닙니다.

AI의 분포·오류·토큰·누적 평균 패널은 **조회 종료 시점에 실행 중인 앱의 누적값**을 instant query로 표시합니다. 앱 재시작 시 초기화되며 선택 시간 구간의 요청 수를 뜻하지 않습니다. 막대 길이는 건수·토큰의 상대 크기이며 비율이나 정확도가 아닙니다. 새 label의 첫 요청부터 counter가 1로 수집되면 increase/rate는 처음 값 이전의 증가를 알 수 없어 0을 반환할 수 있습니다. 소량 학습에서도 첫 요청과 평균 시간을 확인하도록 누적 count와 sum/count를 사용합니다. 기록이 없으면 N/A이며, 10분 AI 평균 추이·p95와 알림은 시간 구간 기반 검사를 유지합니다. 첫 호출만 관측된 상태에서는 누적 평균 카드에 값이 있어도 10분 추이는 비어 있을 수 있습니다.

경로별 API 표는 최근 10분 평균 시간 내림차순입니다. 표본 열은 scrape 기반 요청 증가 추정값이며 소수점·첫 관측 누락이 가능합니다. 활성 알림 카드·표는 기존 6개와 host 2개 규칙의 pending/firing을 보여줍니다. 빈 표와 수집 실패를 구분하려면 요약의 UP 상태도 확인하세요.

대시보드 JSON 변경은 파일 provisioning으로 반영됩니다. 잠시 기다린 뒤 브라우저를 새로고침하세요. 앱과 Prometheus를 재시작할 필요가 없으며, 반영되지 않으면 Grafana만 재시작합니다.

```bash
docker compose -f monitoring/docker-compose.local.yml restart grafana
```

정책 `BLOCK/SAFE_REDIRECT/CLARIFY`, 사진 `NOT_FOOD/UNCERTAIN`은 정상 결정입니다. JEV 오류는 `UNAVAILABLE`, 공급자 오류는 `FAILURE`로 구분합니다. 정책 분포로 모델 정확도를 평가하지 않습니다. HTTP 시간에는 정책·생성 시간이 포함되므로 각 시간을 합산하지 않습니다.

토큰 패널은 검증된 응답에서 관측한 input/output만 집계합니다. usage 누락과 Spring AI의 `EmptyUsage`는 미기록이며 실제 0 사용량과 구분합니다. 실패 요청의 과금은 알 수 없습니다. 기존 SDK `gen_ai.*`와 앱 지표를 더하거나 공급자 청구 금액으로 표현하지 않습니다. 모델·버전·세부 판정 근거는 기존 DB의 AI 요청 이력에서 확인합니다. Prometheus가 DB 이력을 반복 조회하지는 않습니다.

## 알림과 진단

로컬은 외부 메시지를 보내지 않습니다. 운영은 [alerts.yml](../../monitoring/prometheus/alerts.yml)을
Prometheus에서 평가하고 Alertmanager가 Slack에 전달합니다. Grafana에 같은 규칙을 복제하지 않습니다.

| 규칙 | 조건 | 확인 순서 |
| --- | --- | --- |
| AppMetricsUnavailable | scrape 실패 2분 | 앱 실행 → monitoring 프로필 → host 연결 → 전용 인증 파일 → timeout |
| ApiServerErrors | 10분 내 5xx 3건 이상·5% 이상, 2분 지속 | endpoint·발생 시각 → 앱 로그 → JVM·풀·AI 상태 |
| PolicyUnavailable | 10분 내 정책 장애 3건 이상·20% 이상, 2분 지속 | 오류 label → 기존 정책 로그 → 설정/키·timeout·통신·응답 검증 |
| AiProviderFailures | 종류별 10분 내 장애 3건 이상·20% 이상, 2분 지속 | chat/photo 종류 → 오류 label → 기존 요청 로그 → 공급자 설정 |
| JvmHeapPressure | 유효한 heap max 대비 85% 초과, 10분 | heap·GC·프로세스 자원·당시 요청 |
| DbPoolWaiting | pending > 0, 2분 | 풀 사용·획득 시간 → 느린 요청·DB 연결 상태 |
| HostMemoryPressure | MemAvailable < 150MiB, 5분 | monitoring stop → 앱 health → swap·컨테이너 RSS |
| HostDiskPressure | root 가용 < 5GiB, 5분 | 이미지·Docker 로그·TSDB 사용량, volume 보존 |

위 임계값은 학습용 초기값이며 운영 SLO가 아닙니다. `up=0`은 연결·인증 문제일 수도 있습니다. No Data를 장애 없음으로 해석하지 마세요. 비율뿐 아니라 최소 건수도 확인합니다. `increase`는 scrape 기반 추정이라 소수점이 나올 수 있고 앱 재시작으로 누적 counter는 초기화됩니다.

Prometheus·Grafana는 로그나 SQL 원인을 자동 수집하지 않습니다. 상세 원인은 기존 앱 로그·AI 요청 DB 이력에서 같은 시점을 확인하세요. 원문 질문·사진·응답·키를 출력하는 방식으로 진단하지 마세요. 로그 중앙 수집·브라우저 오류·tracing·Telegram은 후속 범위입니다.

## 보관·재시작

최대 30일 또는 TSDB 2GB 조건 중 먼저 도달한 조건이 적용됩니다. 2GB에 먼저 도달하면 보관 기간이 짧아집니다. WAL·head·compaction 때문에 실제 디스크 사용은 2GB를 넘을 수 있습니다. Docker volume 사용량도 함께 확인하세요.

```bash
docker compose -f monitoring/docker-compose.local.yml down
docker compose -f monitoring/docker-compose.local.yml up -d
```

위 명령은 named volume을 보존합니다. `down -v`는 저장 지표와 Grafana DB를 지우는 초기화이므로 데이터 삭제를 의도한 경우에만 실행합니다. Grafana 초기 비밀번호 파일을 바꿔도 기존 볼륨의 관리자 암호는 자동으로 바뀌지 않습니다. 기존 관리자 암호 변경은 로그인 후 UI에서 수행합니다.

## 검증

[local/prod 자동 검증](#localprod-자동-검증)은 임시 암호와 별도 Compose 프로젝트를 사용합니다.
실행 중인 로컬 스택이나 `.local` 암호 파일을 초기화하지 않으며, 실제 AI 키·AWS 연결은 필요하지 않습니다.
앱의 인증·지표 계약은 기존 Java 테스트로 검사합니다.

### 실행 후 단계별 확인

1. 무인증 지표 요청이 401인지 확인합니다.

   ```bash
   curl --silent --output /dev/null --write-out '%{http_code}\n' http://localhost:8080/actuator/prometheus
   ```

2. 전용 계정으로 요청하면 200이어야 합니다. 아래 명령의 암호 입력에는 로컬 app.monitoring.password 값을 사용하며 명령행 인자에 암호를 넣지 않습니다.

   ```bash
   curl --silent --show-error --output /dev/null --write-out '%{http_code}\n' --user prometheus http://localhost:8080/actuator/prometheus
   ```

3. Prometheus Targets에서 my-fitness가 UP인지 확인합니다. Graph에서 `up{job="my-fitness"}`가 1인지, `http_server_requests_seconds_count`, `jvm_memory_used_bytes`, `hikaricp_connections_active`에 시계열이 있는지 확인합니다. 기동 직후에는 첫 수집 주기 15초를 기다립니다.
4. Grafana Connections → Data sources에서 Prometheus의 Save & test 성공을 확인합니다. 요약 → API → AI → DB → JVM 섹션, 경로별 표의 시간 단위·표본, 5xx 축 0〜100%, 가로 막대의 항목명·건수와 각 패널의 Query inspector 오류를 확인합니다. 섹션을 접고 펼쳐도 해당 패널들이 같이 이동해야 합니다. AI를 사용하지 않은 상태의 N/A는 정상입니다.
5. 로컬 앱을 종료하면 target이 DOWN, AppMetricsUnavailable이 pending에서 firing으로 바뀌는지 확인합니다. 2분 유지 조건과 수집·평가 주기를 고려하고, 앱 재시작 후 UP과 알림 해제를 확인합니다.
6. 모니터링 Compose를 위의 down/up 명령으로 재시작한 뒤 이전 시간 범위의 지표·관리자 로그인·대시보드가 보존됐는지 확인합니다.

이 절차는 Python smoke 도구 없이 수행할 수 있습니다. 화면을 확인한 결과와 자동 검사 결과를 구분해 기록하고, 실제 JEV/OpenAI를 호출하지 않은 상태를 모델 품질 검증으로 표현하지 않습니다.

Java·아키텍처·전체 빌드는 [Testing](testing.md)을 따른다. 당시 실제 로컬 실행과 미검증 범위는 [검증 기록](../changes/2026/2026-10-04-local-observability/validation.md)에 둔다.

## 운영 단일 EC2

새 값은 `/my-fitness/prod/` 아래 SecureString으로 준비합니다. 콘솔에서 입력하고 Git·명령 인자·로그에 붙이지 않습니다.

| suffix | 값과 용도 |
| --- | --- |
| monitoring-password | 새 임의 강한 암호. 앱과 Prometheus가 동일 값을 사용 |
| grafana-admin-password | 새 임의 강한 암호. Grafana `admin`의 첫 DB 초기화 |
| slack-webhook-url | Slack 앱의 Incoming Webhook URL. 전송 채널은 Slack에서 선택 |

기존 JEV/OpenAI·DB·로그인 parameter와 HTTPS app-base-url은 유지합니다.
필수 값 누락·조회 실패·공백 값은 기존 앱 교체 전에 release를 중단합니다.
Slack URL 형식은 `https://hooks.slack.com/services/...`로 검증합니다. 키의 유효성·채널 권한은 실제 전송 때 확인해야 합니다.
EC2 role의 기존 prod prefix 조회 권한을 재사용하며 별도 KMS key를 쓰면 decrypt 권한이 필요합니다.

main의 자동 Deploy App은 같은 SHA의 설정 archive와 이미지로 설치합니다. 최초 운영 적용 시에는
[Deployment](deployment.md)의 preflight·복구 경로와 아래 인수를 확인합니다. 이 문서의 코드 구성과 실제 AWS 적용은 별개입니다.
Compose plugin이 없으면 ARM64 v5.5.1 바이너리의 고정 SHA-256을 검증한 뒤 설치합니다.
기존 2GiB swap은 재포맷하지 않고 권한·활성화·fstab을 확인합니다. swappiness는 10으로 설정하며 이전 값을 host에 보존합니다.

운영 서버는 Linux host network를 쓰고 각 서버를 loopback에 bind합니다. 추가 SG ingress는 없습니다.
Caddy는 health 외 actuator를 404로 반환합니다. 내부 지표는 Basic 전용 사용자 `prometheus`로 읽습니다.
SSM 터널을 열고 Grafana에 로그인합니다. PC의 로컬 Grafana와 충돌하지 않도록 예시는 local port 13001을 사용합니다.

```bash
aws ssm start-session --region ap-northeast-2 --target <instance-id> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["3001"],"localPortNumber":["13001"]}'
```

<http://localhost:13001>에 `admin`과 grafana-admin-password로 접속합니다.
이미 Grafana DB가 있으면 parameter 변경만으로 로그인 암호가 바뀌지 않습니다. Grafana의 관리자 암호 변경 절차로
회전한 뒤 parameter를 맞춥니다. 비밀 파일을 변경할 때는 release를 다시 적용해 credential volume도 갱신합니다.
production Compose project는 `my-fitness-monitoring-prod`로 고정하며 로컬 volume과 분리합니다.

Slack은 같은 service·environment·alertname을 묶고, 최초 30초 대기·그룹 갱신 5분·재전송 4시간·해제 알림을 사용합니다.
검증되지 않은 인과관계로 inhibition을 추가하지 않습니다. 점검 silence는 종료 시각과 작업 이유를 지정합니다.
Alertmanager UI는 9093 포트의 별도 SSM 터널로 열어 silence를 설정할 수 있습니다.
자동 검사는 로컬 receiver만 사용합니다. 실제 Slack에는 시험 알림의 발생·해제 1회를 명시한 범위에서 확인합니다.
같은 EC2 전체가 죽으면 이 알림 경로도 멈춥니다. EC2 status·CPU credit은 AWS에서 별도로 확인합니다.

자원 문제가 있으면 EC2의 SSM shell에서 모니터링부터 중단합니다. swapoff나 volume 삭제는 하지 않습니다.

```bash
RELEASE=$(readlink -f /opt/my-fitness/monitoring/current)
sudo bash "$RELEASE/scripts/deploy-monitoring.sh" "$RELEASE" stop
curl --fail --silent http://127.0.0.1:8080/actuator/health
```

기동에 실패해 current가 아직 없으면 Actions/SSM에 전달한 SHA의 release 경로를 사용합니다.
다시 시작하기 전 앱과 자원 상태를 확인합니다. 재실행은 같은 release의 `preflight` 뒤 `apply`입니다.
이전 swappiness로 복원하려면 `/opt/my-fitness/monitoring/previous-swappiness`를 확인한 뒤
`/etc/sysctl.d/99-my-fitness-monitoring.conf`의 값을 조정하고 sysctl로 반영합니다.

### 운영 인수

현재 t4g.micro의 시험 한도는 앱 448MiB, Prometheus 160MiB, Grafana 128MiB, Alertmanager 48MiB,
node_exporter 24MiB, Caddy 48MiB입니다. 합계 856MiB는 OS·Docker·SSM을 포함하지 않아 안전 여유를 보장하지 않습니다.
앱은 64/256MiB heap, RAM+swap 합계 512MiB로 제한합니다. 다른 컨테이너는 추가 swap을 허용하지 않습니다.

2026-10-06 실제 EC2에서는 Grafana가 128MiB 한도로 준비되지 않아 모니터링을 중지했습니다.
현재 설정을 운영 기동이 검증된 구성으로 사용하지 마세요. 자원 구성 조정 후 인수를 다시 수행해야 합니다.
관측과 미확정 원인은 [운영 검증 기록](../changes/2026/2026-10-06-single-ec2-monitoring/validation.md#push-이후-운영-확인과-복구-조건-보완)에 둡니다.

기동·WAL 복구·재배포·대표 API·최대 사진·동시 사진·3일 쿼리·재부팅을 구분해 기록합니다.
`free -m`, `df -h`, `vmstat 1`, `swapon --show`, `docker stats --no-stream`과 OOMKilled/restart를 확인합니다.
node_exporter root 지표가 host의 df와 일치하는지 대조합니다. swap 패널은 pages/s이며
byte 환산에는 `getconf PAGESIZE`를 사용합니다. 전체 환경이나 요청 내용을 출력하지 않습니다.

- 24시간 OOM kill·자원 부족 재시작 0회, 기동·부하·재배포 중 MemAvailable 150MiB 이상.
- 루트 가용 5GiB 이상, swap-in/out 합계 1MiB/s가 5분 지속되지 않음.
- 같은 AI 대역·API 부하 비교에서 추가 5xx 0건, p95 악화 20% 이내.
- 인증 수집 UP·SSM 로그인·공개 지표 404·volume 보존·재부팅 후 swap 유지.

한 항목이라도 실패하면 모니터링을 멈추고 t4g.small 또는 배치 분리를 검토합니다.
사진 전처리의 256MiB child JVM 시험은 전체 앱 RSS·동시 AI 부하와 EC2 24시간 인수를 대신하지 않습니다.
3일 보관 만료·compaction은 24시간 시험과 별도로 관측해야 합니다.

### 비용

현재 micro·20GiB EBS·RDS·Elastic IP를 유지하면 모니터링 서버·볼륨의 새 고정비는 없습니다.
EBS는 할당 용량 기준이므로 기존 20GiB 안에서 사용하는 swap·TSDB가 별도 볼륨 청구로 이어지지는 않습니다.
CPU credit·외부 전송량 증가와 실제 세금·할인은 별도로 확인합니다.
서울 Linux On-Demand EC2만 월 730시간 기준 micro는 약 $7.59, small은 $15.18입니다.
small 변경 시 약 $7.59 추가이며 전체 AWS 청구액이 아닙니다.
[AWS 공식 가격 데이터](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/ap-northeast-2/index.json),
[EC2 요금 안내](https://aws.amazon.com/ec2/pricing/on-demand/).

### local/prod 자동 검증

repository root에서 실행합니다. Docker·jq가 필요하며 격리된 프로젝트와 가짜 비밀값을 생성·삭제합니다.
기존 로컬 앱·모니터링 volume이나 실제 Slack·AWS에는 접근하지 않습니다.

```bash
bash scripts/verify-monitoring.sh
python3 scripts/test-deployment.py
```

검사는 local/prod Compose와 15/60초 interval 규칙, credential UID·서비스 격리, amtool route,
로컬 receiver의 그룹·해제·silence, 실제 Caddy template의 공개 차단을 확인합니다.
native 도구가 Docker Linux에서 동작해도 실제 EC2 host 연결·메모리·실제 Slack 전송을 완료했다고 표현하지 않습니다.
fixture는 `monitoring/tests/`에서 별도 도구 컨테이너에만 마운트합니다.
기존 `test-ai-policy-deploy.py`는 앱·호스트·release 전달을 함께 검사하므로 `test-deployment.py`로 이름을 바꿨습니다.
