# 로컬 관측 검증 기록

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.

이 문서는 당시 파일명과 명령을 보존한다. 이후 local/prod 파일 분리·이름 변경을 반영한 실행 절차는 [Monitoring](../../../monitoring/README.md)을 따른다.

> 2026-10-06 정리: 아래 README·Python 검사 언급은 당시 기록이다. 해당 중복 문서와 모니터링 Python 도구는 제거했으며, 현재 절차는 [Monitoring](../../../monitoring/README.md)을 따른다.


작성일: 2026-10-04, 위치: 기존 main. Prometheus 3.15.0, Grafana 13.2.3, Java 21, macOS Docker Desktop. 실제 AI 공급자는 호출하지 않았다.

## 자동 검증

| 명령 | 결과 |
| --- | --- |
| `./gradlew test --tests 'com.myfitness.common.*' --tests 'com.myfitness.ai.*' --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --tests '*UserSecurityIntegrationTest' --no-daemon` | 통과 |
| `./gradlew build --no-daemon` | 통과, backend 227건·frontend 19건 실패 0, Checkstyle·ArchUnit·Modulith·frontend build 포함 |
| `python3 scripts/test-monitoring-config.py` | 통과, 노출·보관·secret 초기화/참조·25개 패널·수집 상태 색상 계약 |
| `bash -n scripts/setup-local-monitoring.sh` | 통과 |
| `docker compose -f monitoring/docker-compose.yml config --quiet` | 통과 |
| promtool `check config`, `test rules` | 통과, 6개 규칙·4개 시나리오·37개 assertion 통과. pending/firing/복구·무트래픽·최소 건수/비율 조건 검증 |
| `git diff --check` | 통과 |

지표 인증 테스트를 먼저 실행해 미구현 endpoint의 실패를 확인했고 이후 통과했다. AI meter 미구현 상태의 테스트 컴파일 실패도 확인했다. 기본 `EmptyUsage`를 0토큰으로 기록하던 실패를 재현한 후 null로 정규화했고 실제 0 사용량은 계속 기록하는 테스트가 통과했다. 검증 과정의 실패 원인인 Spring Boot 테스트 기본 metrics export 비활성, histogram의 자동 le label, 테스트 import, 합성 시계열의 잘못된 matcher를 수정 후 재실행했다. 추가 검사 중 registry의 AutoCloseable 지원을 잘못 가정한 컴파일 오류도 명시적 close로 고쳤다. 기존 검사 규칙·허용 의존성은 변경하지 않았다.

정책 제한/장애의 생성 호출·meter 증가 없음, chat/photo 장애, 파일 거절, 토큰 누락을 검증했다. DB 성공 저장 메서드에 실패를 주입한 두 테스트에서 공급자 Timer는 한 번만 증가하고 FAILURE는 증가하지 않았다. 실제 Prometheus registry로 AI의 count·bucket·token 출력 이름을 확인했다.

## 실제 로컬 검증

- `python3 scripts/verify-local-monitoring.py`: endpoint의 무인증 401·인증 scrape, HTTP/JVM/Hikari 이름, target UP, Grafana datasource·provisioning·25개 query 통과.
- headless Chrome으로 로그인과 대시보드 렌더링 확인. 캡처는 Git 제외 경로 `build/monitoring-verification/grafana-dashboard.png`에 보관.
- 기존 PostgreSQL과 포트·컨테이너명이 충돌해 별도 검증용 DB의 55432 포트를 사용했다. 기존 컨테이너·데이터를 수정하지 않았다.
- 실제 앱 중지로 AppMetricsUnavailable의 pending→firing, 재기동 후 알림 해제를 확인했다.
- Compose down/up 후 이전 시각의 up 샘플 값이 동일하게 보존됐고 Grafana 관리자 로그인·provisioning·datasource도 유지됐다.
- 컨테이너 전용 암호 파일의 소유권/권한은 Prometheus `65534:65534 600`, Grafana `472:0 600`이었다. 각 기본 사용자로 자기 파일 읽기와 다른 계정 파일 읽기 거절을 검증했다. 호스트 파일 권한은 600을 유지한다.
- 두 이미지의 ARM64/AMD64 manifest를 확인했다. 실제 실행은 ARM64 Docker Desktop이며 AMD64/Linux 호스트 전체 실행 검증과 구분한다.

- 검증 후 이 작업에서 실행한 앱·모니터링 stack·별도 검증 DB를 종료했다. 기존 실행 컨테이너는 유지했으며 수집 named volume과 Git 제외 로컬 암호 파일은 보존했다. 재실행은 [로컬 안내](../../../monitoring/README.md)를 따른다.

## 검토 후 보완

파일 기반 Docker secret을 바로 마운트하면 Linux의 non-root 서버 UID와 호스트 600 파일의 UID가 맞지 않을 수 있다. 일회성 root `secret-init`으로 컨테이너 전용 볼륨에 복사·소유권 설정 후 서버에 읽기 전용으로 전달했다. 서버를 root로 실행하거나 호스트 파일을 644로 변경하지 않았다. CI에도 실제 사용자 읽기 검사를 추가했다.

합성 복구 테스트는 데이터가 끊긴 뒤 알림이 사라지는 것만 확인하지 않도록 정상 시계열을 50분까지 이어 45분 복구를 확인했다. UI에서 수집 상태 0을 빨간색·1을 녹색, 일반 stat의 N/A를 중립 색상으로 표시한다.

## 미검증·범위 제한

실제 JEV/OpenAI 호출·정확도, 30일 경과에 따른 삭제, 운영 AWS 설치·실제 사용자 장애 원인, Linux 호스트에서의 기동, 외부 Slack/Telegram 알림·CloudWatch·로그/tracing 연동은 검증하지 않았다. CI에 검사를 추가했지만 GitHub Actions 원격 실행 결과는 아직 없다.
