# 로컬 Prometheus·Grafana

Spring Boot 지표를 Prometheus가 15초마다 수집하고 Grafana가 시각화합니다. AWS 운영에는 설치하지 않았습니다. [스펙·구현 계획](../changes/2026/2026-10-04-local-observability/spec.md)을 참고하세요.

## 실행

Java 21, Node.js, Docker Compose가 필요합니다. repository root에서 실행합니다.

```bash
bash scripts/setup-local-monitoring.sh
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=monitoring ./gradlew bootRun
```

다른 터미널에서 실행합니다.

```bash
docker compose -f monitoring/docker-compose.yml up -d
```

이미 사용 중인 5432 포트·PostgreSQL 컨테이너가 있다면 기존 환경을 지우지 말고 앱의 `DB_URL`을 확인하거나 별도 개발 DB의 포트를 지정하세요. `.env`는 Spring Boot가 자동으로 읽지 않습니다. 기존 AI 환경변수 주입 방식은 유지하며 AI 키를 켜지 않아도 HTTP·JVM·DB 지표를 학습할 수 있습니다.

- Grafana: <http://localhost:3001>, 사용자 `admin`.
- Prometheus: <http://localhost:9090>, Targets·Graph·Alerts 화면.
- 지표: `http://localhost:8080/actuator/prometheus`, HTTP Basic 사용자 `prometheus`.

비밀번호는 `.local/monitoring/secrets/`의 `grafana_admin_password`, `app.monitoring.password` 파일에 생성됩니다. 직접 로컬에서 확인하되 터미널 기록·문서·Git에 복사하지 마세요. helper는 기존 값을 보존하며 디렉터리 700·파일 600 권한을 적용합니다. 앱은 config tree로 읽습니다. `secret-init`이 원본을 읽어 컨테이너 전용 볼륨에 600 권한으로 복사하고 Prometheus(UID 65534)·Grafana(UID 472)에 각각 소유권을 부여합니다. 두 서버는 해당 볼륨을 읽기 전용으로 마운트합니다. Linux 파일 바인드의 UID 불일치를 처리하기 위한 일회성 초기화이며 호스트 비밀값 권한을 낮추지 않습니다.

`monitoring` 프로필은 비밀번호가 없으면 시작하지 않습니다. 지표 체인은 업무 로그인 세션을 신뢰하지 않으며, 지표 계정으로 업무 API에 로그인할 수도 없습니다. 기본·prod 프로필에는 Prometheus endpoint를 노출하지 않습니다. 운영에서는 이 프로필을 활성화하지 마세요.

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

수집 상태부터 확인한 뒤 API 지연이 증가한 시각을 AI·DB·JVM 그래프에서 비교합니다. DB 대기가 생겼다면 풀 사용 중/최대와 획득 시간을 함께 확인합니다. Heap 증가가 보이면 GC와 API 지연을 함께 봅니다. 같은 시각에 변한 지표는 원인 후보이며 인과관계를 입증하지는 않습니다. 상세 원인은 앱 로그에서 확인합니다.

패널 제목의 **현재**는 조회 종료 시점의 마지막 관측값, **선택 구간**은 우측 시간 범위의 증가 추정, **10분**은 각 시점 직전 10분의 집계, **앱 누적**은 실행 중인 앱의 전체 기록입니다. 비교용 API·AI·DB 평균 추이는 모두 10분이며 GC 시간/초는 JVM 진단용 5분입니다. API 5xx·CPU·heap·풀 사용률은 0〜1 쿼리 값을 0〜100%로 표시합니다. 5xx 추이 축도 이 범위로 고정합니다.

AI를 사용하지 않으면 해당 지표는 N/A입니다. 사용량이 없거나 p95의 최근 10분 표본이 20건 미만인 경우에도 N/A가 정상입니다. HTTP 지표는 API 경로를 대상으로 집계하며 정적 파일·지표 수집 요청은 제외합니다. 인증 필터에서 끝난 요청의 URI가 `UNKNOWN`으로 기록될 수 있으므로 패널의 API 집계가 모든 보안 거절 요청을 포함하는 것은 아닙니다.

AI의 분포·오류·토큰·누적 평균 패널은 **조회 종료 시점에 실행 중인 앱의 누적값**을 instant query로 표시합니다. 앱 재시작 시 초기화되며 선택 시간 구간의 요청 수를 뜻하지 않습니다. 막대 길이는 건수·토큰의 상대 크기이며 비율이나 정확도가 아닙니다. 새 label의 첫 요청부터 counter가 1로 수집되면 increase/rate는 처음 값 이전의 증가를 알 수 없어 0을 반환할 수 있습니다. 소량 학습에서도 첫 요청과 평균 시간을 확인하도록 누적 count와 sum/count를 사용합니다. 기록이 없으면 N/A이며, 10분 AI 평균 추이·p95와 알림은 시간 구간 기반 검사를 유지합니다. 첫 호출만 관측된 상태에서는 누적 평균 카드에 값이 있어도 10분 추이는 비어 있을 수 있습니다.

경로별 API 표는 최근 10분 평균 시간 내림차순입니다. 표본 열은 scrape 기반 요청 증가 추정값이며 소수점·첫 관측 누락이 가능합니다. 활성 알림 카드는 기존 6개 규칙의 pending/firing 개수를, 알림 표는 규칙·상태·수준·대상을 보여줍니다. 빈 표와 수집 실패를 구분하려면 요약의 UP 상태도 확인하세요.

