# JEV 단일 정책 경로 계약

2026-09-29 사용자 요구사항과 실제 구현에 맞춘 현재 계약이다. [이전 설계](2026-09-28-mon-pr-012-ai-policy-validation.md)의 평가 rubric과 Out Port 계약은 유지하고 운영 모드 선택·후보/유효 판정은 이 문서로 대체한다. [구현 계획](2026-09-29-tue-pr-012-jev-single-path-plan.md) · [구현 검증](../testing/ai-policy/2026-09-29-jev-single-path-results.md) · [후속 실호출 1,000건](../testing/ai-policy/2026-09-30-jev-live-1000-results.md).

## 요청과 정책 결정

인증·질문 길이·대화 소유권 확인 → 허용 이력 조회 → 사용자 메시지 별도 transaction 커밋 → JEV 평가 → JEV 주제 저장 → 정책 결과 처리 순서다. 키워드나 현재 화면으로 운영 판정을 대체하지 않는다. 화면은 평가 입력으로만 전송하며 JEV 주제가 Context 선택 기준이다.

| 정책 결과 | 처리 | Context / 답변 모델 |
| --- | --- | --- |
| ALLOW | 기존 Context와 답변 생성·저장 | 호출 |
| BLOCK | 범위/정책 우회에 맞는 고정 안내·거절 로그 | 호출 없음 |
| SAFE_REDIRECT | 의료/위험/긴급 신호의 고정 안전 안내 | 호출 없음 |
| CLARIFY | 추가 질문 고정 안내·명확화 로그 | 호출 없음 |
| 평가 불가 | 사용자 메시지·실패 로그 보존, AI_POLICY_UNAVAILABLE / 503 | 호출 없음 |

평가 불가는 key 누락, 시간 초과, 통신 실패, HTTP 오류, 파싱·확률·모델 불일치 등 유효한 assessment를 얻지 못한 경우다. 재시도·키워드 fallback·다른 정책 provider 경로는 없다. 공개 응답 policyDecision/providerCalled 계약을 유지한다. 답변 생성 장애도 기존 503 계약과 이미 얻은 정책 metadata를 유지한다.

판정 우선순위는 긴급 신호(≥0.35) → 의료/위험 실행(≥0.70) → 우회(≥0.70) → 위험 불확실성(≥0.35) → 주제 불확실성(confidence<0.60 또는 AMBIGUOUS) → 범위 밖 → ALLOW다. 주제는 WORKOUT/NUTRITION/BODY/GENERAL_FITNESS/COMPOSITE/OUT_OF_SCOPE/AMBIGUOUS를 사용한다. 이 임계값은 실제 모델 품질 검증 전의 기존 설계값이다.

## 책임과 데이터 호환

- Controller는 AiCoachUseCase In Port를 호출한다. AiCoachService가 순서를 조정하고 Guard가 평가의 성공/실패, Evaluator가 정책 우선순위를 담당한다.
- AiPolicyGateway Out Port와 검증 가능한 request/assessment record가 외부 계약이다. HTTP·TypeSafe wire 처리는 Infrastructure JevAiPolicyGateway에 있다.
- AiPolicyRun은 sealed Success(version, decision, assessment, latencyMs) / Failure(version, errorCode, latencyMs)다. Success 필드는 필수이며 호출자는 null 조합 대신 두 경우를 분기한다.
- AiMessageTransactionService는 짧은 DB 작업만 담당한다. 정책·답변 원격 호출 동안 transaction을 유지하지 않는다. NOT_SUPPORTED 경계와 통합 테스트로 확인한다.
- V10 및 기존 컬럼·과거 row는 변경하지 않는다. policy_mode 컬럼은 호환을 위해 남겨 신규 row에 jev 감사 표식을 기록하며 운영 설정으로 읽지 않는다. candidate/effective JSON 중복을 제거한다. 성공은 모델/버전/결정/근거/확률/정책 token/지연, 실패는 버전/오류/지연을 기록하며 알 수 없는 실제 모델·결정·사용량은 null이다.
- 사용자 메시지 초기 query_type=OUT_OF_SCOPE는 기존 non-null DB 제약을 위한 미판정 placeholder다. 평가 성공 후 JEV 주제로 갱신하고 실패 턴은 허용 이력에 포함하지 않는다. AMBIGUOUS도 저장용 OUT_OF_SCOPE이며 실제 결정을 policy_decision=CLARIFY로 구분한다.
- 허용 이력은 SUCCESS+ALLOW 또는 과거 SUCCESS+null에서 로그에 연결된 정확한 사용자/답변 쌍이다. 과거 policy_mode 값은 선택 분기에 쓰지 않는다. JEV에는 최근 두 쌍/2000자 이내만 보내며 제한·명확화·실패 턴을 제외한다.
- AiQueryRouter는 운영에서 삭제했다. 후속 실호출 평가 뒤 test-only LegacyAiQueryRouter도 제거했으며, 변경 전 판정은 저장 산출물로만 보존한다. 평가 task는 명시적인 live/replay 옵션만 허용한다.

