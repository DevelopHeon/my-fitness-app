# PR-004 Exercise Catalog / Workout History UX

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.


> 상태: 구현 완료

## 1. 목표

기본 운동 종목과 사용자 커스텀 운동 종목을 분리하고,
운동 기록 화면을 카테고리 중심 선택 흐름으로 변경한다.

## 2. Exercise 데이터 구조

### exercises
- 모든 사용자에게 제공하는 공용 기본 운동 카탈로그
- user_id 없음
- name / category / sort_order 저장
- Flyway V3에서 한글 운동명 seed

### custom_exercises
- 사용자가 직접 추가한 운동 종목
- user_id로 소유권 분리
- name / category / created_at 저장

카테고리:
- CHEST: 가슴
- SHOULDER: 어깨
- BACK: 등
- ARM: 팔
- ABS: 복근
- LEGS: 하체

## 3. 기록 참조 방식

기본/커스텀 테이블의 PK는 서로 겹칠 수 있으므로
WorkoutExercise와 RoutineExercise는 다음 식별자를 함께 저장한다.

- exercise_type: DEFAULT / CUSTOM
- exercise_id
- exercise_name snapshot
- category snapshot

기록 시점의 이름과 카테고리를 스냅샷으로 보존해
커스텀 운동 정의가 이후 변경되어도 과거 기록은 영향을 받지 않는다.

## 4. Flyway

- V1: Workout 기반 테이블
- V2: Routine 테이블
- V3: 기본/커스텀 Exercise 분리 및 기본 운동 seed

V3는 기존 사용자별 exercises 데이터를 custom_exercises로 이관하고
기존 Workout/Routine 운동 참조를 snapshot 구조로 변환한다.

MVP 개발 완료 전까지 application.yml의
Hibernate ddl-auto: create-drop은 유지한다.

create-drop 개발 환경에서는 Hibernate가 Flyway seed 이후 스키마를
재생성할 수 있으므로 기본 운동 카탈로그 initializer가 누락된 seed를 보충한다.
MVP 이후 ddl-auto를 비활성화하면 Flyway migration을 기준으로 운영한다.

## 5. 화면 변경

Workout / Routine 공통:
- 가슴 / 어깨 / 등 / 팔 / 복근 / 하체 카테고리 버튼
- 카테고리 클릭 후 해당 운동 종목 선택
- 기본 운동과 내 운동 표시 구분
- 커스텀 운동 등록 시 카테고리 지정

Workout:
- 완료된 최근 운동 기록 클릭 시 상세 토글
- 수행 운동과 세트 기록 확인

입력 제한:
- 이름 입력은 text 입력 + 숫자 문자 제거
- 중량은 숫자/소수만 허용
- 횟수/시간은 정수 숫자만 허용
- 운동 날짜는 date input 사용

## 6. API 변경

- GET /api/exercises
  - 기본 운동 + 현재 사용자의 커스텀 운동 반환
- POST /api/exercises/custom
  - 현재 사용자의 커스텀 운동 생성
- GET /api/exercises/{exerciseType}/{exerciseId}/previous-record
- Workout 운동 추가 요청에 exerciseType + exerciseId 사용
- Routine 운동 목록도 exerciseType + exerciseId로 저장

## 7. 테스트 기준

- 기본 운동 카탈로그가 모든 사용자에게 제공된다.
- 커스텀 운동은 생성한 사용자에게만 노출된다.
- DEFAULT/CUSTOM의 동일 id가 서로 다른 운동으로 처리된다.
- 이전 기록 조회는 exerciseType + exerciseId 기준이다.
- Routine에서 기본/커스텀 운동을 함께 사용할 수 있다.
- 모든 테스트 메서드는 영문이고 @DisplayName으로 목적을 표현한다.
