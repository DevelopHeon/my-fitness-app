# AI 질문 정책 검증 Implementation Plan

> Changes 기록: 정리 전 문서의 설계·상태·검증을 보존한 이력이다.
> 현재 계약과 실행 방법은 [현재 문서](../../../README.md)를 따른다. 아래 계획과 결과는 현재 배포 상태를 증명하지 않는다.

> 2026-09-28 설계 기록입니다. 운영 mode와 현재 계약은 [2026-09-29 JEV 단일 경로 계약](../2026-09-29-jev-single-path/spec.md)이 대체합니다.
> 구현자는 spec과 이 계획을 함께 읽고 단계별로 실행한다. 실행을 요청받으면 `superpowers:executing-plans`를 사용한다. 이 문서는 구현 승인이나 커밋·push 요청을 대신하지 않는다.

- 기준일: 2026-09-28 (월요일)
- 상태: 로컬 구현·검증, 독립 라벨/live·운영 승격 gate 보류
- Goal: 현재 질문 gate와 Jev 정책 gate의 차이를 재현 가능한 데이터·지표로 확인하고 검증된 후보만 활성화한다.
- Architecture: 기존 Modulith/ArchUnit 경계를 유지한다. application Out Port 뒤에 Jev adapter를 두고 application의 순수 정책 코드가 최종 동작을 결정한다. 기존 답변 생성 gateway는 재사용한다.
- Tech Stack: Java 21 / Spring Boot 4.1.1 / Spring AI 2.0.1 / Spring Modulith 2.1.1 / ArchUnit 1.4.2 / JUnit 5 / Flyway.
- Spec: [정책 spec](spec.md)

## 공통 제약

- 버전·기존 규칙·allowedDependencies를 검사 통과 목적으로 바꾸지 않는다.
- 정책과 Context 주제, Jev 확률과 Java 결정, candidate와 effective 결정을 구별한다.
- API key는 서버 환경에만 둔다. 실제 외부 호출은 opt-in 평가·확인된 shadow 운영에 한정한다.
- `providerCalled`는 답변 생성 호출 여부다. 정책 거절·명확화는 Insight/답변 호출 0회다.
- 초기 임계값은 spec section 5, 초기 deadline 1500ms/재시도 0회. dev 검증 후 version/hash를 고정한다.
- 목표 corpus 1000개는 아직 확보되지 않았다. task와 초안 seed 실행 도구는 구현됐다. 미구현·미측정을 pass로 보고하지 않는다.
- 구현 명령은 Java 21 환경에서 실행한다. 현재 로컬 환경은 기본 Java 17이므로 필요 시 `JAVA_HOME=$(/usr/libexec/java_home -v 21)`을 설정한다.
- 각 단계는 의미 있는 실패 테스트 → 최소 구현 → 아키텍처·컨벤션 → 동작 검증 순으로 수행한다.

## 검토 중 특히 확인할 사례

| 실패 가능성 | 담당 단계와 검증 |
| --- | --- |
| 운동 단어를 섞은 범위 밖·의료·우회 질문 | 단계 2 corpus, 단계 3 결정 우선순위, 단계 6 API 호출 0회 |
| 정상 질문의 부정 표현·교육적 인용을 위험으로 오판 | 단계 2 독립 라벨링, 단계 7 오차단·한국어 slice |
| 직전 거절 뒤 짧은 후속 질문이 허용됨 | 단계 5 이력 필터, 단계 6 다중 턴 API |
| shadow 후보와 실제 응답이 다른데 로그·이력이 후보를 정답으로 취급 | 단계 5 effective/candidate 저장, 단계 6 shadow 회귀 |
| 캐시·timeout을 제외해 비용과 지연이 유리하게 보고됨 | 단계 7 보고서 fixture와 장애 포함 집계 |

## 단계 1. 기존 아키텍처 검사 보완

**수정:** `src/test/java/com/myfitness/architecture/LayerArchitectureTest.java`.

**추가:** `src/test/java/com/myfitness/architecture/ArchitectureRuleDetectionTest.java`, `src/test/java/com/myfitness/architecture/fixture/`의 최소 정상·금지 의존성 클래스. fixture는 운영 import에 포함하지 않는다.

