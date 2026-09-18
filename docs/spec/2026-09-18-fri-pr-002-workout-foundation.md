# PR-002 Workout 기반 설계

- 날짜: 2026-09-18
- 요일: 금요일
- 상태: 설계 완료 / 구현 예정

## 1. 배경 및 목적

MVP의 첫 기능인 Workout 기록을 구현하기 위한 데이터 모델과 API 경계를 확정한다.

## 2. 구현 범위

- 운동 종목(Exercise) 조회 및 등록
- Workout 생성/조회/완료
- Workout 내 운동 종목 추가/삭제/순서 관리
- 세트 기록 추가/수정/삭제
- 특정 종목의 직전 기록 조회
- 최근 Workout 목록 조회

## 3. 핵심 데이터 모델

```text
User 1 --- N Workout
Workout 1 --- N WorkoutExercise
Exercise 1 --- N WorkoutExercise
WorkoutExercise 1 --- N WorkoutSet
```

### Exercise
- id
- userId: 사용자 정의 종목일 때 소유자, 공통 종목은 null 허용 여부 추후 결정
- name
- category
- createdAt

### Workout
- id
- userId
- workoutDate
- status: IN_PROGRESS / COMPLETED
- memo
- startedAt
- completedAt

### WorkoutExercise
- id
- workoutId
- exerciseId
- orderIndex
- memo

### WorkoutSet
- id
- workoutExerciseId
- setNumber
- weightKg
- reps
- durationSeconds: 유산소/시간 운동 확장을 위한 선택값
- completed

## 4. API 초안

- POST /api/workouts
- GET /api/workouts/{workoutId}
- GET /api/workouts?from=&to=
- PATCH /api/workouts/{workoutId}/complete
- POST /api/workouts/{workoutId}/exercises
- DELETE /api/workouts/{workoutId}/exercises/{workoutExerciseId}
- POST /api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets
- PATCH /api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}
- DELETE /api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}
- GET /api/exercises
- POST /api/exercises
- GET /api/exercises/{exerciseId}/previous-record

## 5. 완료 조건

- 한 번의 운동 세션을 생성하고 여러 종목/세트를 기록할 수 있다.
- 운동을 완료 상태로 변경할 수 있다.
- 동일 종목을 다시 수행할 때 직전 Workout 기록을 조회할 수 있다.
- 다른 사용자의 Workout/Set에는 접근할 수 없다.

## 6. 제외 범위

- Routine
- Dashboard 집계
- PR/1RM 통계
- Nutrition
- AI 분석
