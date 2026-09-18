# PR-005 BodyRecord

> 상태: 구현 완료

## 목표

체중, 체지방률, 골격근량을 측정 시각 기준으로 누적하고
최근 측정값과 직전 기록 대비 변화를 확인할 수 있게 한다.

## Backend

- BodyRecord 도메인/JPA 구현
- 사용자별 소유권 분리
- 같은 날 여러 건 기록 허용
- 생성 / 목록 / 단건 / 수정 / 삭제
- 측정 시각 최신순 조회
- 최근 기록과 직전 기록의 변화량 계산
- 미래 측정 시각 등록 차단

필수 측정값:
- weightKg
- bodyFatPercentage
- skeletalMuscleKg
- measuredAt

선택값:
- memo

## API

- POST /api/body-records
- GET /api/body-records?from=&to=
- GET /api/body-records/{bodyRecordId}
- PUT /api/body-records/{bodyRecordId}
- DELETE /api/body-records/{bodyRecordId}
- GET /api/body-records/trend?days=90

## Persistence

Flyway V4__create_body_records.sql에서 관리한다.

body_records:
- user_id
- weight_kg
- body_fat_percentage
- skeletal_muscle_kg
- measured_at
- memo
- created_at / updated_at

날짜 unique 제약은 두지 않는다.
하루 여러 번 측정할 수 있으며 user_id + measured_at desc 인덱스를 사용한다.

MVP 개발 완료 전까지 Hibernate ddl-auto: create-drop은 유지한다.

## Frontend

상단 내비게이션에 Body 탭을 추가한다.

입력:
- 날짜: date input
- 시간: time input
- 체중/체지방률/골격근량: 숫자/소수 입력만 허용
- 메모: text 입력

조회:
- 최신 체중 / 체지방률 / 골격근량
- 직전 측정 대비 변화
- 최근 90일 측정 기록
- 기록 수정 / 삭제

Phase 4 Dashboard에서 시계열 차트를 추가하므로
Phase 3에서는 기록과 최근 변화 확인에 집중한다.

## 테스트

테스트 메서드명은 영문 camelCase,
각 테스트는 @DisplayName으로 목적을 기술한다.

검증:
- 정상 신체 기록 생성
- 잘못된 측정값 거부
- 미래 측정 시각 거부
- 같은 날 복수 기록 허용
- 최신순 조회
- 직전 대비 변화량 계산
- 수정 / 삭제
- 다른 사용자 접근 차단

## 완료 기준

- 사용자가 날짜/시간별 신체 기록을 추가할 수 있다.
- 같은 날짜에 여러 측정값을 저장할 수 있다.
- 기존 기록을 수정/삭제할 수 있다.
- 최근 측정값과 직전 대비 변화량을 확인할 수 있다.
- 최근 90일 기록을 시간 역순으로 확인할 수 있다.