**입력:** 현재 ArchRule·운영 클래스, 기존 Modulith 메타데이터.

**출력:** 같은 package 내에서 재사용 가능한 기존 rule 객체, 운영 import의 명시적 범위, 금지 의존성 탐지 근거. 검사를 위한 범용 rule framework를 만들지 않는다.

- [x] 운영 import에 테스트 클래스가 포함되지 않음을 확인하는 실패 테스트를 작성한다. `AiQueryRouter`, `SpringAiChatGateway`와 각 필수 계층이 포함되어야 한다.
- [x] Result와 read-only transaction 대상이 0개일 때 실패하는 검증을 추가한다. 기존 ArchRule의 empty-subject 실패도 유지한다.
- [x] `DO_NOT_INCLUDE_TESTS`를 적용하고 검사 집합의 비어 있지 않음을 assert한다. 생성 코드가 있다면 실제 위치를 확인하여 구체적으로 처리한다.
- [x] 대표 rule에 `because(...)`로 이유와 허용 수정 방향을 추가한다. 같은 객체로 정상 fixture 통과·금지 fixture 실패를 assert한다. 단순한 별도 복제 rule의 실패만 검사하지 않는다.
- [x] AI의 domain/application/presentation이 Spring AI SDK 및 `ai.infrastructure.typesafe`에 의존하지 못하게 제한한다. source import 문자열만 검색하는 검사로 대체하지 않는다.
- [x] Modulith는 현재 metadata 그대로 verify한다. 격리된 fixture 또는 임시 운영 클래스로 다른 모듈 내부 접근이 실제 실패하는지 확인하고 임시 변경을 제거한다.

검증: `./gradlew test --tests 'com.myfitness.architecture.*'`.

완료 기준: 기존 32개 아키텍처 검사를 유지하고, 신규 검사 정상 통과 및 대표 금지 fixture 탐지를 확인한다. 현재 위반이 나오면 위반별 원인을 보고하고 요청 범위 안에서 해소한다. 광범위한 제외·기준선 재동결 금지.

## 단계 2. 정답 데이터와 변경 전 평가를 먼저 고정

**추가:** `src/test/resources/ai-policy/corpus-v1.jsonl`, `src/test/resources/ai-policy/manifest-v1.json`, `src/test/java/com/myfitness/ai/evaluation/AiPolicyCorpusTest.java`, `src/test/java/com/myfitness/ai/evaluation/LegacyAiPolicyBaselineTest.java`, `src/test/java/com/myfitness/ai/evaluation/legacy/LegacyAiQueryRouter.java`.

**입력:** [seed 24개](../../../evaluations/ai-policy/router-seed-v0/seed-v0.jsonl), spec의 정책 rubric, 현재 Router commit.

**출력:** 불변 corpus/hash·분할·라벨 근거·독립 검토 기록과 A의 case별 판정. legacy Router는 평가 재현에만 쓰는 현재 구현의 고정본이며 운영에서 주입하지 않는다.

- [ ] id 중복, 필수 라벨 부재, familyId의 split 중복, counts 불일치, 위험 Noul 정답 부재, 합의되지 않은 정답이 실패하는 corpus 테스트를 작성한다.
- [ ] 두 사람이 모델 결과를 보지 않고 seed부터 라벨링한다. 불일치가 있으면 rubric과 seed를 수정하고 그 사유를 남긴다.
- [ ] dev 400 / holdout 600을 준비한다. 규모가 부족하면 실제 개수와 CI 한계를 기록하고 승격 완료 처리하지 않는다.
- [ ] 현재 Router를 immutable legacy fixture로 보존하고 원본 SHA·source hash를 manifest에 기록한다. 입력 case의 gold는 Router나 Jev 입력에 전달하지 않는다.
- [ ] A는 OUT_OF_SCOPE→BLOCK, 그 외→ALLOW로 계산한다. 명확화·의료 안내를 현재 존재하는 동작처럼 만들지 않는다.
- [ ] A 결과·confusion matrix·누락·오차단을 고정한다. 정책 분류와 주제 분류 결과를 별도 저장한다.