대시보드 JSON 변경은 파일 provisioning으로 반영됩니다. 잠시 기다린 뒤 브라우저를 새로고침하세요. 앱과 Prometheus를 재시작할 필요가 없으며, 반영되지 않으면 Grafana만 재시작합니다.

```bash
docker compose -f monitoring/docker-compose.yml restart grafana
```

정책 `BLOCK/SAFE_REDIRECT/CLARIFY`, 사진 `NOT_FOOD/UNCERTAIN`은 정상 결정입니다. JEV 오류는 `UNAVAILABLE`, 공급자 오류는 `FAILURE`로 구분합니다. 정책 분포로 모델 정확도를 평가하지 않습니다. HTTP 시간에는 정책·생성 시간이 포함되므로 각 시간을 합산하지 않습니다.

토큰 패널은 검증된 응답에서 관측한 input/output만 집계합니다. usage 누락과 Spring AI의 `EmptyUsage`는 미기록이며 실제 0 사용량과 구분합니다. 실패 요청의 과금은 알 수 없습니다. 기존 SDK `gen_ai.*`와 앱 지표를 더하거나 공급자 청구 금액으로 표현하지 않습니다. 모델·버전·세부 판정 근거는 기존 DB의 AI 요청 이력에서 확인합니다. Prometheus가 DB 이력을 반복 조회하지는 않습니다.

## 알림과 진단

외부 메시지를 보내지 않습니다. 규칙은 [alerts.yml](../../monitoring/prometheus/alerts.yml) 한 곳에 있으며 Prometheus Alerts와 Grafana 패널에서 확인합니다.

| 규칙 | 조건 | 확인 순서 |
| --- | --- | --- |
| AppMetricsUnavailable | scrape 실패 2분 | 앱 실행 → monitoring 프로필 → host 연결 → 전용 인증 파일 → timeout |
| ApiServerErrors | 10분 내 5xx 3건 이상·5% 이상, 2분 지속 | endpoint·발생 시각 → 앱 로그 → JVM·풀·AI 상태 |
| PolicyUnavailable | 10분 내 정책 장애 3건 이상·20% 이상, 2분 지속 | 오류 label → 기존 정책 로그 → 설정/키·timeout·통신·응답 검증 |
| AiProviderFailures | 종류별 10분 내 장애 3건 이상·20% 이상, 2분 지속 | chat/photo 종류 → 오류 label → 기존 요청 로그 → 공급자 설정 |
| JvmHeapPressure | 유효한 heap max 대비 85% 초과, 10분 | heap·GC·프로세스 자원·당시 요청 |
| DbPoolWaiting | pending > 0, 2분 | 풀 사용·획득 시간 → 느린 요청·DB 연결 상태 |

위 임계값은 학습용 초기값이며 운영 SLO가 아닙니다. `up=0`은 연결·인증 문제일 수도 있습니다. No Data를 장애 없음으로 해석하지 마세요. 비율뿐 아니라 최소 건수도 확인합니다. `increase`는 scrape 기반 추정이라 소수점이 나올 수 있고 앱 재시작으로 누적 counter는 초기화됩니다.

Prometheus·Grafana는 로그나 SQL 원인을 자동 수집하지 않습니다. 상세 원인은 기존 앱 로그·AI 요청 DB 이력에서 같은 시점을 확인하세요. 원문 질문·사진·응답·키를 출력하는 방식으로 진단하지 마세요. CloudWatch·브라우저 오류·tracing·외부 Slack/Telegram 전송은 후속 범위입니다.

## 보관·재시작

최대 30일 또는 TSDB 2GB 조건 중 먼저 도달한 조건이 적용됩니다. 2GB에 먼저 도달하면 보관 기간이 짧아집니다. WAL·head·compaction 때문에 실제 디스크 사용은 2GB를 넘을 수 있습니다. Docker volume 사용량도 함께 확인하세요.

```bash
docker compose -f monitoring/docker-compose.yml down
docker compose -f monitoring/docker-compose.yml up -d
```

위 명령은 named volume을 보존합니다. `down -v`는 저장 지표와 Grafana DB를 지우는 초기화이므로 데이터 삭제를 의도한 경우에만 실행합니다. Grafana 초기 비밀번호 파일을 바꿔도 기존 볼륨의 관리자 암호는 자동으로 바뀌지 않습니다. 기존 관리자 암호 변경은 로그인 후 UI에서 수행합니다.

## 검증

```bash
bash -n scripts/setup-local-monitoring.sh
docker compose -f monitoring/docker-compose.yml config --quiet
bash scripts/setup-local-monitoring.sh
docker compose -f monitoring/docker-compose.yml run --rm secret-init
docker compose -f monitoring/docker-compose.yml run --rm --no-deps --entrypoint sh prometheus -ec 'test -r /credentials/metrics_password; test ! -r /credentials/grafana_admin_password'
docker compose -f monitoring/docker-compose.yml run --rm --no-deps --entrypoint sh grafana -ec 'test -r /credentials/grafana_admin_password; test ! -r /credentials/metrics_password'
docker compose -f monitoring/docker-compose.yml run --rm --no-deps --entrypoint promtool prometheus check config /etc/prometheus/prometheus.yml
docker compose -f monitoring/docker-compose.yml run --rm --no-deps --entrypoint promtool prometheus test rules /etc/prometheus/alerts.test.yml
```

CI는 GitHub runner에서 임시 암호를 생성해 Compose·파일 접근 권한·promtool 설정과 규칙을 검사합니다. 실제 AI 키·AWS 연결은 필요하지 않습니다. 앱의 인증·지표 계약은 기존 Java 테스트로 검사합니다.

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
