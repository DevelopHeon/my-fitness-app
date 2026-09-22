package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NutritionResultAssemblerTest {
    private static final Instant NOW =
            Instant.parse("2026-09-22T00:00:00Z");

    private final NutritionResultAssembler assembler =
            new NutritionResultAssembler();

    @Test
    @DisplayName("음식 추천은 최근 사용 순서와 사용 빈도를 각각 계산한다")
    void buildsRecentAndFrequentFoodSuggestions() {
        Food chicken = food(1L, "닭가슴살");
        Food egg = food(2L, "계란");
        Food rice = food(3L, "현미밥");

        var result = assembler.suggestions(
                List.of(chicken, egg, rice),
                List.of(
                        usage(2L),
                        usage(1L),
                        usage(2L),
                        usage(3L),
                        usage(2L),
                        usage(1L)));

        assertThat(result.recent())
                .extracting(item -> item.name())
                .containsExactly("계란", "닭가슴살", "현미밥");
        assertThat(result.frequent())
                .extracting(item -> item.name())
                .containsExactly("계란", "닭가슴살", "현미밥");
    }

    @Test
    @DisplayName("일일 영양 결과는 식사 구분별 합계와 전체 섭취량 및 남은 목표를 계산한다")
    void buildsDailyNutritionTotals() {
        LocalDate date = LocalDate.of(2026, 9, 22);
        MealFood breakfast = mealFood(
                MealType.BREAKFAST,
                1L,
                "오트밀",
                "300",
                "50",
                "20",
                "8");
        MealFood dinner = mealFood(
                MealType.DINNER,
                2L,
                "닭가슴살",
                "400",
                "10",
                "50",
                "12");
        NutritionGoal goal = NutritionGoal.create(
                1L,
                new BigDecimal("2000"),
                new BigDecimal("250"),
                new BigDecimal("150"),
                new BigDecimal("60"),
                NOW);

        var result = assembler.daily(
                date,
                List.of(breakfast, dinner),
                goal);

        assertThat(result.consumed().calories())
                .isEqualByComparingTo("700");
        assertThat(result.consumed().proteinGrams())
                .isEqualByComparingTo("70");
        assertThat(result.remaining().calories())
                .isEqualByComparingTo("1300");
        assertThat(result.remaining().proteinGrams())
                .isEqualByComparingTo("80");
        assertThat(result.meals())
                .filteredOn(section ->
                        section.mealType() == MealType.BREAKFAST)
                .singleElement()
                .satisfies(section ->
                        assertThat(section.total().calories())
                                .isEqualByComparingTo("300"));
        assertThat(result.meals())
                .filteredOn(section ->
                        section.mealType() == MealType.DINNER)
                .singleElement()
                .satisfies(section ->
                        assertThat(section.total().calories())
                                .isEqualByComparingTo("400"));
    }

    private static Food food(Long id, String name) {
        Food food = mock(Food.class);
        when(food.getId()).thenReturn(id);
        when(food.getName()).thenReturn(name);
        when(food.getServingAmount())
                .thenReturn(new BigDecimal("100"));
        when(food.getServingUnit()).thenReturn(ServingUnit.G);
        when(food.getCalories()).thenReturn(new BigDecimal("100"));
        when(food.getCarbohydrateGrams()).thenReturn(BigDecimal.ZERO);
        when(food.getProteinGrams()).thenReturn(new BigDecimal("20"));
        when(food.getFatGrams()).thenReturn(BigDecimal.ZERO);
        when(food.getCreatedAt()).thenReturn(NOW);
        when(food.getUpdatedAt()).thenReturn(NOW);
        return food;
    }

    private static MealFood usage(Long sourceFoodId) {
        MealFood item = mock(MealFood.class);
        when(item.getSourceFoodId()).thenReturn(sourceFoodId);
        return item;
    }

    private static MealFood mealFood(
            MealType mealType,
            Long sourceFoodId,
            String name,
            String calories,
            String carbohydrate,
            String protein,
            String fat) {
        Meal meal = mock(Meal.class);
        when(meal.getMealType()).thenReturn(mealType);

        MealFood item = mock(MealFood.class);
        when(item.getMeal()).thenReturn(meal);
        when(item.getSourceFoodId()).thenReturn(sourceFoodId);
        when(item.getFoodName()).thenReturn(name);
        when(item.getServingAmount())
                .thenReturn(new BigDecimal("100"));
        when(item.getServingUnit()).thenReturn(ServingUnit.G);
        when(item.getCaloriesPerServing())
                .thenReturn(new BigDecimal(calories));
        when(item.getCarbohydrateGramsPerServing())
                .thenReturn(new BigDecimal(carbohydrate));
        when(item.getProteinGramsPerServing())
                .thenReturn(new BigDecimal(protein));
        when(item.getFatGramsPerServing())
                .thenReturn(new BigDecimal(fat));
        when(item.getServings()).thenReturn(BigDecimal.ONE);
        when(item.totalCalories())
                .thenReturn(new BigDecimal(calories));
        when(item.totalCarbohydrateGrams())
                .thenReturn(new BigDecimal(carbohydrate));
        when(item.totalProteinGrams())
                .thenReturn(new BigDecimal(protein));
        when(item.totalFatGrams())
                .thenReturn(new BigDecimal(fat));
        return item;
    }
}
