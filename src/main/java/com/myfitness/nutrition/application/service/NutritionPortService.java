package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.usecase.request.MealItemCommand;
import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import com.myfitness.nutrition.application.usecase.response.MealFoodResult;
import com.myfitness.nutrition.application.usecase.response.NutritionCalendarDayResult;
import com.myfitness.nutrition.application.usecase.response.NutritionGoalResult;
import com.myfitness.nutrition.application.usecase.response.NutritionTotals;
import com.myfitness.nutrition.application.port.in.DailyNutritionView;
import com.myfitness.nutrition.application.port.in.MealFoodView;
import com.myfitness.nutrition.application.port.in.MealSectionView;
import com.myfitness.nutrition.application.port.in.NutritionCalendarDayView;
import com.myfitness.nutrition.application.port.in.NutritionGoalView;
import com.myfitness.nutrition.application.port.in.NutritionPort;
import com.myfitness.nutrition.application.port.in.NutritionTotalsView;
import com.myfitness.nutrition.application.usecase.NutritionUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NutritionPortService implements NutritionPort {
    private final NutritionUseCase useCase;

    public NutritionPortService(NutritionUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public MealFoodView addMealItem(Long userId, LocalDate mealDate, MealType mealType, String foodName,
            BigDecimal calories, BigDecimal carbohydrateGrams, BigDecimal proteinGrams, BigDecimal fatGrams) {
        return toFood(useCase.addMealItem(
                userId, mealDate, mealType == null ? null : mealType.name(), foodName,
                calories, carbohydrateGrams, proteinGrams, fatGrams));
    }

    @Override
    public MealFoodView updateMealItem(Long userId, Long itemId, LocalDate mealDate, MealType mealType,
            String foodName, BigDecimal calories, BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams, BigDecimal fatGrams) {
        return toFood(useCase.updateMealItem(
                userId, itemId, mealDate, mealType == null ? null : mealType.name(), foodName,
                calories, carbohydrateGrams, proteinGrams, fatGrams));
    }

    @Override
    public List<MealFoodView> addMealItems(Long userId, LocalDate mealDate, MealType mealType,
            List<MealItemInput> items) {
        return useCase.addMealItems(userId, mealDate, mealType == null ? null : mealType.name(), toCommands(items))
                .stream()
                .map(NutritionPortService::toFood)
                .toList();
    }

    @Override
    public List<NutritionCalendarDayView> calendar(Long userId, YearMonth month) {
        return useCase.calendar(userId, month).stream().map(NutritionPortService::toDay).toList();
    }

    @Override
    public void deleteMealItem(Long userId, Long itemId) {
        useCase.deleteMealItem(userId, itemId);
    }

    @Override
    public DailyNutritionView daily(Long userId, LocalDate date) {
        return toDaily(useCase.daily(userId, date));
    }

    @Override
    public NutritionGoalView currentGoal(Long userId) {
        return toGoal(useCase.currentGoal(userId));
    }

    @Override
    public NutritionGoalView upsertGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        return toGoal(useCase.upsertGoal(userId, calories, carbohydrateGrams, proteinGrams, fatGrams));
    }

    private static List<MealItemCommand> toCommands(List<MealItemInput> items) {
        if (items == null) {
            return null;
        }
        return items.stream().map(item -> item == null ? null : new MealItemCommand(
                item.foodName(), item.calories(), item.carbohydrateGrams(), item.proteinGrams(), item.fatGrams()))
                .toList();
    }

    private static MealFoodView toFood(MealFoodResult food) {
        return new MealFoodView(
                food.id(), food.mealDate(), food.mealType(), food.foodName(), food.calories(),
                food.carbohydrateGrams(), food.proteinGrams(), food.fatGrams(), food.createdAt(), food.updatedAt());
    }

    private static DailyNutritionView toDaily(DailyNutritionResult daily) {
        return new DailyNutritionView(
                daily.date(), toGoal(daily.goal()), toTotals(daily.consumed()), toTotals(daily.remaining()),
                daily.meals().stream().map(meal -> new MealSectionView(
                        meal.mealType(), meal.items().stream().map(NutritionPortService::toFood).toList(),
                        toTotals(meal.total()))).toList());
    }

    private static NutritionGoalView toGoal(NutritionGoalResult goal) {
        return goal == null ? null : new NutritionGoalView(
                goal.id(), goal.calories(), goal.carbohydrateGrams(), goal.proteinGrams(), goal.fatGrams(), goal.updatedAt());
    }

    private static NutritionTotalsView toTotals(NutritionTotals totals) {
        return totals == null ? null : new NutritionTotalsView(
                totals.calories(), totals.carbohydrateGrams(), totals.proteinGrams(), totals.fatGrams());
    }

    private static NutritionCalendarDayView toDay(NutritionCalendarDayResult day) {
        return new NutritionCalendarDayView(day.date(), day.calories(), day.mealTypes());
    }
}
