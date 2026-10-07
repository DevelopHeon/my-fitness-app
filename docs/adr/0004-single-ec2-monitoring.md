# ADR 0004 · 단일 EC2 모니터링과 자원 인수

- 상태: Accepted. 배치 선택의 상태이며 24시간 안정성 인수가 완료됐다는 뜻은 아니다.
- 정리일: 2026-10-07. local 설계·운영 실패·small 후속 관측을 근거로 작성한 회고 기록이다.

## 배경과 대안

사용 중 간헐적인 API·AI 문제의 시점과 자원 상태를 관측하고 Prometheus·Grafana를 학습하려 했다.
기존 EC2에 함께 설치, 별도 모니터링 EC2, 관리형 서비스 사용을 비교했다.
사용량이 적어 서버를 추가하지 않는 방향을 선택했지만 micro의 RAM은 앱과 Grafana 기동을 감당하기 부족했다.

## 결정

단일 t4g.small에 앱·Caddy·Prometheus·Grafana·Alertmanager·node_exporter를 배치한다.
수집·평가 60초, 보관 3일/1GB, 컨테이너 자원 한도와 앱 heap·swap 경계를 적용한다.
경보는 Prometheus에서 평가하고 Alertmanager가 그룹·재전송·해제를 관리해 Slack으로 전달한다.
대시보드·설정·운영 접속은 [Monitoring](../../monitoring/README.md), AWS 배치는 [Infrastructure](../../infra/README.md)를 따른다.

## 결과와 비용

별도 서버·공인 IP·볼륨 없이 구성할 수 있지만 앱과 관측의 자원·장애 영역이 같다.
EC2 전체 장애에서는 이 알림 경로도 멈춘다. AWS 상태 지표와 함께 확인해야 한다.
small에서도 Grafana가 512MiB 상한에 접근하면서 HTTP 지연·SQLITE_BUSY가 발생했고 OOM·재시작은 없었다.
최종 설정은 GOMEMLIMIT 320MiB로 Go 메모리 기준에 여유를 두었다. 이는 전체 RSS를 강제하는 한도가 아니다.
당시 영구 설정 배포 후 20분간 21회 관측에서 health·login·인증 API·Grafana 경유 PromQL이 통과했다.
재시작 효과와 메모리 기준의 영향을 완전히 분리한 실험이나 장기 안정성 검증은 아니다.
24시간·동시 사진 부하·실제 Slack·실공급자·보관 만료 검증은 [현재 인수 항목](../../monitoring/README.md#운영-인수)에서 추적한다.

## 근거

- [local 관측 설계](../archive/specs/2026-10-04-local-observability-spec.md)
- [단일 EC2 설계와 small 후속 선택](../archive/specs/2026-10-06-single-ec2-monitoring-spec.md)
- [당시 실패와 시간 경과 후 운영 관측](../archive/specs/2026-10-06-single-ec2-monitoring-validation.md#시간-경과-후-운영-검증)
