package com.myfitness.nutrition.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface NutritionPort {
    MealFoodView addMealItem(Long userId, LocalDate mealDate, MealType mealType, String foodName,
            BigDecimal calories, BigDecimal carbohydrateGrams, BigDecimal proteinGrams, BigDecimal fatGrams);

    MealFoodView updateMealItem(Long userId, Long itemId, LocalDate mealDate, MealType mealType,
            String foodName, BigDecimal calories, BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams, BigDecimal fatGrams);

    List<MealFoodView> addMealItems(Long userId, LocalDate mealDate, MealType mealType,
            List<MealItemInput> items);

    List<NutritionCalendarDayView> calendar(Long userId, YearMonth month);

    void deleteMealItem(Long userId, Long itemId);

    DailyNutritionView daily(Long userId, LocalDate date);

    NutritionGoalView currentGoal(Long userId);

    NutritionGoalView upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);

    enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    record MealItemInput(
            String foodName, BigDecimal calories, BigDecimal carbohydrateGrams, BigDecimal proteinGrams,
            BigDecimal fatGrams) {}
}
