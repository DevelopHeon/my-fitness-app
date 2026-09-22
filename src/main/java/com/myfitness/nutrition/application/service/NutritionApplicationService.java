package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
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
public class NutritionApplicationService {
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

    public List<Food> listFoods(Long userId, String query) {
        return foodService.list(userId, query);
    }

    @Transactional
    public Food createFood(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        return foodService.create(
                userId,
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams);
    }

    @Transactional
    public Food updateFood(
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
        return foodService.update(
                food,
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams);
    }

    @Transactional
    public void deleteFood(Long userId, Long foodId) {
        foodService.delete(foodService.getOwned(userId, foodId));
    }

    public FoodSuggestionsResult suggestions(Long userId) {
        List<Food> foods = foodService.list(userId, null);
        Map<Long, Food> foodById = foods.stream()
                .collect(Collectors.toMap(
                        Food::getId, Function.identity()));

        List<MealFood> history = mealService.listUsageHistory(userId);
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

        return new FoodSuggestionsResult(recent, frequent);
    }

    @Transactional
    public MealFood addMealItem(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Long foodId,
            BigDecimal servings) {
        Food food = foodService.getOwned(userId, foodId);
        return mealService.addFood(
                userId,
                mealDate,
                mealType,
                food,
                servings);
    }

    @Transactional
    public MealFood updateMealItem(
            Long userId,
            Long itemId,
            BigDecimal servings) {
        MealFood item = mealService.getOwnedItem(userId, itemId);
        return mealService.updateItem(item, servings);
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
                    mealItems,
                    sectionTotal));
        }

        NutritionGoal goal =
                nutritionGoalService.get(userId).orElse(null);
        NutritionTotals remaining = goal == null
                ? null
                : totalsOf(goal).subtract(consumed);

        return new DailyNutritionResult(
                date,
                goal,
                consumed,
                remaining,
                sections);
    }

    public NutritionGoal currentGoal(Long userId) {
        return nutritionGoalService.get(userId).orElse(null);
    }

    @Transactional
    public NutritionGoal upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        return nutritionGoalService.upsert(
                userId,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams);
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
