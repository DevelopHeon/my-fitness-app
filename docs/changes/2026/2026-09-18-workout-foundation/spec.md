# PR-002 Workout 기반 설계

> Changes 기록: 정리 전 문서의 설계·상태·검증을 보존한 이력이다.
> 현재 계약과 실행 방법은 [현재 문서](../../../README.md)를 따른다. 아래 계획과 결과는 현재 배포 상태를 증명하지 않는다.

- 날짜: 2026-09-18
- 요일: 금요일
- 상태: 구현 완료

## 1. 배경 및 목적

MVP의 첫 기능인 Workout 기록을 구현하기 위한 데이터 모델과 API 경계를 확정한다.

## 2. 구현 범위

- 공용 기본 운동 종목(Exercise) 조회
- 사용자 커스텀 운동 종목(CustomExercise) 등록/조회
- Workout 생성/조회/완료
- Workout 내 운동 종목 추가/삭제/순서 관리
- 세트 기록 추가/수정/삭제
- 특정 종목의 직전 기록 조회
- 최근 Workout 목록 조회

## 3. 핵심 데이터 모델

```text
User 1 --- N Workout
Workout 1 --- N WorkoutExercise
WorkoutExercise 1 --- N WorkoutSet

Exercise        # 공용 기본 운동
CustomExercise  # 사용자별 커스텀 운동
        ↓
exerciseType + exerciseId
        ↓
WorkoutExercise snapshot
```

### Exercise
- id
- name
- category
- sortOrder
- userId 없음

### CustomExercise
- id
- userId
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
- exerciseType: DEFAULT / CUSTOM
- exerciseId
- exerciseName snapshot
- category snapshot
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
- POST /api/exercises/custom
- GET /api/exercises/{exerciseType}/{exerciseId}/previous-record

## 5. 테스트 요구사항

Workout 구현 시 비즈니스 규칙을 자동 테스트로 검증한다.

필수 테스트 후보:
- Workout 생성 시 IN_PROGRESS 상태로 시작한다.
- 운동 종목과 세트를 순서대로 추가할 수 있다.
- 세트의 중량/횟수 수정 및 삭제 결과가 올바르다.
- Workout 완료 시 COMPLETED 상태와 완료 시각이 기록된다.
- 완료된 Workout에 허용되지 않은 변경을 시도하면 거부한다.
- 특정 종목의 이전 기록 조회 시 가장 최근 완료 Workout을 기준으로 반환한다.
- 다른 사용자의 Workout/WorkoutExercise/WorkoutSet 접근을 거부한다.
- 빈 기록, 잘못된 식별자 등 경계 조건을 검증한다.

Repository 쿼리가 포함되는 이전 기록 조회는 JPA 통합 테스트를 작성하고, 핵심 REST 흐름은 API 통합 테스트를 포함한다.

## 6. 완료 조건

- 한 번의 운동 세션을 생성하고 여러 종목/세트를 기록할 수 있다.
- 운동을 완료 상태로 변경할 수 있다.
- 동일 종목을 다시 수행할 때 직전 Workout 기록을 조회할 수 있다.
- 다른 사용자의 Workout/Set에는 접근할 수 없다.
- 위 비즈니스 규칙을 검증하는 자동 테스트가 존재하고 전체 테스트가 통과한다.
- 프론트 변경이 있으면 lint/build가 통과하고 통합 bootJar가 성공한다.

## 7. 제외 범위

- Routine
- Dashboard 집계
- PR/1RM 통계
- Nutrition
- AI 분석

## 8. 구현 결과

### Backend
- Exercise 등록/목록 조회
- Workout 생성/단건 조회/최근 30일 목록 조회/완료
- WorkoutExercise 추가/삭제
- WorkoutSet 추가/수정/삭제
- 완료된 Workout 변경 차단
- 다른 사용자의 Workout 접근 차단
- 동일 종목의 가장 최근 완료 Workout 기록 조회
- Flyway 기반 Workout 스키마 생성
- 개발용 Next.js(3000) → Spring Boot(8080) CORS 허용

인증 기능은 아직 구현 전이므로 Phase 1에서는 `X-User-Id` 헤더를 임시 사용자 컨텍스트로 사용한다. 실제 인증 구현 시 Controller 입력이 아닌 Spring Security 인증 사용자로 교체한다.

### 구조
Controller는 `WorkoutApplicationService` 하나만 주입받는다.

```text
controller
    ↓
WorkoutApplicationService
    ↓
WorkoutService / ExerciseService
    ↓
Domain + Repository
```

Workout 내부 패키지는 실용적인 역할 단위로 구성했다.

```text
workout/
├── controller
├── dto/
│   ├── request
│   └── response
├── service
├── domain
├── repository
└── exception
```

HTTP 예외 변환은 도메인별 Handler가 아니라 `common.exception.GlobalExceptionHandler`에서 일괄 처리한다.

### Frontend
- 날짜 선택 후 운동 시작
- 가슴/어깨/등/팔/복근/하체 카테고리 선택 후 운동 종목 선택
- 공용 기본 운동과 사용자 커스텀 운동 구분 표시
- 커스텀 운동 등록 시 카테고리 지정 후 현재 Workout에 즉시 추가
- Workout에 운동 종목 추가/삭제
- 종목 추가 직후 세트 기록 카드로 이동
- 이전 완료 기록 표시
- 목표 세트 수 지정 후 해당 수만큼 중량/횟수 입력 행 생성
- 여러 세트 일괄 저장 및 세트 수정/삭제
- 기존 시간형 세트 기록 조회 호환 유지
- 운동 완료
- 선택 날짜의 Workout만 조회
- 완료 Workout 클릭 시 수행 운동/세트 상세 토글
- 월간 Calendar에서 운동 날짜와 수행 종목 요약 후 날짜 상세 이동
- 텍스트/숫자/날짜 입력을 각 입력 의미에 맞게 제한
- 모바일 우선의 단순하고 깔끔한 카드 UI

## 9. 자동 테스트 결과

Phase 1 완료 시 다음 테스트를 포함하고 전체 테스트 통과를 확인했다.

- Domain: Workout 상태/소유권/완료 후 변경 제한/세트 유효성/세트 순서
- Service: 다른 사용자 Workout 접근 제한
- Repository: 가장 최근 완료 운동 기록 조회
- API Integration: 운동 생성 → 종목 추가 → 세트 기록/수정/삭제 → 시간형 세트 → 완료 → 이전 기록 조회 및 사용자 격리

검증 명령:
- `./gradlew test --no-daemon`
- `npm run lint`
- `npm run build`
- `./gradlew bootJar --no-daemon`
