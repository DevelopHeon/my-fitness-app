# ADR 0002 · JEV 단일 정책 경로와 평가 실패 차단

- 상태: Accepted
- 정리일: 2026-10-07. 2026-09-28~30 설계·변경·실측 기록을 근거로 작성한 회고 기록이다.

## 배경과 대안

기존 키워드·주제 Router는 의미·위험·우회 요청을 충분히 구분하지 못했다.
고정한 합성 1,000건 기준선에서 제한 질문 400건 중 340건을 허용했고 정상 질문 400건 중 40건을 차단했다.
키워드 보완, JEV 후보를 기록하는 shadow, 생성 전 JEV 판정을 강제하는 경로를 검토했다.
여러 운영 모드는 candidate/effective 판정·분기·설정과 장애 우회를 복잡하게 만들었다.

## 결정

정상 텍스트 요청은 답변 생성 전에 JEV를 한 번 평가한다. ALLOW만 생성으로 진행한다.
BLOCK·SAFE_REDIRECT·CLARIFY는 서버 정책 안내를 반환한다. 평가 불가는 AI_POLICY_UNAVAILABLE / 503으로 종료한다.
legacy/shadow·키워드 fallback을 운영에서 제거하고 외부 호출 중 DB transaction을 유지하지 않는다.
현재 임계값·판정 순서·오류 계약은 [AI](../ai.md)에 둔다.

## 결과와 비용

의미에 기반한 판정과 장애 시 생성 차단이 가능하지만 외부 평가의 지연·비용·가용성에 의존한다.
실호출 1,000건은 유효 판정 983건, 평가 불가 17건이었다. 합성 50 family의 표현 변형이며 독립 사용자 질문이 아니다.
이 측정은 입력 gate만 평가했고 실제 답변의 안전성이나 운영 가용성을 입증하지 않는다.
범위 밖 판정 순서 수정은 저장 응답 replay로 평가했다. 모델 재실험이나 새 정확도 측정으로 표현하지 않는다.
confidence를 정답 확률로 해석하지 않으며 정상 질문의 CLARIFY는 실제 사용을 관측한 뒤 개선 여부를 판단한다.

## 근거

- [최초 설계와 비교 방법](../archive/specs/2026-09-28-ai-policy-validation-spec.md)
- [단일 경로 계약](../archive/specs/2026-09-29-jev-single-path-spec.md)
- [변경 전 기준선](../evaluations/ai-policy/baseline-1000-v1/report.md)
- [JEV 실호출 결과와 한계](../evaluations/ai-policy/jev-live-1000-v1/report.md)
- [저장 응답 replay](../evaluations/ai-policy/policy-rule-replay/report.md)
