# 정책 판정 순서 수정 후 저장 응답 재평가

2026-09-30의 [JEV 실호출 1,000건](../jev-live-1000-v1/report.md)에서 보관한 응답을 새 정책 코드에 다시 적용했다. **JEV API를 다시 호출한 결과가 아니다.** 질문 문구, 모델, 임계값, 저장된 평가 응답과 변경 전 산출물은 그대로 유지했다.

## 변경

- 높은 위험 신호와 확실한 정책 우회는 종전대로 우선 처리한다. 그 다음 `OUT_OF_SCOPE`이고 주제 확신도가 0.60 이상이면 `BLOCK`한다. 이전에는 위험 점수 0.35~0.70의 불확실성 분기가 먼저 실행돼 명백한 범위 밖 질문에도 피트니스 질문을 구체화해 달라고 했다.
- 잘못된 JEV 응답은 원문·질문·키를 기록하지 않고 실패한 검증 단계만 오류 코드에 넣는다. 예: `INVALID_RESPONSE_JSON`, `INVALID_RESPONSE_TOPIC`, `INVALID_RESPONSE_USAGE`, `INVALID_RESPONSE_MEDICAL_DECISION`. 공개 오류 응답 `AI_POLICY_UNAVAILABLE / 503`과 답변 생성 중단은 유지한다. 기존 실호출의 `INVALID_RESPONSE` 17건은 세부 정보가 없어 소급 분류할 수 없다.

## 저장 응답 replay 결과

| 지표 | 실호출 당시 정책 | 판정 순서 수정 후 replay |
| --- | ---: | ---: |
| 유효 판정 | 983/1,000 | 983/1,000 |
| 기대 동작과 일치 | 942/983 (95.83%) | 960/983 (97.66%) |
| 제한 질문의 정확한 제한 동작 | 372/390 (95.38%) | 390/390 (100%) |
| 제한 질문을 `ALLOW`로 통과 | 0/390 | 0/390 |
| 정상 질문 오차단 | 0/396 | 0/396 |
| 정상 질문 불필요한 `CLARIFY` | 23/396 | 23/396 |
| 동작 macro-F1 | 0.9481 | 0.9787 |
| 평가 불가 | 17/1,000 | 17/1,000 |

변경된 판정은 정확히 18건(`S38-01`~`S38-18`)으로, 모두 같은 의미 family의 범위 밖 질문이다. `CLARIFY`에서 기대한 `BLOCK`으로 바뀌었고 나머지 982건은 그대로다. 18개의 독립적인 실제 사용자 질문에서 개선됐다는 뜻은 아니다. 새로운 오류 코드의 실공급자 분포, 실사용 질문 정확도, 생성 답변의 안전성, 운영 가용성은 이 replay에서 측정할 수 없다.

다음 명령으로 저장 응답만 재평가했다. `replay`에서는 원격 호출 지연이나 과금액을 산출하지 않는다.

```bash
./gradlew aiPolicyEval -PaiPolicyEval.mode=replay \
  -PaiPolicyEval.dataset=src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl \
  -PaiPolicyEval.allowDraft=true \
  -PaiPolicyEval.replay=docs/evaluations/ai-policy/jev-live-1000-v1/cases.jsonl \
  -PaiPolicyEval.runs=1 --no-daemon
```

검증은 JDK 21에서 `./gradlew checkstyleMain checkstyleTest test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --tests 'com.myfitness.ai.*' --no-daemon`, 위 replay, `./gradlew build --no-daemon` 순서로 실행했고 모두 통과했다. 전체 빌드의 테스트는 206개, 실패·오류·건너뜀은 0개였다. 정책 경계와 파서의 새 동작 테스트를 먼저 실패시킨 뒤 수정했고, 사용량 검증에서 음수 입력 token이 마지막 객체 생성 단계까지 통과하던 문제도 바로잡았다.
