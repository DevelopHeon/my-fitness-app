package com.myfitness.nutrition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.nutrition.exception.NutritionRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoodTest {

    private final Instant now = Instant.parse("2026-09-18T09:00:00Z");

    @Test
    @DisplayName("사용자 음식은 1회 제공량과 칼로리, 탄단지를 저장한다")
    void createsFoodWithServingNutrition() {
        Food food = Food.create(
                1L,
                "닭가슴살",
                new BigDecimal("100"),
                ServingUnit.G,
                new BigDecimal("165"),
                new BigDecimal("0"),
                new BigDecimal("31"),
                new BigDecimal("3.6"),
                now);

        assertThat(food.getName()).isEqualTo("닭가슴살");
        assertThat(food.getServingAmount()).isEqualByComparingTo("100");
        assertThat(food.getServingUnit()).isEqualTo(ServingUnit.G);
        assertThat(food.getCalories()).isEqualByComparingTo("165");
        assertThat(food.getProteinGrams()).isEqualByComparingTo("31");
    }

    @Test
    @DisplayName("음식 제공량은 0보다 커야 하고 영양소는 음수가 될 수 없다")
    void rejectsInvalidFoodNutrition() {
        assertThatThrownBy(() -> Food.create(
                1L,
                "닭가슴살",
                BigDecimal.ZERO,
                ServingUnit.G,
                new BigDecimal("165"),
                BigDecimal.ZERO,
                new BigDecimal("31"),
                new BigDecimal("3.6"),
                now))
                .isInstanceOf(NutritionRuleException.class);

        assertThatThrownBy(() -> Food.create(
                1L,
                "닭가슴살",
                new BigDecimal("100"),
                ServingUnit.G,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                new BigDecimal("31"),
                new BigDecimal("3.6"),
                now))
                .isInstanceOf(NutritionRuleException.class);
    }

    @Test
    @DisplayName("식단 음식은 음식 수정과 무관하게 기록 시점의 영양값을 스냅샷으로 보존한다")
    void preservesMealFoodNutritionSnapshot() {
        Food food = Food.create(
                1L,
                "오트밀",
                new BigDecimal("50"),
                ServingUnit.G,
                new BigDecimal("190"),
                new BigDecimal("32"),
                new BigDecimal("7"),
                new BigDecimal("4"),
                now);
        Meal meal = Meal.create(
                1L,
                java.time.LocalDate.of(2026, 9, 18),
                MealType.BREAKFAST,
                now);

        MealFood mealFood = MealFood.fromFood(
                meal,
                food,
                new BigDecimal("1.5"),
                now);

        food.update(
                "오트밀 수정",
                new BigDecimal("60"),
                ServingUnit.G,
                new BigDecimal("250"),
                new BigDecimal("40"),
                new BigDecimal("10"),
                new BigDecimal("6"),
                now.plusSeconds(60));

        assertThat(mealFood.getFoodName()).isEqualTo("오트밀");
        assertThat(mealFood.getCaloriesPerServing()).isEqualByComparingTo("190");
        assertThat(mealFood.totalCalories()).isEqualByComparingTo("285.00");
        assertThat(mealFood.totalCarbohydrateGrams()).isEqualByComparingTo("48.00");
        assertThat(mealFood.totalProteinGrams()).isEqualByComparingTo("10.50");
        assertThat(mealFood.totalFatGrams()).isEqualByComparingTo("6.00");
    }
}
