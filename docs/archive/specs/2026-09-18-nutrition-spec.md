# PR-008 Nutrition

> Archive: 당시 설계·구현·검증 기록이다. 현재 계약과 실행 방법은 [루트 문서 지도](../../../README.md#문서-찾기)를 따른다. 남은 운영 인수는 현재 영역 문서에서 추적한다. 보관 기준은 문서 재편 전 commit `e9b1df1f93ed7a94e682cc544b33a1b6c1668029`이며 각 본문의 실제 구현·평가 대상 commit과 구분한다.


> 상태: 구현 완료

## 목표

사용자가 직접 등록한 음식을 반복 입력 없이 재사용하고,
아침 / 점심 / 저녁 / 간식 단위로 하루 식단을 빠르게 기록한다.

하루 섭취 칼로리와 탄수화물 / 단백질 / 지방을 합산하고
사용자 목표와 즉시 비교한다.

## 데이터 모델

### Food

사용자 소유 음식 카탈로그다.

- userId
- name
- 1회 제공량
- 제공량 단위: G / ML / COUNT
- calories
- carbohydrateGrams
- proteinGrams
- fatGrams
- createdAt / updatedAt

사용자 내부에서 같은 음식 이름은 중복 등록하지 않는다.

### Meal

날짜와 식사 구분을 나타낸다.

- userId
- mealDate
- mealType
  - BREAKFAST
  - LUNCH
  - DINNER
  - SNACK

사용자 + 날짜 + 식사 구분 조합은 하나만 유지하고
여러 음식은 MealFood로 추가한다.

### MealFood

식단에 실제로 추가된 음식 기록이다.

MealFood는 Food FK에 의존하지 않고
원본 foodId를 sourceFoodId 값으로만 남긴다.

기록 시점의 다음 값을 스냅샷으로 저장한다.

- foodName
- servingAmount / servingUnit
- caloriesPerServing
- carbohydrateGramsPerServing
- proteinGramsPerServing
- fatGramsPerServing
- servings

따라서 Food를 나중에 수정하거나 삭제해도
과거 식단의 이름과 영양정보는 변하지 않는다.

섭취 합계:

~~~text
total nutrition = per serving nutrition × servings
~~~

### NutritionGoal

사용자별 현재 목표를 하나 유지한다.

- calories
- carbohydrateGrams
- proteinGrams
- fatGrams

목표를 다시 저장하면 기존 목표를 갱신한다.

## 비즈니스 규칙

- 1회 제공량은 0보다 커야 한다.
- 칼로리와 탄단지는 0 이상이어야 한다.
- 섭취 회분은 0보다 크고 100 이하여야 한다.
- 미래 날짜의 식단은 기록하지 않는다.
- 다른 사용자의 Food / MealFood는 수정하거나 삭제할 수 없다.
- Food 삭제는 과거 MealFood 스냅샷에 영향을 주지 않는다.
- 하루 영양 합계와 목표 대비 남은 양은 Java 코드에서 계산한다.
- 목표를 초과한 경우 remaining은 음수가 될 수 있으며 UI에서 초과량으로 표시한다.

## 음식 재사용

### 검색

~~~text
GET /api/foods?query={name}
~~~

사용자 음식 이름을 부분 일치 검색한다.

### 최근 / 자주 먹는 음식

~~~text
GET /api/foods/suggestions
~~~

- recent: 최근 식단에서 사용한 현재 Food 최대 5개
- frequent: 누적 사용 횟수가 많은 현재 Food 최대 5개
- 삭제된 Food는 추천에서 제외
- 빈도 동률이면 최근 사용한 Food를 우선

## API

### Food

~~~text
GET    /api/foods
GET    /api/foods?query=
GET    /api/foods/suggestions
POST   /api/foods
PUT    /api/foods/{foodId}
DELETE /api/foods/{foodId}
~~~

### Meal

~~~text
GET    /api/meals/daily?date=YYYY-MM-DD
POST   /api/meals/items
PATCH  /api/meals/items/{itemId}
DELETE /api/meals/items/{itemId}
~~~

일별 응답은 빈 식사도 포함해 항상 다음 4개 section을 제공한다.

1. BREAKFAST
2. LUNCH
3. DINNER
4. SNACK

각 section은 음식 목록과 section 영양 합계를 가진다.

### Nutrition Goal

~~~text
GET /api/nutrition-goals/current
PUT /api/nutrition-goals/current
~~~

## Flyway

V5__create_nutrition_tables.sql

생성 테이블:

- foods
- meals
- meal_foods
- nutrition_goals

메인 환경은 ddl-auto: none을 유지하며
Nutrition schema도 Flyway migration으로만 생성한다.

## Frontend

상단 메뉴에 Nutrition을 추가한다.

Nutrition 내부는 긴 단일 화면 대신 3개 메뉴로 분리한다.

1. 요약 · 목표
   - 선택 날짜
   - 칼로리 / 탄수화물 / 단백질 / 지방 섭취 요약
   - 목표량 / 남은 양 / 초과량
   - 목표 설정 / 수정
2. 식단 기록
   - 선택 날짜
   - 아침 / 점심 / 저녁 / 간식 카드
   - 식사 카드별 음식 추가
   - 최근 먹은 음식
   - 자주 먹는 음식
   - 사용자 음식 검색
   - 섭취 회분 수정 / 식단 항목 삭제
3. 음식 관리
   - 음식 등록
   - 음식 수정
   - 음식 삭제
   - 등록된 음식 영양정보 목록

기본 진입 메뉴는 요약 · 목표다.

숫자 입력은 decimal 입력만 허용하고,
날짜는 date input을 사용하며 오늘 이후 날짜는 선택하지 못하게 한다.

음식 등록/수정 완료 후 입력값은 초기화한다.

## 테스트

도메인 규칙:

- Food 제공량 / 영양값 검증
- MealFood 영양 스냅샷
- servings 기준 총 영양 계산

API 통합:

- 목표 + 여러 식사 기록의 일별 합계
- 목표 대비 남은 칼로리 / 탄단지
- Food 수정/삭제 후 과거 MealFood 스냅샷 유지
- 최근 / 자주 먹는 음식
- 음식 검색
- 사용자 소유권 격리

테스트 메서드는 영문 camelCase를 사용하고
모든 테스트에 @DisplayName을 지정한다.
