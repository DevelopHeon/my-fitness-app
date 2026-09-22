package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.application.result.FoodResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.application.result.MealFoodResult;
import com.myfitness.nutrition.application.result.NutritionGoalResult;
import com.myfitness.nutrition.application.result.MealSectionResult;
import com.myfitness.nutrition.application.result.NutritionTotals;
import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionApplicationService implements NutritionUseCase {
    private static final int SUGGESTION_LIMIT = 5;

    private final FoodService foodService;
    private final MealService mealService;
    private final NutritionGoalService nutritionGoalService;

    public NutritionApplicationService(
            FoodService foodService,
            MealService mealService,
            NutritionGoalService nutritionGoalService) {
        this.foodService = foodService;
        this.mealService = mealService;
        this.nutritionGoalService = nutritionGoalService;
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
                        userId, usageHistoryLimit));
    }

    private FoodSuggestionsResult buildSuggestions(
            Long userId,
            List<MealFood> history) {
        List<Food> foods = foodService.list(userId, null);
        Map<Long, Food> foodById = foods.stream()
                .collect(Collectors.toMap(
                        Food::getId, Function.identity()));

        Set<Long> recentIds = new LinkedHashSet<>();
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, Integer> firstSeenOrder = new HashMap<>();

        int order = 0;
        for (MealFood item : history) {
            Long foodId = item.getSourceFoodId();
            if (!foodById.containsKey(foodId)) {
                continue;
            }
            recentIds.add(foodId);
            counts.merge(foodId, 1, Integer::sum);
            firstSeenOrder.putIfAbsent(foodId, order++);
        }

        List<Food> recent = recentIds.stream()
                .limit(SUGGESTION_LIMIT)
                .map(foodById::get)
                .toList();

        List<Food> frequent = counts.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<Long, Integer>>comparingInt(
                                Map.Entry::getValue)
                        .reversed()
                        .thenComparingInt(entry ->
                                firstSeenOrder.getOrDefault(
                                        entry.getKey(),
                                        Integer.MAX_VALUE)))
                .limit(SUGGESTION_LIMIT)
                .map(entry -> foodById.get(entry.getKey()))
                .toList();

        return new FoodSuggestionsResult(
                recent.stream().map(FoodResult::from).toList(),
                frequent.stream().map(FoodResult::from).toList());
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

        List<MealSectionResult> sections = new ArrayList<>();
        NutritionTotals consumed = NutritionTotals.zero();

        for (MealType mealType : MealType.values()) {
            List<MealFood> mealItems = items.stream()
                    .filter(item ->
                            item.getMeal().getMealType() == mealType)
                    .toList();

            NutritionTotals sectionTotal = mealItems.stream()
                    .map(NutritionApplicationService::totalsOf)
                    .reduce(
                            NutritionTotals.zero(),
                            NutritionTotals::add);

            consumed = consumed.add(sectionTotal);
            sections.add(new MealSectionResult(
                    mealType,
                    mealItems.stream()
                            .map(MealFoodResult::from)
                            .toList(),
                    sectionTotal));
        }

        NutritionGoal goal =
                nutritionGoalService.get(userId).orElse(null);
        NutritionTotals remaining = goal == null
                ? null
                : totalsOf(goal).subtract(consumed);

        return new DailyNutritionResult(
                date,
                NutritionGoalResult.from(goal),
                consumed,
                remaining,
                sections);
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

    private static NutritionTotals totalsOf(MealFood item) {
        return new NutritionTotals(
                item.totalCalories(),
                item.totalCarbohydrateGrams(),
                item.totalProteinGrams(),
                item.totalFatGrams());
    }

    private static NutritionTotals totalsOf(NutritionGoal goal) {
        return new NutritionTotals(
                goal.getCalories(),
                goal.getCarbohydrateGrams(),
                goal.getProteinGrams(),
                goal.getFatGrams());
    }
}
