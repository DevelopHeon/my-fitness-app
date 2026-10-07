# AI 처리 계약

텍스트 정책 평가·답변 생성·사진 분석의 현재 계약이다.
사용자 흐름은 [Product](product.md), 구조 규칙은 [Architecture](architecture.md),
환경변수와 배포 설정은 [Infrastructure](../infra/README.md#설정과-비밀값)를 따른다.

## 텍스트 요청

```text
소유권·입력 확인 → 사용자 메시지 커밋 → JEV 평가 → 최종 주제 저장
  ALLOW → 개인 Context → 허용 이력 → 답변 생성 → 답변·요청 로그 저장
  BLOCK / SAFE_REDIRECT / CLARIFY → 정책 안내 저장·반환
  평가 실패 → 실패 로그 저장 → AI_POLICY_UNAVAILABLE / 503
```

정상 텍스트 요청은 JEV를 반드시 거친다. 운영 모드 선택, legacy/shadow, 키워드 fallback은 없다.
제한과 평가 실패에서 개인 Context와 답변 생성기를 호출하지 않는다.
문자열 version 변경은 실행 규칙을 복원하거나 모델 품질을 보증하지 않는다.

`AiCoachService`는 In Port 구현과 대화/메시지 위임을 담당한다.
`AiChatMessageService`가 텍스트 처리 순서를 조정하고 `AiPolicyGuard`가 성공/실패를 반환한다.
HTTP·wire 검증은 `AiPolicyGateway` Out Port 뒤의 `JevAiPolicyGateway`에 있다.
`AiPolicyRun`은 필수 값이 있는 Success와 타입이 지정된 정책 예외를 보존하는 Failure로 표현한다.

## 정책 평가 기준

JEV 요청은 질문·현재 화면 힌트·최근 허용 대화와 서버가 정의한 평가 항목으로 구성한다.
주제는 WORKOUT, NUTRITION, BODY, GENERAL_FITNESS, COMPOSITE, OUT_OF_SCOPE, AMBIGUOUS다.
의료 판단·위험 행동·긴급 위험 신호·정책 우회 평가와 주제 분포·confidence를 검증한다.
confidence를 정답 확률로 해석하지 않는다.

현재 기본값은 review 0.35, action 0.70, topic confidence 0.60이다.
`AiPolicyEvaluator`는 다음 순서로 판정한다.

| 순서 | 조건 | 결과 |
| --- | --- | --- |
| 1 | 긴급 위험 신호 ≥ review | SAFE_REDIRECT |
| 2 | 의료 판단 또는 위험 행동 ≥ action | SAFE_REDIRECT |
| 3 | 정책 우회 ≥ action | BLOCK |
| 4 | OUT_OF_SCOPE이고 confidence ≥ topic 기준 | BLOCK |
| 5 | 의료 판단·위험 행동·우회 중 하나가 ≥ review | CLARIFY |
| 6 | AMBIGUOUS 또는 confidence가 topic 기준 미만 | CLARIFY |
| 7 | 위 조건에 해당하지 않음 | ALLOW |

정책 안내는 서버가 구성한다. 모델의 자유 문장이나 명령을 실행하지 않는다.
정확한 문구·판정 변경의 근거는 [정책 순서 replay](evaluations/ai-policy/policy-rule-replay/report.md)에 보존한다.

## 실패와 답변 생성

JEV의 key/설정 누락, timeout, HTTP·통신 오류, 모델 불일치, 응답 검증 실패는
`AI_POLICY_UNAVAILABLE / 503`으로 종료한다. 답변 생성으로 우회하지 않으며 자동 재시도하지 않는다.
정책 오류는 `AiPolicyUnavailableException.Code`, 답변·사진 공급자 오류는
`AiProviderUnavailableException.Code` enum으로 생성·전달한다. 지표용 문자열 whitelist를 중복 관리하지 않는다.
응답 검증 실패는 `INVALID_RESPONSE_` 뒤에 검증 단계만 기록한다. 원문 질문·응답·키를 오류 로그에 넣지 않는다.

허용 뒤 답변은 Spring AI의 선택된 ChatModel로 생성한다. Provider 미설정·빈 응답·생성 실패는
`AI_PROVIDER_UNAVAILABLE / 503`이며 정책 평가 실패와 구분한다.
채팅의 모델 미설정은 `CONFIGURATION_ERROR`, 빈 응답은 `INVALID_RESPONSE`로 관측한다.
DB·평가 보고서에서는 enum을 문자열로 변환하며 기존 로그를 재작성하지 않는다.
JEV HTTP 오류는 로그에 `HTTP_429`처럼 상태를 보존하고 지표에서는
`HTTP_4XX/HTTP_5XX/HTTP_OTHER`로 묶는다. 알 수 없는 채팅 생성 오류의 지표는 `OTHER`다.
모델·timeout 기본값은 [설정 계약](../infra/README.md#앱-환경-기본값), 로컬 Provider 주입은 [개발 가이드](backend/README.md#ai와-로그인-설정)를 따른다.

## Context·이력·저장

- 다른 모듈은 공개 insight In Port로 조회한다. 서버가 계산한 집계 Context만 필요한 범위로 전달한다.
- 허용 이력은 SUCCESS+ALLOW 또는 과거 SUCCESS+null 로그의 실제 질문·답변 ID로 짝을 구성한다.
  제한·명확화·실패·사진 턴은 텍스트 허용 이력에서 제외한다.
- JEV에는 최근 두 허용 쌍·2,000자 이내를 전달한다. 답변 생성 이력은 별도 제한 설정을 따른다.
- 정책/생성 외부 호출 중 DB transaction을 유지하지 않는다. 사용자 메시지·결과·로그는 짧은 별도 transaction에서 저장한다.
- 모델·정책 버전·판정·근거·확률·오류·지연·알려진 token usage를 기존 요청 로그에 남긴다.
- 과거 migration과 로그를 보존한다. `policy_mode`는 호환/감사 정보이며 운영 선택 설정이 아니다.
  candidate/effective 이중 판정과 중복 JSON 저장은 없다.
- 초기 OUT_OF_SCOPE는 DB의 미판정 placeholder다. 성공한 평가의 주제로 갱신하고 실패 턴은 허용 이력에서 제외한다.
  AMBIGUOUS의 저장 주제와 CLARIFY 정책 결정을 구분한다.

## 음식 사진 분석

사진은 `POST /api/ai/conversations/{id}/food-photos`의 `image` 한 part로 전송한다.
임의 질문·prompt·URL·userId를 사진 입력 계약에 추가하지 않는다.
UI의 진입점은 식단 메뉴지만 서버의 소유권·파일 검증은 화면 위치에 의존하지 않는다.

```text
소유권 확인 → 파일 검증·정규화 → 고정 사용자 메시지 저장
  → OpenAI 분석 1회 → 구조화 결과 검증 → 결과 대화·요청 로그 저장
  → 사용자가 식단 입력을 수정·저장
```

사진은 JEV 텍스트 평가를 호출하지 않는다. OpenAI 이미지 입력과 Structured Outputs로
음식 판별·일반적인 1인분 추정을 한 번 요청하며 Ollama fallback이나 자동 재시도는 없다.
전처리는 원본 크기·형식을 검증한 뒤 ImageIO subsampling으로 디코딩 메모리를 줄이고
기존 최대 edge 1600 JPEG로 정규화한다. 5MiB·3,200만 픽셀 입력 제한을 축소하지 않는다.

| 입력·결과 | 계약                                                         |
| --- |------------------------------------------------------------|
| 원본 | JPEG/PNG 한 장, 5 MiB·3,200만 픽셀 이하, 전체 multipart 요청 6 MiB 이하 |
| 정규화 | 최대 긴 변 1,600px의 JPEG로 재인코딩, 원본 metadata 제거                 |
| FOOD | 1~5개 검증된 음식 후보                                             |
| NOT_FOOD / UNCERTAIN | 빈 후보, 정상 판별 결과                                             |
| 잘못된 이미지·픽셀 제한 | 400 INVALID_FOOD_PHOTO                                     |
| 파일·전체 요청 크기 제한 | 413 PAYLOAD_TOO_LARGE                                      |
| 지원하지 않는 포맷·MIME 불일치 | 415 UNSUPPORTED_MEDIA_TYPE                                 |
| 설정·통신·응답 오류·refusal·미완성 | 503 AI_PROVIDER_UNAVAILABLE                                |

`FoodPhotoService`는 순서를 조정하며 `FoodPhotoTransactionService`는 저장을 담당한다.
이미지 처리·응답 해석은 Infrastructure 협력 클래스에 있다. 파일 검증 실패는 대화·제목·로그를 저장하기 전에 반환한다.
분석은 Nutrition에 쓰지 않고 Frontend가 초안을 전달한 뒤 사용자의 저장으로 식단 유스케이스를 호출한다.

앱은 원본 사진·base64·파일명·모델 원문을 DB나 로그에 보관하지 않는다.
업로드 임시 파일은 처리 종료 시 정리한다. 공급자 측 보관 정책의 삭제까지 보장하는 계약은 아니다.
대화에는 고정 요청과 검증된 결과만 저장하고 `messageKind`·`requestKind`로 사진 요청을 구분한다.

## 관측과 평가의 구분

`AiMetrics`는 정책 결정/장애, chat/photo 결과/장애, 지연과 관측한 input/output token을 기록한다.
제한과 비음식 판별은 정상 결정이다. DB 저장 실패를 외부 호출 실패로 중복 집계하지 않는다.
usage 누락과 Spring AI EmptyUsage는 미기록이며 명시한 0과 구분한다.
변동 모델명·사용자 ID·질문·음식명·키·예외 원문을 metric label에 넣지 않는다.

대시보드 분포와 테스트 대역은 실제 모델 정확도나 공급자 과금이 아니다.
변경 전 baseline, JEV 실호출, 저장 응답 replay는 [평가 자료](../README.md#정량-평가)를 각각 참조한다.
독립적인 사용자 질문·사람 정답·E2E 안전성·사진 열량 정확도와 기존 합성 입력 평가를 구분한다.

## 근거 코드

- [정책 평가 순서](../src/main/java/com/myfitness/ai/application/support/policy/AiPolicyEvaluator.java)
- [JEV 연동](../src/main/java/com/myfitness/ai/infrastructure/client/JevAiPolicyGateway.java)
- [텍스트 조율](../src/main/java/com/myfitness/ai/application/service/AiChatMessageService.java)
- [사진 조율](../src/main/java/com/myfitness/ai/application/service/FoodPhotoService.java)
- [설정 기본값](../src/main/resources/application.yml)
