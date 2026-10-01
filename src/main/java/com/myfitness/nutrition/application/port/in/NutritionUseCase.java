package com.myfitness.nutrition.application.port.in;

import com.myfitness.nutrition.application.dto.request.MealItemCommand;
import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.application.dto.response.NutritionCalendarDayResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface NutritionUseCase {
    MealFoodResult addMealItem(Long userId, LocalDate mealDate, MealType mealType, String foodName,
            BigDecimal calories, BigDecimal carbohydrateGrams, BigDecimal proteinGrams, BigDecimal fatGrams);

    MealFoodResult updateMealItem(Long userId, Long itemId, LocalDate mealDate, MealType mealType,
            String foodName, BigDecimal calories, BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams, BigDecimal fatGrams);

    List<MealFoodResult> addMealItems(Long userId, LocalDate mealDate, MealType mealType,
            List<MealItemCommand> items);

    List<NutritionCalendarDayResult> calendar(Long userId, YearMonth month);

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
