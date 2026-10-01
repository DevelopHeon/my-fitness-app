package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.application.support.NutritionResultAssembler;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionApplicationService implements NutritionUseCase {
    private final MealService mealService;
    private final NutritionGoalService nutritionGoalService;
    private final NutritionResultAssembler resultAssembler;

    NutritionApplicationService(
            MealService mealService,
            NutritionGoalService nutritionGoalService,
            NutritionResultAssembler resultAssembler) {
        this.mealService = mealService;
        this.nutritionGoalService = nutritionGoalService;
        this.resultAssembler = resultAssembler;
    }

    @Transactional
    public MealFoodResult addMealItem(Long userId, LocalDate mealDate, MealType mealType, String name,
            BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {
        return MealFoodResult.from(mealService.addFood(userId, mealDate, mealType, name,
                calories, carbohydrate, protein, fat));
    }

    @Transactional
    public MealFoodResult updateMealItem(Long userId, Long itemId, LocalDate date, MealType type,
            String name, BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {
        return MealFoodResult.from(mealService.updateItem(mealService.getOwnedItem(userId, itemId),
                date, type, name, calories, carbohydrate, protein, fat));
    }

    @Transactional
    public void deleteMealItem(Long userId, Long itemId) {
        mealService.deleteItem(
                mealService.getOwnedItem(userId, itemId));
    }

    public DailyNutritionResult daily(
            Long userId,
            LocalDate date) {
        List<MealFood> items =
                mealService.listDailyItems(userId, date);
        NutritionGoal goal =
                nutritionGoalService.get(userId).orElse(null);

        return resultAssembler.daily(
                date,
                items,
                goal);
    }

    public NutritionGoalResult currentGoal(Long userId) {
        return NutritionGoalResult.from(
                nutritionGoalService.get(userId).orElse(null));
    }

    @Transactional
    public NutritionGoalResult upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        return NutritionGoalResult.from(
                nutritionGoalService.upsert(
                        userId,
                        calories,
                        carbohydrateGrams,
                        proteinGrams,
                        fatGrams));
    }
}
