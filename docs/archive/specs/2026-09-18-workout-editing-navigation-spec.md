# PR-007 Workout Editing / Navigation

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.


> 상태: 구현 완료

## 목표

Workout 기록 중 탐색과 완료 기록 수정 흐름을 개선하고,
입력 화면에 이전 작성값이 의도치 않게 남지 않도록 초기화 정책을 명확히 한다.

## 기록 중 이전 화면

진행 중 Workout 화면에 `← 이전 화면` 버튼을 제공한다.

- 뒤로 이동해도 DB의 진행 중 Workout은 삭제하지 않는다.
- 일별 조회 화면에 진행 중 Workout을 표시한다.
- `운동 기록 이어서 작성` 또는 기록 행의 `이어하기`로 복귀한다.
- 진행 중 Workout이 있으면 새 Routine / 빈 Workout 시작은 막는다.

## 완료 기록 수정

완료된 Workout은 직접 변경하지 않는다.

1. 완료 기록의 `수정` 선택
2. `PATCH /api/workouts/{workoutId}/reopen`
3. 상태를 COMPLETED → IN_PROGRESS로 전환
4. completedAt 초기화
5. 기존 종목 / 세트 수정
6. 다시 운동 완료

이 방식으로 완료 후 임의 변경을 막는 기존 도메인 규칙과
사용자의 과거 기록 수정 요구를 함께 만족시킨다.

## 입력 초기화

Workout:
- 새 세트 입력 행은 항상 빈 중량 / 횟수로 시작한다.
- 직전 완료 기록은 참고용으로만 표시한다.
- Workout 완료 / 이전 화면 / 새 Workout 시작 시 임시 입력 상태를 초기화한다.
- 저장된 세트 수정 모드도 작업 종료 시 초기화한다.

Routine:
- 루틴 저장 / 수정 취소 시 이름과 선택 운동 목록을 초기화한다.
- ExercisePicker의 선택 카테고리도 초기화한다.
- CustomExerciseForm의 이름 / 카테고리도 등록 후 초기화한다.

## Workout 조회 구조

Calendar를 상위 메뉴에서 제거한다.

상위 메뉴:
- Workout
- Routine
- Body

Workout 내부:
- 일별 조회
- 캘린더 조회

캘린더 날짜를 누르면 해당 날짜를 선택한 일별 조회로 이동한다.

## 테스트

Domain:
- 완료 Workout 재오픈
- 재오픈 후 수정 가능
- 수정 후 재완료 가능

API Integration:
- 완료 API
- reopen API
- reopen 후 세트 수정
- 다시 완료 후 수정 값 유지

전체 backend test, frontend lint/build, bootJar를 통과해야 한다.
