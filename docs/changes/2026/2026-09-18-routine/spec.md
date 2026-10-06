# PR-003 Routine

> Changes 기록: 정리 전 문서의 설계·상태·검증을 보존한 이력이다.
> 현재 계약과 실행 방법은 [현재 문서](../../../README.md)를 따른다. 아래 계획과 결과는 현재 배포 상태를 증명하지 않는다.

> 상태: 구현 완료

## 1. 목표

자주 사용하는 운동 조합과 순서를 Routine 템플릿으로 저장하고, Routine 선택만으로 새로운 Workout을 시작할 수 있게 한다.

## 2. 구현 범위

### Backend
- Routine / RoutineExercise 도메인 및 JPA 매핑
- Routine 생성/조회/수정/삭제
- 운동 종목 순서 저장 및 수정
- 공용 기본 운동 및 현재 사용자의 커스텀 운동만 Routine에 포함
- 동일 Routine 내 운동 종목 중복 방지
- Routine으로 Workout 시작
- Routine 순서대로 WorkoutExercise 생성
- Workout 시작 응답에 종목별 직전 완료 기록 포함
- 사용자별 Routine 접근 격리

### API
- POST /api/routines
- GET /api/routines
- GET /api/routines/{routineId}
- PUT /api/routines/{routineId}
- DELETE /api/routines/{routineId}
- POST /api/routines/{routineId}/workouts

### Frontend
- Workout / Routine 화면 전환
- Routine 내부 메뉴를 조회 / 등록·수정으로 분리
- 기본 진입은 Routine 조회
- Routine 목록
- Routine 생성/수정/삭제
- 목록에서 수정 선택 시 등록·수정 메뉴로 자동 전환
- 저장 또는 수정 취소 후 조회 메뉴로 복귀
- 등록·수정 화면은 루틴 이름 → 선택된 운동 목록 → 운동 종목 선택 순서로 배치
- 선택된 운동 목록을 이름 바로 아래에 노출해 현재 구성을 즉시 확인
- 카테고리 선택 후 운동 종목을 추가해도 사용자가 선택한 카테고리 탭을 유지
- 커스텀 Exercise 등록 후 Routine에 즉시 추가
- Routine 운동 종목 순서 위/아래 변경
- Routine 화면에서 Workout 시작
- Workout 시작 화면에서도 저장된 Routine을 선택해 바로 Workout 시작
- Workout 화면 전환 즉시 직전 기록 표시
- Routine 시작 후에도 기존 Workout 종목 추가/삭제 및 세트 기록 기능 유지

## 3. Persistence

DDL 변경은 Flyway로 관리한다.

- V2__create_routine_tables.sql
- routines
- routine_exercises
- 사용자/수정일 조회 인덱스
- Routine 삭제 시 RoutineExercise cascade 삭제

메인 실행 환경은 Hibernate ddl-auto: none을 사용하고 스키마 변경은 Flyway로만 관리한다.

## 4. 테스트

테스트 메서드명은 영문 camelCase, 테스트 설명은 @DisplayName으로 작성한다.

포함 테스트:
- Routine 운동 순서 보존
- 타 사용자 Exercise 추가 거부
- 빈 Routine 생성 거부
- Routine 생성/수정/목록 조회
- 저장 순서 기반 Workout 시작
- 직전 완료 기록 반환
- 사용자별 Routine 접근 격리
- Routine 삭제

## 5. 완료 기준

- Routine 선택 후 별도 운동 종목 검색 없이 Workout을 시작할 수 있다.
- Routine에 저장한 순서가 Workout에 그대로 적용된다.
- 시작 시 해당 종목의 직전 완료 기록을 확인할 수 있다.
- Routine 수행 중 기존 Workout 편집 기능을 그대로 사용할 수 있다.
- 전체 backend test, frontend lint/build, bootJar가 통과한다.
