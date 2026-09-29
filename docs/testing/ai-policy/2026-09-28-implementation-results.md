# Jev AI 질문 정책 구현·검증 기록

> 이전 작업 시점의 검증 기록입니다. 현재 구현·1000개 기준선·커밋/푸시 상태는 [최신 검증 기록](2026-09-29-jev-single-path-results.md)을 따릅니다.
- 일자: 2026-09-28
- 구현 작업트리: `/Users/gimhuiheon/.codex/worktrees/jev-policy/my-fitness`
- 브랜치: `codex/jev-policy` (미커밋, push/배포 미실행)
- 기준 commit: `0bd9787b7577b4d5d808f4b8d63d73e830476291`
- 상태: 코드·로컬 검증 완료 / 독립 gold·live·운영 승격 미완료
- [Spec](../../spec/2026-09-28-mon-pr-012-ai-policy-validation.md) · [단계별 계획](../../spec/2026-09-28-mon-pr-012-ai-policy-validation-plan.md)

## 구현된 동작

사용자 입력/소유권 확인 → 사용자 메시지 커밋 → Jev 정책 평가 → 최종 주제 저장 → ALLOW에만 개인 Context·기존 답변 생성 순서다. BLOCK/SAFE_REDIRECT/CLARIFY는 고정 안내를 저장한다. enforce 평가 장애는 사용자 메시지·FAILED 로그와 503 `AI_POLICY_UNAVAILABLE`을 남긴다. `policyDecision`은 공개 응답에 추가되며 `providerCalled`는 답변 생성 여부를 유지한다.

기본 mode는 legacy다. shadow는 기존 결과를 적용하고 Jev 후보·오류만 별도 기록한다. 정책 사용량/모델/확률/결정과 생성 사용량을 분리했다. V10 nullable 컬럼은 기존 row를 변경하거나 추정 backfill하지 않는다. 성공+effective ALLOW 및 기존 SUCCESS+null 이력만 사용한다. 요청 로그의 연결 ID로 동시 요청도 정확한 질문·답변 쌍을 구성하며 정책 이력은 최근 두 쌍·2000자 이내다.

운영 ArchUnit import에서 test fixture를 제외하고 빈 대상·대표 위반·직접 SDK 의존 탐지를 보완했다. 실제 임시 금지 의존성으로 Modulith가 실패하는 것을 확인한 뒤 probe를 제거하고 정상 suite가 통과했다. 개인 skill 파일이나 allowedDependencies/기존 기준선을 완화하지 않았다.

## 정량 비교의 현재 범위

초안 seed 24개는 독립 검토되지 않은 진단 사례다. 모집단 성능이나 운영 위험률로 해석하지 않는다. 3회 legacy 실행은 같은 24개 반복이며 독립 표본은 24개, 총 실행은 72개, 결정 변동률은 0이다.

| 지표 | 변경 전 legacy | 실제 Jev 변경 후 |
| --- | --- | --- |
| 제한 누락률 | 9/10 = 90%, Wilson95% 59.58–98.21% | 미측정 |
| 정상 오차단률 | 2/10 = 20%, Wilson95% 5.67–50.98% | 미측정 |
| 정상 허용률 | 8/10 = 80% | 미측정 |
| 동작 macro-F1 | 0.192857 | 미측정 |
| 답변 노출 위반률·API 지연·생성 비용 | 미측정 | 미측정 |

확률·SDK 대역 통과율을 Jev 의미 정확도로 계산하지 않았다. 실패 전용 합성 replay 24개는 replay 보고 동작만 확인한 자료이며 Jev 결과가 아니다. 모두 UNAVAILABLE, F1/CI/지연/비용은 null로 남았다. 실제 key 없는 live는 명확히 실패했고 외부 호출을 수행하지 않았다.

생성된 실제 legacy 보고서는 작업트리의 `build/reports/ai-policy/1790603980146-legacy/run-1/`와 batch root aggregate.json에 있다. build 산출물은 ignored이며 후속 clean 시 삭제될 수 있다. 영구 보존은 corpus/immutable Router/해시/실행 도구다.

- dataset SHA-256: `a45b49e919765fb3faf24ebeb7d0977ceb59f82308c7ae050bf2b42a8392bdc2`
- source SHA-256: `b3d2dd8586c027af74e97942a458e889f6c61e96ef0fc671eda9fcf65c612371`
- questions SHA-256: `97f72cee2b22878f6696ae3987b15d627e2ee949f85c2beace135abc154e4f51`
- policy SHA-256: `84709f8a167ba0ff03b6f31187bb260cabaf3f03659bb97fb3e4321be0815af9`

## 실행 명령과 결과