검증: `./gradlew test --tests 'com.myfitness.ai.evaluation.AiPolicyCorpusTest' --tests 'com.myfitness.ai.evaluation.LegacyAiPolicyBaselineTest'`.

완료 기준: 최종 정답이 독립 검토되었고, 같은 fixture/hash에서 A 결과가 재현된다. holdout은 이후 임계값 튜닝에 사용하지 않는다.

## 단계 3. 외부 호출 없는 정책 결정 코드

**추가:** `src/main/java/com/myfitness/ai/application/port/out/AiPolicyGateway.java`, `src/main/java/com/myfitness/ai/application/policy/AiPolicyDecision.java`, `src/main/java/com/myfitness/ai/application/policy/AiPolicyEvaluator.java`, `src/main/java/com/myfitness/ai/application/config/AiPolicyProperties.java`, `src/test/java/com/myfitness/ai/application/policy/AiPolicyEvaluatorTest.java`.

**인터페이스 제안:**

```java
// AiPolicyGateway 내부에 요청/평가 record와 TopicAssessment를 함께 둔다.
AiPolicyAssessment assess(AiPolicyRequest request);
// request: currentQuestion, screen, previousTurns. 식별자·개인 기록 없음.
// assessment: 응답 모델 ID, 4개 위험 확률, topic/분포/confidence,
//             inputTokens, latencyMs. 외부 JSON 타입 없음.

// AiPolicyEvaluator
AiPolicyDecision decide(AiPolicyAssessment assessment);
// decision: Action(ALLOW/BLOCK/SAFE_REDIRECT/CLARIFY), reason, nullable AiQueryType.
// AMBIGUOUS는 null 주제이며 저장 계층에서 기존 enum 호환 방식 적용.
```

- [ ] `0.35`, `0.70`, `0.60`의 바로 아래·같음·바로 위 경계 테스트부터 작성한다. 동작과 reason을 함께 assert한다.
- [ ] urgent+scope 밖, medical+fitness, unsafe+정상 화면, bypass+직전 허용, 낮은 topic confidence, AMBIGUOUS의 우선순위를 테스트한다.
- [ ] 정상 운동 질문과 위험을 설명하는 일반 질문의 고정 assessment는 ALLOW가 되어야 한다. 테스트 대역의 고정 확률은 실제 모델 정확도 근거로 쓰지 않는다.
- [ ] spec section 5의 순서만 구현하고 settings 유효성(확률 범위, timeout 양수, mode 유효)을 검사한다. `@Service` 위치 정책에 맞추어 순수 결정기는 일반 클래스 또는 `@Component`로 둔다.

검증 순서: `./gradlew test --tests 'com.myfitness.architecture.*'` → `./gradlew test --tests 'com.myfitness.ai.application.policy.*'`.

완료 기준: 네트워크·DB 없이 결정이 재현되고, 정책 동작과 주제가 분리된다.

## 단계 4. Jev adapter와 공급자 계약 검증

**추가:** `src/main/java/com/myfitness/ai/infrastructure/typesafe/JevAiPolicyGateway.java`, `src/test/java/com/myfitness/ai/infrastructure/typesafe/JevAiPolicyGatewayTest.java`.

**수정:** `src/main/resources/application.yml`. 필요 시 기존 HTTP client 설정만 재사용한다.

**입력:** 단계 3 요청, server key·모델·정책 version.

**출력:** 공식 `/v1/systemone` 응답에서 매핑한 `AiPolicyAssessment`. 평가 원문·SDK 타입은 application에 전달하지 않는다.

- [ ] 로컬 stub HTTP 서버로 method/path, 인증 header 존재, 5개 질문 type·criteria, state의 current question·최소 이력만 전송함을 검증한다. secret의 실제 값은 출력하지 않는다.
- [ ] 정상 Noul/Choice fixture를 매핑하는 테스트를 작성한다. Noul confidence를 읽는 구현은 금지한다.
- [ ] key 누락, type 불일치, NaN·범위 밖 확률, 분포합 불일치, Choice 선택지 부재, 모델 불일치, 잘못된 usage를 실패시키는 테스트를 작성한다.
- [ ] 401/422/429/529/5xx, 끊긴 연결, 빈 응답, 1500ms deadline을 검사한다. 요청 재시도 수는 0회다.
- [ ] Java/Spring HTTP client로 최소 adapter를 구현한다. Python sidecar, 별도 moderation SDK, 공급자 plugin 설치를 추가하지 않는다.

