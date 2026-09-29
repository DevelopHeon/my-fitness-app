package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.FoodResult;
import com.myfitness.nutrition.application.dto.response.FoodSuggestionsResult;
import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.application.support.NutritionResultAssembler;
import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionApplicationService implements NutritionUseCase {
    private final FoodService foodService;
    private final MealService mealService;
    private final NutritionGoalService nutritionGoalService;
    private final NutritionResultAssembler resultAssembler;

    NutritionApplicationService(
            FoodService foodService,
            MealService mealService,
            NutritionGoalService nutritionGoalService,
            NutritionResultAssembler resultAssembler) {
        this.foodService = foodService;
        this.mealService = mealService;
        this.nutritionGoalService = nutritionGoalService;
        this.resultAssembler = resultAssembler;
    }

    public List<FoodResult> listFoods(Long userId, String query) {
        return foodService.list(userId, query).stream()
                .map(FoodResult::from)
                .toList();
    }

    @Transactional
    public FoodResult createFood(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        return FoodResult.from(
                foodService.create(
                        userId,
                        name,
                        servingAmount,
                        servingUnit,
                        calories,
                        carbohydrateGrams,
                        proteinGrams,
                        fatGrams));
    }

    @Transactional
    public FoodResult updateFood(
            Long userId,
            Long foodId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        Food food = foodService.getOwned(userId, foodId);
        return FoodResult.from(
                foodService.update(
                        food,
                        name,
                        servingAmount,
                        servingUnit,
                        calories,
                        carbohydrateGrams,
                        proteinGrams,
                        fatGrams));
    }

    @Transactional
    public void deleteFood(Long userId, Long foodId) {
        foodService.delete(foodService.getOwned(userId, foodId));
    }

    public FoodSuggestionsResult suggestions(Long userId) {
        return buildSuggestions(
                userId,
                mealService.listUsageHistory(userId));
    }

    FoodSuggestionsResult suggestions(
            Long userId,
            int usageHistoryLimit) {
        return buildSuggestions(
                userId,
                mealService.listRecentUsageHistory(
                        userId,
                        usageHistoryLimit));
    }

    private FoodSuggestionsResult buildSuggestions(
            Long userId,
            List<MealFood> history) {
        return resultAssembler.suggestions(
                foodService.list(userId, null),
                history);
    }

    @Transactional
    public MealFoodResult addMealItem(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Long foodId,
            BigDecimal servings) {
        Food food = foodService.getOwned(userId, foodId);
        return MealFoodResult.from(
                mealService.addFood(
                        userId,
                        mealDate,
                        mealType,
                        food,
                        servings));
    }

    @Transactional
    public MealFoodResult updateMealItem(
            Long userId,
            Long itemId,
            BigDecimal servings) {
        MealFood item = mealService.getOwnedItem(userId, itemId);
        return MealFoodResult.from(
                mealService.updateItem(item, servings));
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