현재 macOS 기본 Java 17은 프로젝트 Java 21과 맞지 않는다. 아래 실행에 사용한 JDK:

```bash
export JAVA_HOME=/Users/gimhuiheon/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
./gradlew test --tests 'com.myfitness.architecture.*' --no-daemon
./gradlew test --tests 'com.myfitness.ai.*' --no-daemon
./gradlew test --no-daemon
npm --prefix frontend ci
npm --prefix frontend run lint
./gradlew build --no-daemon
./gradlew aiPolicyEval -PaiPolicyEval.mode=legacy -PaiPolicyEval.runs=3 --no-daemon
bash -n scripts/deploy-ec2.sh
```

마지막 전체 backend build 실행의 XML 집계: **203 tests / failures=0 / errors=0 / skipped=0**, 이 중 architecture 37 / ai 101. `build`의 test/check와 Next.js TypeScript·정적 export·PWA 검증·통합 bootJar가 성공했다. frontend lint와 diff 검사도 통과했다. 기존 CI의 test/build가 새 대역 검사를 포함하며 live task는 opt-in이다. 원격 CI는 실행하지 않았고 CDK 정의는 변경하지 않아 이번 CDK 검증은 미실행이다.

부분 검사 재실행으로 `build/test-results/test`와 report는 덮어써질 수 있다. 최종 독립 재검사는 architecture 37개 이후 AI 101개 순서로 통과했다.

실행한 주요 로그는 `/tmp/jev-policy-final-build.log`, `/tmp/jev-policy-full-test.log`, `/tmp/jev-policy-frontend-lint.log`, `/tmp/jev-policy-eval-final-legacy.log`다. 임시 파일은 보존을 보장하지 않는다.

live 조건의 부정 검증:

```bash
env -u TYPESAFE_API_KEY ./gradlew aiPolicyEval -PaiPolicyEval.mode=jev-live --no-daemon
```

예상된 실패: `TYPESAFE_API_KEY가 없습니다` (`/tmp/jev-policy-live-no-key.xml`에 원인 보존). 실제 모델 run은 수행하지 않았다.

replay 실행 검증:

```bash
./gradlew aiPolicyEval -PaiPolicyEval.mode=replay -PaiPolicyEval.replay=/tmp/jev-policy-failure-replay.jsonl --no-daemon
```

hash/ID가 맞는 **테스트용 실패 metadata만** 준비한 replay로 성공했다. 정상 공급자 assessment의 캐시·모델/hash mismatch는 unit/HTTP 계약 테스트로 검증했다. replay는 실제 지연·비용 측정에 사용하지 않는다.

SSM 설정 검증은 실제 deploy를 실행하지 않고 로컬 stub AWS를 사용했다. mode 없음→legacy, shadow/enforce+키, enforce 키 누락, 잘못된 mode, AccessDenied의 6가지 시나리오가 예상 결과를 냈다. 조회 장애를 legacy 기본값으로 바꾸지 않으며 키 누락 시 container 교체 전 실패한다.

## 코드 리뷰와 회귀 수정

독립 read-only 리뷰에서 발견한 두 Important 문제를 재현하는 테스트가 실패한 뒤 수정해 통과했다: 동시 요청의 잘못된 이력 짝짓기, Choice 선택지와 최대 확률의 모순. Minor인 전체 UNAVAILABLE의 F1=0 표현도 null로 수정했다. 거절 시 Context 조회 0회, 직접 SDK 위반 fixture, topic F1/coverage/오류 포함 분모/한국어·위험 slice/반복 run 평균·최악·변동률을 보완했다.

## 남은 운영 gate

1. 두 사람이 모델 결과 없이 라벨링하고 rubric·일치율을 검토한다. dev400/holdout600은 아직 없다.
2. TypeSafe 계정·모델 접근·전송/보존 조건·budget을 확정하고 실제 key로 동일 corpus live 3회 평가를 수행한다.
3. 고정 120개 E2E 응답을 맹검 사람 검토해 실제 노출 답변의 안전성과 생성 비용·API 지연을 측정한다.
4. 실제 PostgreSQL/Flyway migration, staging1000회 품질/가용성/지연, 검증된 이전 이미지·모델 복원과 rollback을 확인한다.

H2 테스트는 기존 PostgreSQL TIMESTAMPTZ 표현을 domain alias로 호환시킨 SQL 검증이다. 실제 PostgreSQL migration을 검증했다고 표현하지 않는다. 현재 도구의 범위는 입력 gate이며 E2E 실행기·사람 검토 흐름은 후속 작업이다. 모든 보고서의 `promotionReady=false`를 유지하고 운영 enforce 승격은 보류했다. [운영 설정](../../infra/OPERATIONS.md)을 따른다.