검증 순서: `./gradlew test --tests 'com.myfitness.architecture.*'` → `./gradlew test --tests 'com.myfitness.ai.infrastructure.typesafe.*'`.

완료 기준: 실제 키 없이 계약·실패 테스트가 통과하고 adapter만 원격 wire 형식을 안다.

## 단계 5. 정책 저장·허용 이력·트랜잭션 경계

**수정:** `AiMessage.java`, `AiRequestLog.java`, `AiRequestStatus.java`, `AiMessageTransactionService.java`, `AiMessageRepositoryPort.java`, `AiMessageRepositoryAdapter.java`, `SpringDataAiMessageRepository.java`, `AiHistorySelector.java` (모두 기존 `ai` 패키지의 해당 파일).

**추가:** `src/main/resources/db/migration/`에 다음 번호의 정책 메타데이터 migration, `src/test/java/com/myfitness/ai/infrastructure/persistence/AiPolicyPersistenceIntegrationTest.java`.

**입력:** 단계 3 assessment/decision, mode, 기존 생성 결과.

**출력:** 정책과 생성 메타데이터가 합쳐진 기존 요청 로그, 같은 소유 conversation의 허용 user/assistant 쌍. 추가 테이블은 만들지 않는다.

- [ ] 기존 row의 정책 필드가 null인 상태에서 migration·조회가 성공하는 테스트를 작성한다.
- [ ] 최종 주제 업데이트, 정책 거절·명확화 상태, 생성 실패 후에도 남는 정책 평가, 외부 실패의 null 결정·오류코드를 검사한다.
- [ ] shadow에서는 로그의 effective decision을 기존 동작으로 남기고 candidate decision은 결과 JSON에 저장하는 테스트를 작성한다. 둘을 혼합하지 않는다.
- [ ] 이력에서 SUCCESS+effective ALLOW만 조회한다. legacy SUCCESS+null은 호환한다. 거절·명확화·실패와 다른 conversation은 제외한다. 실제 조회를 repository 통합 테스트로 확인한다.
- [ ] 현재 4개/2000자 평가 이력 한도를 적용한다. 같은 사용자의 대화라도 conversation이 다르면 전달하지 않는다.
- [ ] 차단 안내의 assistantMessage와 요청 로그 연결도 검증한다. JSON 메타데이터에 질문·응답·식별자·인증 헤더가 없는지 검사한다.

검증 순서: architecture → `./gradlew test --tests 'com.myfitness.ai.infrastructure.persistence.*' --tests 'com.myfitness.ai.application.service.AiHistorySelectorTest' --tests 'com.myfitness.ai.domain.model.*'`.

완료 기준: append migration과 실제 조회로 이력·판정 일관성을 증명하고 기존 생성 메타데이터의 의미를 보존한다.

## 단계 6. 기존 AI Coach API에 gate 연결

**수정:** `AiCoachService.java`, `AiSendMessageResult.java`, `AiSendMessageResponse.java`, `GlobalExceptionHandler.java`, `frontend/src/lib/ai-api.ts`, 기존 `AiCoachApiIntegrationTest.java`, `AiMessagePersistenceIntegrationTest.java`.

**추가:** `ai.application.exception.AiPolicyUnavailableException`과 `src/test/java/com/myfitness/ai/presentation/controller/AiPolicyApiIntegrationTest.java`. 새 exception은 기존 공개 application-exception 패키지에 두어 Modulith 허용 목록을 늘리지 않는다.

**입력:** 기존 `AiMessageCommand`; 서버 policy mode; 단계 4 gateway 및 단계 5 이력.

**출력:** 기존 API + `policyDecision`; 기존 `providerCalled` 의미 유지.

