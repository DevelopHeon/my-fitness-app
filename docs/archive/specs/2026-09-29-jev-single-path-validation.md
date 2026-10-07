# JEV 단일 경로 구현·검증 결과

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.

이후 배포 검사는 `scripts/test-deployment.py`로 이름을 바꿨다. 아래 당시 명령은 보존하며 현재 실행 방법은 [Testing](../../testing.md)을 따른다.


> 이 문서는 2026-09-29 당시 구현 검증 기록이다. 이후 실제 JEV 1,000건 평가 결과는 [2026-09-30 실측 보고서](../../evaluations/ai-policy/jev-live-1000-v1/report.md)를 본다. 아래의 `mode=legacy` 명령과 test-only Router 재현 검사는 당시 실행 기록이며 현재 코드에서는 제거됐다.

- 일자: 2026-09-29, 브랜치: codex/jev-policy
- 기준 commit: 0bd9787b7577b4d5d808f4b8d63d73e830476291
- 작업트리: /Users/gimhuiheon/.codex/worktrees/jev-policy/my-fitness
- [현재 계약](2026-09-29-jev-single-path-spec.md) · [구현 계획](2026-09-29-jev-single-path-spec.md#당시-구현-계획)

## 제거한 복잡도와 유지한 책임

운영 legacy/shadow/enforce mode enum·설정·선택 분기와 키워드 AiQueryRouter를 제거했다. Router 힌트 인자와 이력의 latestUserQueryType 조회도 제거했다. AiPolicyRun의 candidate/effective 구분·중복 JSON을 성공/실패 record로 바꿨다. 정상 결과는 필수 decision/assessment를 가지며 실패에는 오류/지연을 담는다. 정책 판단에 null 조합을 해석하지 않는다. 기존 방식은 immutable test-only Router와 평가 task에만 남으며 fallback으로 연결되지 않는다.

Controller → In Port → 유스케이스, JEV HTTP Infrastructure → Out Port, 순수 정책 결정, 짧은 DB 기록 transaction과 외부 호출 분리는 유지했다. ALLOW만 Context/답변 모델을 호출하고 BLOCK/SAFE_REDIRECT/CLARIFY와 모든 평가 실패는 생성하지 않는다. key가 없는 실제 Adapter를 사용한 API 테스트도 AI_POLICY_UNAVAILABLE / 503과 생성/Context 0회를 확인한다. 대역 policy/chat 내부에서 활성 DB transaction이 없음을 확인하고, 대기 중 사용자 메시지가 커밋된 것도 별도 연결에서 확인했다.

모델·정책 버전·판정 근거·확률·오류·지연과 정책/생성 사용량 분리를 유지했다. 실패의 실제 모델/판정/사용량은 알 수 없으므로 null이며 version/error/latency를 기록한다. V10과 과거 row를 수정하지 않았다. V10 SHA-256은 이전 구현과 동일한 `cfeddcc950305a986f5e07923077fd3e9943b79831eb928d4736501936a46ba1`이다. 신규 policy_mode=jev는 감사 표식이며 운영 설정이 아니다. 과거 SUCCESS+null, shadow+ALLOW 이력의 호환을 테스트했다.

main/test에서 기존 var 선언 150개를 명시적 타입으로 변환하거나 모드 경로 삭제와 함께 제거했다. 선언/대입 압축도 풀었다. Checkstyle 10.21.1 MatchXpath의 TYPE/IDENT AST 검사로 가독성 컨벤션을 강제하고 test/check/build/CI에 연결했다. 실제 같은 설정으로 local/final/for/enhanced-for/resource/lambda 6개 금지 fixture와 이름·주석·문자열·암시적 lambda 정상 fixture를 검증한다. IllegalType에서 resource/lambda 2건이 통과하는 실패를 발견해 AST XPath로 교체했다. 소스 검사 범위나 ArchUnit/Modulith 허용 규칙은 완화하지 않았다.

## 변경 전 1000개 정량 기준선

운영 경로 변경 **전에** 기존 Router의 판정을 실행하고 [manifest](../../evaluations/ai-policy/baseline-1000-v1/manifest.json), [판정 1000개](../../evaluations/ai-policy/baseline-1000-v1/cases.jsonl), [지표](../../evaluations/ai-policy/baseline-1000-v1/metrics.json), [비교표](../../evaluations/ai-policy/baseline-1000-v1/report.md)를 영구 보존했다. 변경 후 일반 테스트가 dataset hash와 각 판정을 재현한다. 최종 test-only 평가 재실행도 같은 결과를 냈다.

| 지표 | 변경 전 기존 판정 | 실제 JEV 변경 후 |
| --- | --- | --- |
| 제한 누락률(BLOCK/SAFE_REDIRECT가 ALLOW) | 340/400 = 85% | 미측정 |
| 정상 오차단률(ALLOW가 BLOCK/SAFE_REDIRECT) | 40/400 = 10% | 미측정 |
| 정상 허용률 | 360/400 = 90% | 미측정 |
| 동작 macro-F1 | 0.211255 | 미측정 |
| 정책 평가 장애 | 기존 판정 0/1000 | 미측정 |
| 실제 지연·비용·노출 답변 위반률 | 미측정 | 미측정 |

데이터는 **draft-synthetic**, 50 semantic family ×20 표현 변형의 합성 초안이다. ALLOW400/SAFE_REDIRECT280/BLOCK120/CLARIFY200이며 독립 사람 라벨 검토와 dev/holdout 분리가 없다. 1000개 독립 의미 표본이나 운영 요청 모집단 성능으로 해석하지 않는다. case 단위 Wilson CI는 family 상관을 반영하지 않는 탐색 값이고 F1 family bootstrap은 50개 cluster 기준이다. 대역 정확도를 실제 JEV 정확도로 표현하지 않았다.

Dataset SHA-256: `e0bce629e9ddd2d7a2c033bd911443cf0dc8000d238f023b259b487c3a913286`. 원본 운영 Router hash는 corpus manifest, 동결 fixture hash는 seed manifest에서 확인한다. 과거 baseline 결과와 migration/log를 삭제·변조하지 않았다.

## 실행한 검증과 결과

JDK 21 설정: `JAVA_HOME=/Users/gimhuiheon/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home`. macOS 기본 Java 17은 사용하지 않았다.

```bash
# 변경 전과 최종 재현 시 같은 명령
./gradlew aiPolicyEval -PaiPolicyEval.mode=legacy \
  -PaiPolicyEval.dataset=src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl \
  -PaiPolicyEval.allowDraft=true --no-daemon

./gradlew test --tests com.myfitness.architecture.JavaConventionTest --no-daemon
./gradlew checkstyleMain checkstyleTest test \
  --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.ai.*' --no-daemon
./gradlew build --no-daemon
npm --prefix frontend run lint
bash -n scripts/deploy-ec2.sh
python3 scripts/test-ai-policy-deploy.py
git diff --check
```

- 관련 검사: **150 tests / failures=0 / errors=0 / skipped=0** (architecture44, ai106).
- 최종 전체 build: **215 tests / failures=0 / errors=0 / skipped=0**. AST Main/Test 검사, ArchUnit, Spring Modulith verify/documentation, backend 동작, Next.js 정적/PWA frontend build, 통합 bootJar 성공.
- frontend eslint, deploy bash 문법, diff 검사 성공.
- 배포 script 검사: 3 test method의 6 시나리오(기본 모델/버전+과거 mode 무시, 명시값, 키 미등록, 빈 키, 키 권한 오류, 모델 권한 오류) 통과. 실패 시 이전 env/container 유지, 성공 시 env 권한600·변수 이름·키 로그 미노출을 검증한다. 실제 AWS/Docker를 호출하지 않는다. CI에도 연결했다.
- var 실제 부정 검증: 임시 main VarConventionProbe.java에 local var를 넣고 `./gradlew checkstyleMain --no-daemon`의 **예상 실패**와 해당 파일/컨벤션 오류를 확인했다. finally에서 probe를 삭제하고 정상 build를 실행했다. fixture의 resource/lambda까지 실제 AST 규칙으로 실패한다.
- 독립 read-only 최종 리뷰: 주요 오류 없음. AiRequestLog의 한 줄 다중 대입 지적을 수정했다.

검증 로그는 `/tmp/jev-single-baseline.log`, `/tmp/jev-single-final-baseline.log`, `/tmp/jev-var-fixed.log`, `/tmp/jev-var-negative-task.log`, `/tmp/jev-final-related.log`, `/tmp/jev-single-final-build.log`, `/tmp/jev-single-frontend-lint.log`다. 임시 로그 보존은 보장하지 않으며 영구 근거는 버전 관리한 corpus/보고서/테스트/설정이다. test report는 부분 재실행 시 덮어써질 수 있다. 평가 tag는 기본 build에서 제외한 opt-in이다.

## 미검증 범위와 배포 인계

실제 JEV API 호출·모델 의미 품질·공급자 운영 계약·사용량/청구·latency/availability, 독립 gold/holdout, 실제 답변 E2E 사람 평가, PostgreSQL/Flyway staging과 rollback, 원격 CI/CD는 미실행이다. migration 테스트는 H2의 TIMESTAMPTZ alias를 이용하므로 PostgreSQL 검증을 대신하지 않는다. CDK 정의는 변경하지 않았고 이번 로컬 CDK 검사는 실행하지 않았다.

코드 commit/push는 사용자 요청 범위이고 실제 AWS parameter 등록·배포는 사용자가 수동 실행한다. [운영 안내](../../../infra/README.md)에 `/my-fitness/prod/typesafe-api-key`(SecureString, 실제 key), `ai-policy-model`(String, jev-1.13.0), `ai-policy-version`(String, fitness-policy-v1)을 정리했다. 모델/버전은 미등록 시 기본값, key는 필수다. AI_POLICY_MODEL로 이름을 통일했으며 ai-policy-mode는 사용하지 않는다. 현재 CI는 main push/PR trigger이므로 이 작업 브랜치 push만으로 CI/CD 완료를 주장하지 않는다.