완화된 DDD/JPA 허용·기존 모듈 의존성·Port/Adapter 구조를 유지한다. 새로운 전략 프레임워크나 entity 분리를 추가하지 않는다.

## Java 가독성 컨벤션

src/main/java와 src/test/java에서 var 선언을 금지하고 명시적인 타입을 사용한다. 지역 변수·반복문·resource·lambda 타입을 모두 포함하며 선언/대입을 한 줄에 여러 개 압축하지 않는다. 기존 var를 명시적 타입으로 치환했다.

기존 AST 린터가 없어 Gradle 기본 Checkstyle plugin(10.21.1)에 MatchXpath 규칙 하나를 추가했다. `//TYPE/IDENT[@text='var']`가 Java AST의 타입 노드를 검사한다. 변수 이름·문자열·주석의 var는 허용한다. IllegalType으로 시작했으나 resource와 lambda를 놓치는 실제 실패를 확인해 AST XPath로 교체했다. 정상 fixture와 여섯 금지 형태를 같은 설정으로 검증한다. 실제 main 소스에 임시 var를 넣으면 checkstyleMain이 실패하는 것도 확인하고 probe를 제거했다.

Checkstyle은 test의 선행 작업, check/build, CI에서 실행한다. ArchUnit·Modulith 기존 규칙/허용 의존성/검사 범위를 완화하지 않는다. 규칙 근거: [Checkstyle MatchXpath](https://checkstyle.sourceforge.io/version/10.21.1/checks/coding/matchxpath.html), [Gradle Checkstyle plugin](https://docs.gradle.org/current/userguide/checkstyle_plugin.html).

## 정량 평가와 검증

변경 전 합성 1000개(50 family ×20 표현)의 기존 판정을 [cases.jsonl](../testing/ai-policy/baseline-1000-v1/cases.jsonl), manifest, metrics에 보존했다. draft-synthetic이며 독립 검토 정답·1000개 독립 semantic 표본을 뜻하지 않는다. 제한 누락 340/400(85%), 정상 오차단 40/400(10%), 동작 macro-F1 0.211255다. 같은 데이터의 JEV 실호출 1회 결과는 [별도 보고서](../testing/ai-policy/2026-09-30-jev-live-1000-results.md)에 기록했다.

일반 테스트는 허용/제한·장애·키 누락·생성/Context 호출 수·transaction 경계·과거 로그 호환·AST·ArchUnit·Modulith를 검증한다. 변경 전 baseline은 저장 결과로 보존하되 현재 테스트에서 이전 Router를 재실행하지 않는다. 실제 모델 의미 품질을 대역 결과로 표시하지 않는다. 독립 라벨 검토와 dev/holdout 분리, E2E 사람 평가, PostgreSQL와 staging/원격 CI는 별도 검증 범위다.

```bash
./gradlew checkstyleMain checkstyleTest --no-daemon
./gradlew test --tests 'com.myfitness.architecture.*' --no-daemon
./gradlew test --tests 'com.myfitness.ai.*' --no-daemon
./gradlew build --no-daemon
./gradlew aiPolicyEval -PaiPolicyEval.mode=jev-live -PaiPolicyEval.runs=1 \
  -PaiPolicyEval.dataset=src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl \
  -PaiPolicyEval.allowDraft=true --no-daemon
python3 scripts/test-ai-policy-deploy.py
```

## 배포 설정

모델 환경 변수는 AI_POLICY_MODEL로 통일한다. TYPESAFE_API_KEY는 서버 전용이며 frontend에 전송하지 않는다. [운영 안내](../infra/OPERATIONS.md)의 Parameter Store 3개 값/타입을 등록한다. ai-policy-mode와 AI_POLICY_MODE는 사용하지 않는다. 필수 key 누락과 조회 오류는 container 교체 전에 배포를 중단한다. key가 없이 실행된 앱도 정책 503을 반환한다. 사용자 승인에 따라 코드 commit/push만 실행하고 AWS 등록과 CI/CD는 사용자가 수동으로 수행한다.
