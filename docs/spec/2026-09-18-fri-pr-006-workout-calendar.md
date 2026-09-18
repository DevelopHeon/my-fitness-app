# PR-006 Workout Set Planning / Calendar

> 상태: 구현 완료

## 목표

Workout 기록 입력을 세트 수 중심으로 단순화하고,
일 단위 기록 화면과 월 단위 탐색 화면을 분리한다.

## 세트 입력

기존의 중량 / 횟수 / 시간 직접 1세트 추가 흐름 대신
총 세트 수를 먼저 지정한다.

예:
- 총 세트 수 4 입력
- 1~4세트 중량 / 횟수 입력 행 생성
- 저장 전 각 세트 입력 행을 개별 삭제 가능
- 삭제 시 남은 입력 행 기준으로 총 세트 수 자동 조정
- 한 번에 저장

이미 2세트가 저장된 상태에서 총 세트 수를 4로 바꾸면
3~4세트 입력 행만 추가한다.

직전 완료 기록이 있으면 동일 세트 번호의 중량과 횟수를
새 입력 행의 초깃값으로 사용한다.

시간형 WorkoutSet 데이터 모델과 기존 기록 조회 호환성은 유지하지만
새 근력 운동 입력 UI에서는 중량 / 횟수 입력을 기본으로 한다.

## Batch API

POST /api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/batch

- 한 요청에서 최대 20세트
- 하나의 트랜잭션에서 세트 추가
- 기존 WorkoutSet 비즈니스 규칙 재사용

## Workout 일 단위 화면

기본 Workout 화면은 선택한 날짜의 Workout만 조회한다.

Workout이 진행 중이지 않을 때는 저장된 Routine 목록을 먼저 보여주고,
Routine을 선택하면 해당 날짜로 운동 종목/순서를 복사한 Workout을 시작한다.
각 종목의 직전 완료 기록도 함께 불러와 세트 입력 초깃값으로 사용한다.
Routine 없이 시작하는 빈 Workout 옵션도 함께 제공한다.

- 기본 날짜: 오늘
- 날짜 직접 선택 가능
- 선택 날짜 Workout 시작
- 해당 날짜의 진행 중 / 완료 Workout만 표시
- 완료 Workout 클릭 시 운동 / 세트 상세 토글

## 월간 Calendar

상단에 Calendar 화면을 별도로 추가한다.

GET /api/workouts/calendar?month=YYYY-MM

날짜별 응답:
- workoutCount
- completedCount
- exerciseNames

Calendar:
- 이전 / 다음 월 이동
- 오늘 이후 날짜 선택 차단
- 운동한 날짜에 수행 운동명 최대 2개 요약
- 추가 운동은 +N개 표시
- 완료 운동 수 / 전체 운동 수 표시
- 날짜 클릭 시 해당 날짜 Workout 화면으로 이동

## 테스트

- 여러 세트 batch 저장
- 세트 번호 연속성
- 월간 Calendar 날짜 요약
- 완료 Workout 수 집계
- 운동명 요약
- 기존 Workout API 회귀 테스트

테스트 메서드명은 영문 camelCase,
각 테스트는 @DisplayName으로 목적을 기술한다.
