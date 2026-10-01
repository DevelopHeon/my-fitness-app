package com.myfitness.nutrition.application.support;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.MealFoodResult;
import com.myfitness.nutrition.application.dto.response.MealSectionResult;
import com.myfitness.nutrition.application.dto.response.NutritionGoalResult;
import com.myfitness.nutrition.application.dto.response.NutritionTotals;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NutritionResultAssembler {
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
                : NutritionTotals.from(goal).subtract(consumed);

        return new DailyNutritionResult(
                date,
                NutritionGoalResult.from(goal),
                consumed,
                remaining,
                List.copyOf(sections));
    }

    private static MealSectionResult mealSection(
            MealType mealType,
            List<MealFood> items) {
        List<MealFood> mealItems = items.stream()
                .filter(item ->
                        item.getMeal().getMealType() == mealType)
                .toList();

        NutritionTotals total = mealItems.stream()
                .map(NutritionTotals::from)
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

}
