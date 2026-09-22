package com.myfitness.nutrition.application.port.in;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.application.result.MealFoodResult;
import com.myfitness.nutrition.application.result.NutritionGoalResult;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface NutritionUseCase {
    List<FoodResult> listFoods(Long userId, String query);

    FoodResult createFood(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);

    FoodResult updateFood(
            Long userId,
            Long foodId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);

    void deleteFood(Long userId, Long foodId);

    FoodSuggestionsResult suggestions(Long userId);

    MealFoodResult addMealItem(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Long foodId,
            BigDecimal servings);

    MealFoodResult updateMealItem(
            Long userId,
            Long itemId,
            BigDecimal servings);

    void deleteMealItem(Long userId, Long itemId);

    DailyNutritionResult daily(Long userId, LocalDate date);

    NutritionGoalResult currentGoal(Long userId);

    NutritionGoalResult upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);
}
