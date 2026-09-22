package com.myfitness.nutrition.application.port.in;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface NutritionUseCase {
    List<Food> listFoods(Long userId, String query);

    Food createFood(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);

    Food updateFood(
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

    MealFood addMealItem(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Long foodId,
            BigDecimal servings);

    MealFood updateMealItem(
            Long userId,
            Long itemId,
            BigDecimal servings);

    void deleteMealItem(Long userId, Long itemId);

    DailyNutritionResult daily(Long userId, LocalDate date);

    NutritionGoal currentGoal(Long userId);

    NutritionGoal upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);
}
