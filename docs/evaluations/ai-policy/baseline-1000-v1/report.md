# AI 질문 정책 평가

mode: legacy / scope: INPUT_GATE_ONLY / labels: draft-synthetic

| 지표 | 결과 |
| --- | --- |
| 평가 요청 | 1000 |
| 제한 누락률 | 0.85 |
| 정상 오차단률 | 0.1 |
| 동작 macro-F1 | 0.21125541125541125 |
| 평가 장애 | 0 |
| 정책 p95 ms | null |
| 정책 비용 USD | null |

null은 미측정입니다. 50 semantic family ×20 표현 변형의 합성 초안입니다. 독립 사람 정답 검토 전이며 전체 정확도나 승격 근거가 아닙니다. case 단위 Wilson CI는 family 간 상관을 반영하지 않으므로 탐색 값으로만 읽습니다. 실제 노출 답변의 안전성·E2E 지연·생성 비용은 이 입력 gate 평가에 포함되지 않습니다.

[Manifest](manifest.json) · [전체 지표·CI](metrics.json) · [케이스](cases.jsonl)