- [ ] 인증 실패·타인 대화·빈 질문·1001자 입력은 두 gateway 호출 0회로 assert한다.
- [ ] ALLOW는 Jev 1회, 최종 주제의 Context만 조회, 답변 gateway 1회다. BLOCK/SAFE_REDIRECT/CLARIFY는 Jev 1회, Context와 답변 0회, 저장 응답 200이다.
- [ ] Jev 차단 판단을 기존 Router/화면/이력이 덮어쓸 수 없음을 검사한다. 반대로 의미상 허용인데 Router가 모르는 표현은 Jev 최종 주제로 Context를 선택한다.
- [ ] 정책 deadline 실패는 503 `AI_POLICY_UNAVAILABLE`, 사용자 메시지 1개·FAILED 로그, 답변 0회다. 뒤늦은 원격 응답은 assistant 저장으로 이어지지 않아야 한다.
- [ ] 기존 blocking gateway 테스트를 정책에도 적용해 원격 평가 대기 중 사용자 메시지가 커밋된 것을 별도 DB 조회로 확인한다. 최종 주제 수정·생성 단계의 기존 저장 검증도 유지한다.
- [ ] legacy/shadow/enforce 각각의 실제 응답·candidate 메타데이터·가용성 동작을 확인한다. shadow 후보 BLOCK + 실제 legacy ALLOW의 이후 이력도 검사한다.
- [ ] UI는 기존 안내 메시지 표시를 재사용하고 타입만 확장한다. 정책 확률이나 공급자 내부 설정을 사용자 화면에 표시하지 않는다.

검증: architecture → `./gradlew test --tests 'com.myfitness.ai.*' --tests 'com.myfitness.user.integration.UserSecurityIntegrationTest'` → 전체 `./gradlew test --no-daemon` → `npm --prefix frontend run lint` → `./gradlew build --no-daemon`.

완료 기준: 기존 API·사용자 격리·저장 테스트를 유지하면서 네 가지 정책 동작과 실패 경로가 검증된다.

## 단계 7. 동일 corpus A/B 평가와 보고 도구

**추가:** `src/test/java/com/myfitness/ai/evaluation/AiPolicyEvaluationTest.java`, `AiPolicyMetricsTest.java`, 같은 패키지의 최소 평가·지표 helper.

**수정:** `build.gradle.kts` (기본 test에서 `ai-policy-eval` tag 제외, 별도 `aiPolicyEval` Test task와 mode 선택), `docs/guides/testing.md` (실제 추가된 평가 명령 안내).

**입력:** 단계 2 corpus/legacy, 단계 3·4 후보, 고정 모델·질문·임계값·hash.

**출력:** `build/reports/ai-policy/<run-id>/{manifest.json,cases.jsonl,metrics.json,comparison.md}`.

- [ ] 작은 수작업 정답 fixture로 제한 누락, 정상 오차단, 불필요 명확화, 4×4 matrix, macro-F1, 분모 0일 때 N/A를 검증한다. 모든 제한을 CLARIFY로 돌린 결과가 높은 대응 정확도로 보고되지 않아야 한다.
- [ ] 비용 분모가 전체 입력이며 Jev+생성 호출을 합산함을 확인한다. timeout·unknown usage·실패도 보고서에서 제외되지 않아야 한다.
- [ ] live/replay 구분, key 없는 live 실패, cache replay에서 지연·비용 N/A, requested/actual model mismatch 실패를 검사한다.
- [ ] 기본 PR CI에 외부 호출이 없음을 확인한다. 아키텍처 fixture도 운영 import에서 빠져야 한다.
- [ ] dev 데이터만으로 질문·임계값을 조정하고 hash를 고정한 뒤 holdout을 봉인 해제한다.
- [ ] legacy / jev-live / replay를 실행한다. 실제 live run 3회의 case 결과를 보존하고 반복 횟수를 표본 개수로 곱하지 않는다.
- [ ] 고정 E2E subset 120개의 실제 노출 응답을 사람이 맹검 검토한다. Jev 자신을 유일한 평가자로 쓰지 않는다.
- [ ] Wilson CI와 familyId bootstrap delta, 한국어·우회·의료 slice, 비용·지연·가용성 보고서를 작성한다.

검증: architecture → `./gradlew test --tests 'com.myfitness.ai.evaluation.*'` → opt-in 평가 3개 mode (spec section 9).

완료 기준: 같은 hash에서 A/B 수치를 비교하고 spec section 8의 목표·CI·한계를 근거로 후보의 도입 여부를 결정한다. 키·정답·budget이 없으면 미측정 상태를 남기고 enforce로 넘어가지 않는다.

