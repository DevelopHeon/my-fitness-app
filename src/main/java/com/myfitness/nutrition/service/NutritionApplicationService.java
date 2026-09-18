package com.myfitness.nutrition.service;

import com.myfitness.nutrition.domain.Food;
import com.myfitness.nutrition.domain.MealFood;
import com.myfitness.nutrition.domain.MealType;
import com.myfitness.nutrition.domain.NutritionGoal;
import com.myfitness.nutrition.dto.request.FoodUpsertRequest;
import com.myfitness.nutrition.dto.request.MealItemCreateRequest;
import com.myfitness.nutrition.dto.request.MealItemUpdateRequest;
import com.myfitness.nutrition.dto.request.NutritionGoalUpsertRequest;
import com.myfitness.nutrition.dto.response.DailyNutritionResponse;
import com.myfitness.nutrition.dto.response.FoodResponse;
import com.myfitness.nutrition.dto.response.FoodSuggestionsResponse;
import com.myfitness.nutrition.dto.response.MealFoodResponse;
import com.myfitness.nutrition.dto.response.MealSectionResponse;
import com.myfitness.nutrition.dto.response.NutritionGoalResponse;
import com.myfitness.nutrition.dto.response.NutritionTotalsResponse;
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

    public List<FoodResponse> listFoods(Long userId, String query) {
        return foodService.list(userId, query).stream()
                .map(FoodResponse::from)
                .toList();
    }

    @Transactional
    public FoodResponse createFood(
            Long userId,
            FoodUpsertRequest request) {
        return FoodResponse.from(foodService.create(
                userId,
                request.name(),
                request.servingAmount(),
                request.servingUnit(),
                request.calories(),
                request.carbohydrateGrams(),
                request.proteinGrams(),
                request.fatGrams()));
    }

    @Transactional
    public FoodResponse updateFood(
            Long userId,
            Long foodId,
            FoodUpsertRequest request) {
        Food food = foodService.getOwned(userId, foodId);
        return FoodResponse.from(foodService.update(
                food,
                request.name(),
                request.servingAmount(),
                request.servingUnit(),
                request.calories(),
                request.carbohydrateGrams(),
                request.proteinGrams(),
                request.fatGrams()));
    }

    @Transactional
    public void deleteFood(Long userId, Long foodId) {
        foodService.delete(foodService.getOwned(userId, foodId));
    }

    public FoodSuggestionsResponse suggestions(Long userId) {
        List<Food> foods = foodService.list(userId, null);
        Map<Long, Food> foodById = foods.stream()
                .collect(Collectors.toMap(Food::getId, Function.identity()));

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

        List<FoodResponse> recent = recentIds.stream()
                .limit(SUGGESTION_LIMIT)
                .map(foodById::get)
                .map(FoodResponse::from)
                .toList();

        List<FoodResponse> frequent = counts.entrySet().stream()
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
                .map(FoodResponse::from)
                .toList();

        return new FoodSuggestionsResponse(recent, frequent);
    }

    @Transactional
    public MealFoodResponse addMealItem(
            Long userId,
            MealItemCreateRequest request) {
        Food food = foodService.getOwned(userId, request.foodId());
        return MealFoodResponse.from(mealService.addFood(
                userId,
                request.mealDate(),
                request.mealType(),
                food,
                request.servings()));
    }

    @Transactional
    public MealFoodResponse updateMealItem(
            Long userId,
            Long itemId,
            MealItemUpdateRequest request) {
        MealFood item = mealService.getOwnedItem(userId, itemId);
        return MealFoodResponse.from(
                mealService.updateItem(item, request.servings()));
    }

    @Transactional
    public void deleteMealItem(Long userId, Long itemId) {
        mealService.deleteItem(mealService.getOwnedItem(userId, itemId));
    }

    public DailyNutritionResponse daily(
            Long userId,
            LocalDate date) {
        List<MealFood> items = mealService.listDailyItems(userId, date);

        List<MealSectionResponse> sections = new ArrayList<>();
        NutritionTotalsResponse consumed = NutritionTotalsResponse.zero();

        for (MealType mealType : MealType.values()) {
            List<MealFoodResponse> responses = items.stream()
                    .filter(item ->
                            item.getMeal().getMealType() == mealType)
                    .map(MealFoodResponse::from)
                    .toList();

            NutritionTotalsResponse sectionTotal =
                    responses.stream()
                            .map(MealFoodResponse::total)
                            .reduce(
                                    NutritionTotalsResponse.zero(),
                                    NutritionTotalsResponse::add);

            consumed = consumed.add(sectionTotal);
            sections.add(new MealSectionResponse(
                    mealType,
                    responses,
                    sectionTotal));
        }

        NutritionGoalResponse goal = nutritionGoalService.get(userId)
                .map(NutritionGoalResponse::from)
                .orElse(null);
        NutritionTotalsResponse remaining =
                goal == null
                        ? null
                        : goal.totals().subtract(consumed);

        return new DailyNutritionResponse(
                date,
                goal,
                consumed,
                remaining,
                sections);
    }

    public NutritionGoalResponse currentGoal(Long userId) {
        return nutritionGoalService.get(userId)
                .map(NutritionGoalResponse::from)
                .orElse(null);
    }

    @Transactional
    public NutritionGoalResponse upsertGoal(
            Long userId,
            NutritionGoalUpsertRequest request) {
        NutritionGoal goal = nutritionGoalService.upsert(
                userId,
                request.calories(),
                request.carbohydrateGrams(),
                request.proteinGrams(),
                request.fatGrams());
        return NutritionGoalResponse.from(goal);
    }
}
