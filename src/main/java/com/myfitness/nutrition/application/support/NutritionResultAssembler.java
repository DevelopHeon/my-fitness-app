package com.myfitness.nutrition.application.support;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.FoodResult;
import com.myfitness.nutrition.application.dto.response.FoodSuggestionsResult;
import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.application.dto.response.MealSectionResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.application.dto.response.NutritionTotals;
import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
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
import org.springframework.stereotype.Component;

@Component
public class NutritionResultAssembler {
    private static final int SUGGESTION_LIMIT = 5;

    public FoodSuggestionsResult suggestions(
            List<Food> foods,
            List<MealFood> history) {
        Map<Long, Food> foodById = foods.stream()
                .collect(Collectors.toMap(
                        Food::getId,
                        Function.identity()));

        SuggestionStats stats = collectSuggestionStats(
                history,
                foodById);

        List<FoodResult> recent = stats.recentIds().stream()
                .limit(SUGGESTION_LIMIT)
                .map(foodById::get)
                .map(FoodResult::from)
                .toList();

        List<FoodResult> frequent = stats.counts().entrySet().stream()
                .sorted(frequentComparator(stats.firstSeenOrder()))
                .limit(SUGGESTION_LIMIT)
                .map(entry -> foodById.get(entry.getKey()))
                .map(FoodResult::from)
                .toList();

        return new FoodSuggestionsResult(recent, frequent);
    }

    public DailyNutritionResult daily(
            LocalDate date,
            List<MealFood> items,
            NutritionGoal goal) {
        List<MealSectionResult> sections = new ArrayList<>();
        NutritionTotals consumed = NutritionTotals.zero();

        for (MealType mealType : MealType.values()) {
            MealSectionResult section = mealSection(
                    mealType,
                    items);
            sections.add(section);
            consumed = consumed.add(section.total());
        }

        NutritionTotals remaining = goal == null
                ? null
                : totalsOf(goal).subtract(consumed);

        return new DailyNutritionResult(
                date,
                NutritionGoalResult.from(goal),
                consumed,
                remaining,
                List.copyOf(sections));
    }

    private static SuggestionStats collectSuggestionStats(
            List<MealFood> history,
            Map<Long, Food> foodById) {
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

        return new SuggestionStats(
                recentIds,
                counts,
                firstSeenOrder);
    }

    private static Comparator<Map.Entry<Long, Integer>> frequentComparator(
            Map<Long, Integer> firstSeenOrder) {
        return Comparator
                .<Map.Entry<Long, Integer>>comparingInt(
                        Map.Entry::getValue)
                .reversed()
                .thenComparingInt(entry ->
                        firstSeenOrder.getOrDefault(
                                entry.getKey(),
                                Integer.MAX_VALUE));
    }

    private static MealSectionResult mealSection(
            MealType mealType,
            List<MealFood> items) {
        List<MealFood> mealItems = items.stream()
                .filter(item ->
                        item.getMeal().getMealType() == mealType)
                .toList();

        NutritionTotals total = mealItems.stream()
                .map(NutritionResultAssembler::totalsOf)
                .reduce(
                        NutritionTotals.zero(),
                        NutritionTotals::add);

        return new MealSectionResult(
                mealType,
                mealItems.stream()
                        .map(MealFoodResult::from)
                        .toList(),
                total);
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

    private record SuggestionStats(
            Set<Long> recentIds,
            Map<Long, Integer> counts,
            Map<Long, Integer> firstSeenOrder
    ) {}
}
