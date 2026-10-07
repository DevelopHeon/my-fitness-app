# PR-007 Dashboard

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.


> 상태: 구현 완료

## 목표

Workout과 BodyRecord에 축적된 데이터를 사용자가 빠르게 이해할 수 있도록
숫자 요약과 추이 차트로 제공한다.

통계 계산은 Spring Boot에서 수행하며 프론트엔드는 계산 결과를 표시한다.

## 집계 대상

운동 통계에는 COMPLETED Workout만 포함한다.

IN_PROGRESS Workout은 운동 중 값이 계속 변경될 수 있으므로
Dashboard 횟수, Volume, PR, 추이에서 제외한다.

사용자 데이터는 X-User-Id 기준으로 분리한다.

## 기간 기준

비교 기간은 동일 길이의 rolling period를 사용한다.

- 최근 7일: 오늘 포함 최근 7일
- 직전 7일: 최근 7일 바로 이전 7일
- 최근 30일: 오늘 포함 최근 30일
- 직전 30일: 최근 30일 바로 이전 30일

이를 통해 월 중간이나 주 중간에도 현재 기간과 이전 기간을 동일한 일수로 비교한다.

## 운동 지표

Dashboard API는 다음 값을 제공한다.

- 최근 7일 Workout 횟수
- 최근 30일 Workout 횟수
- 최근 7일 Volume
- 직전 7일 Volume
- 7일 Volume 증감률
- 최근 30일 Volume
- 직전 30일 Volume
- 30일 Volume 증감률
- 최근 30일 일별 Volume
- 최근 30일 카테고리별 일별 Volume

Volume 계산:

~~~text
set volume = weightKg × reps
workout volume = Σ completed set volume
~~~

이전 기간 Volume이 0이면 무한대 형태의 증감률을 만들지 않고
증감률을 null로 응답한다.

## 날짜 정렬 기준

Dashboard의 모든 추이는 레코드 생성/등록 순서가 아니라 실제 기록 날짜를 사용한다.

- BodyRecord: measuredAt 오름차순, 동일 시각이면 id 오름차순
- 최신 Body 판단은 measuredAt 내림차순, 동일 시각이면 id 내림차순
- Workout / 종목 기록: workoutDate 오름차순
- 나중에 과거 날짜의 기록을 추가해도 차트는 실제 날짜 순서로 재정렬
- BodyRecord의 measuredAt은 Instant이므로 프론트 라벨은 브라우저 로컬 날짜로 변환

예를 들어 9월 17일 기록을 먼저 등록하고 이후 9월 16일 기록을 등록해도
차트는 9/16 → 9/17 순서로 표시한다.

## Body 지표

최신값과 이전값은 전체 BodyRecord에서 조회하고,
차트에 사용하는 history만 최근 90일로 제한한다.

- 최신 체중 / 체지방률 / 골격근량
- 최신 측정값과 바로 이전 측정값의 차이
- 최근 90일 체중 추이
- 최근 90일 체지방률 추이
- 최근 90일 골격근량 추이

BodyRecord가 없는 경우 latest/change는 null이고 history는 빈 배열이다.

## 종목별 지표

완료된 전체 Workout 기록을 기준으로 DEFAULT/CUSTOM + exerciseId 단위로 집계한다.

- 최고 중량
- 최고 추정 1RM
- 마지막 수행 날짜
- 최근 8회 수행 기록
  - Workout 날짜
  - Volume
  - 해당 수행 최고 중량
  - 해당 수행 최고 추정 1RM

추정 1RM은 Epley 공식을 사용한다.

~~~text
estimated 1RM = weight × (1 + reps / 30)
~~~

소수 둘째 자리까지 반올림한다.

## API

~~~text
GET /api/dashboard
~~~

Response:

- generatedDate
- workout
- body
- exercises

Dashboard 전용 집계 결과 DTO를 사용하고 JPA Entity는 노출하지 않는다.

## Frontend

상단 메뉴에 Dashboard를 추가하고 기본 진입 화면으로 사용한다.

화면 구성:

1. 최근 7일 / 30일 Workout 횟수
2. 최근 7일 / 30일 Volume 및 이전 기간 대비 변화
3. 최근 30일 일별 Volume 차트
   - 전체 / 가슴 / 어깨 / 등 / 팔 / 복근 / 하체 필터
   - 동일 카테고리 운동의 합산 Volume을 일자별로 표시
4. 최신 Body 지표 및 이전 측정 대비 변화
5. 최근 Body 추이 차트
6. 운동 종목 선택
7. 종목별 최고 중량 / 최고 추정 1RM
8. 최근 수행 중량 / 추정 1RM 추이
9. 최근 사용 종목 PR 요약

차트는 정적 export 구조를 유지하기 위해 클라이언트 SVG로 렌더링하며
별도 Next.js 서버 기능을 사용하지 않는다.

## 테스트

핵심 계산 규칙:

- weight × reps Volume
- Epley 추정 1RM
- 이전 기간 대비 Volume 증감률
- 이전 기간 0 Volume 처리

API 통합:

- 최근 7일 / 30일 Workout 횟수
- 기간별 Volume
- Body 최신값 / 변화량
- 종목 최고 중량 / 추정 1RM
- 종목 최근 기록
- 사용자 데이터 격리

테스트 메서드는 영문 camelCase,
모든 테스트는 @DisplayName으로 검증 의도를 기술한다.
