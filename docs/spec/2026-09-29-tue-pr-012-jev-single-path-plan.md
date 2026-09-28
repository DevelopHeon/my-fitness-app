# JEV 단일 운영 경로 구현 계획

승인 기준: 2026-09-29 사용자 요구사항 1–5. 기존 JEV 작업트리를 재사용한다. Java 21, 완화된 DDD/Port-Adapter, 기존 ArchUnit/Modulith 규칙과 migration을 유지한다.

1. [x] 운영 변경 전에 기존 Router의 test-only fixture로 합성 1000개 판정을 실행·보존한다. 50 family × 20 표현 변형이며 독립 사람 검토 전이다. 데이터/hash와 각 판정, 지표, 표본 한계를 함께 기록한다.
2. [x] 모드 enum/설정·legacy/shadow 분기·운영 Router·힌트 인자를 제거한다. 성공은 non-null assessment/decision, 실패는 오류/지연을 담는 최소 결과 타입으로 구분한다. JEV 필수·503/생성 0회·외부 호출 중 transaction 없음 회귀를 검증한다.
3. [x] 전체 main/test Java의 var를 명시적 타입으로 바꾸고 압축된 선언·대입을 풀어 쓴다. 기존 Java AST 린터가 없어 Gradle Checkstyle/MatchXpath(TYPE/IDENT=var)만 추가한다. 실제 정상·local/loop/resource/lambda 위반 fixture로 production 설정을 검증한다. check/build/CI에서 실행한다.
4. [x] 모델 환경 변수를 AI_POLICY_MODEL로 통일하고 배포 스크립트·현재 문서·테스트를 갱신한다. 과거 policy_mode/null 기록과 V10은 보존하고 신규 로그에 candidate 중복을 저장하지 않는다.
5. [x] AST 검사 → ArchUnit/Modulith → 관련/전체 동작 테스트 → frontend lint/통합 build와 diff 검사를 실행한다. 실제 JEV 품질·PostgreSQL·staging·원격 CI는 실행한 것만 보고한다. 사용자 후속 지시에 따라 검증 후 작업 브랜치를 커밋·push한다. CI/CD와 AWS parameter 등록은 사용자가 수동으로 진행한다.

JEV 계정 키가 없어 live 측정은 수행하지 않는다. 운영 key가 없으면 keyword fallback 없이 정책 503이 발생한다. 기본 test는 JEV 대역만 사용한다.

실행 기록: 변경 전 1000개 기준선 저장 → 정책/AST 테스트 실패 확인(25개 중 23 실패) → 단일 경로 구현 → AST resource/lambda 누락 2건 원인 확인·MatchXpath 보완 → 관련 150개 통과 → 전체 build 215개/실패 0 및 frontend lint 통과. 임시 var main 소스로 checkstyleMain의 실제 실패를 확인한 후 삭제했다. 독립 read-only 최종 리뷰의 주요 오류는 없었고 다중 대입 한 줄 표기 지적을 수정했다. 이전 V10 SHA-256과 내용이 동일하다. [최신 검증 기록](../testing/ai-policy/2026-09-29-jev-single-path-results.md)에 근거와 미검증 범위를 남긴다.

Ruling: 기존 IllegalType의 resource/lambda 누락을 확인해 같은 도구의 MatchXpath AST 규칙으로 대체한다. 검사 대상/허용 규칙은 완화하지 않는다. 사용자 후속 커밋/푸시 요청이 처음 계획의 비포함 범위를 대체한다.