## 단계 8. 운영 확인과 문서 동기화

**수정 대상:** 구현된 계약에 맞춘 `docs/reference/architecture.md`, `docs/reference/product.md`, `docs/guides/testing.md`, `docs/guides/troubleshooting.md`. secret 운영 경로는 기존 SSM 방식에 맞춰 구현 시 확인한다.

- [ ] TypeSafe 계정·전송·보존 조건, 실제 모델 접근, budget, 고정 안내 문구, holdout 승격 목표를 확정한다.
- [ ] 스테이징 shadow에서 1000회 이상 품질·가용성·지연을 관찰하고 원문 없는 메타데이터를 점검한다.
- [ ] 검증된 이전 정책·모델로 복원하면서 enforce를 유지하는 rollback을 검증한다. 복원 대상이 없으면 503 처리한다. 스테이징의 enforce→shadow/legacy 모드 전환도 확인하고 운영에서 정책 공백을 재개하는 변경은 별도 기록한다.
- [ ] 기존 CI의 test/build가 새 검사와 대역 테스트를 실행하는지 확인한다. 새 live 자동 실행 workflow는 기본 산출물에 넣지 않는다.
- [ ] 구현된 기능만 현재 구조·제품 문서에 반영하고 이 spec 하단에 실제 결과·미달 gate·후속 과제를 갱신한다.

최종 검증: `git diff --check`, `./gradlew test --no-daemon`, `npm --prefix frontend run lint`, `./gradlew build --no-daemon`. infra를 실제 수정하면 기존 `npm run build`, `npm test -- --runInBand`, `npx cdk synth --quiet`도 실행한다.

완료 기준: CI·동작·평가·운영 gates가 검증되고 실제 활성 모드·rollback 결과를 보고한다. 미측정 비용·한국어 품질·가용성을 완료로 표현하지 않는다. 커밋·push·배포는 별도 요청 범위에 따른다.

## 단계별 변경 단위

1. 검사 보완과 기준선/corpus를 먼저 독립 검토한다 (단계 1–2).
2. 대역으로 정책·adapter·저장·API를 완성한다 (단계 3–6).
3. 동일 데이터 평가와 관찰 후 활성화 여부를 결정한다 (단계 7–8).

각 단위는 동작 검증까지 완료한 후 다음 단계로 진행한다. 구현자가 source path나 계약을 바꾸면 spec과 계획도 같은 변경에서 맞춘다. 원래 체크리스트는 승인된 평가·운영 완료 기준을 보존한다. 아래 상태표는 실제 구현 범위와 미달 gate를 구분한다.

## 구현 실행 현황

| 단계 | 구현/검증 상태 | 남은 gate |
| --- | --- | --- |
| 1 구조 검사 | 운영 import/empty selection/위반 fixture/SDK/Modulith 탐지 검증 | 없음 |
| 2 corpus·기준선 | draft 24개, immutable Router/hash, 재현 보고서 | 두 사람 독립 검토·dev400/holdout600 |
| 3 결정 코드 | 임계값/우선순위·유효 설정 검사 | dev 데이터로 임계값 검증 |
| 4 Jev adapter | 로컬 HTTP 계약/오류/deadline/응답 모순 검증 | 실제 account/model 접근 |
| 5 저장·이력 | V10/H2 migration·legacy null·허용 턴·동시 요청 pair | 실제 PostgreSQL/Flyway staging |
| 6 API | 인증/격리/ALLOW/거절/명확화/장애/모드/commit 통합 검증 | 실제 provider 연동 |
| 7 평가 | legacy/live/replay task, 지표/CI/slice/run 변동 도구 | live 3회, 독립 holdout, E2E120 사람 검토 |
| 8 운영/문서 | SSM script·배포 전 설정 실패·문서 동기화·로컬 build | 데이터 전송 조건/budget, staging1000, rollback/배포 |

실행 명령·실제 결과·리뷰 수정은 [구현 검증 기록](validation.md)을 따른다. 키·정답·운영 증거가 없는 gate는 완료 처리하지 않았다. 커밋·push·배포는 수행하지 않았다.
