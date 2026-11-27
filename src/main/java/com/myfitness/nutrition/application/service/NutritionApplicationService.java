package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.usecase.NutritionGoalUseCase;
import com.myfitness.nutrition.application.usecase.MealOperationsUseCase;
import com.myfitness.nutrition.application.usecase.request.MealItemCommand;
import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import com.myfitness.nutrition.application.usecase.response.MealFoodResult;
import com.myfitness.nutrition.application.usecase.response.NutritionCalendarDayResult;
import com.myfitness.nutrition.application.usecase.response.NutritionGoalResult;
import com.myfitness.nutrition.application.usecase.NutritionUseCase;
import com.myfitness.nutrition.application.port.out.MealFoodReadPort;
import com.myfitness.nutrition.application.support.NutritionResultAssembler;
import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionApplicationService implements NutritionUseCase {
    private final MealOperationsUseCase mealService;
    private final MealFoodReadPort mealFoodRead;
    private final NutritionGoalUseCase nutritionGoalService;
    private final NutritionResultAssembler resultAssembler;

    NutritionApplicationService(
            MealOperationsUseCase mealService,
            NutritionGoalUseCase nutritionGoalService,
            NutritionResultAssembler resultAssembler, MealFoodReadPort mealFoodRead) {
        this.mealService = mealService;
        this.nutritionGoalService = nutritionGoalService;
        this.resultAssembler = resultAssembler;
        this.mealFoodRead = mealFoodRead;
    }

    @Transactional
    public MealFoodResult addMealItem(Long userId, LocalDate mealDate, String mealType, String name,
            BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {
        return MealFoodResult.from(mealService.addFood(userId, mealDate, toMealType(mealType), name,
                calories, carbohydrate, protein, fat));
    }

    @Transactional
    public MealFoodResult updateMealItem(Long userId, Long itemId, LocalDate date, String type,
            String name, BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {
        return MealFoodResult.from(mealService.updateItem(mealService.getOwnedItem(userId, itemId),
                date, toMealType(type), name, calories, carbohydrate, protein, fat));
    }

    @Transactional
    public List<MealFoodResult> addMealItems(Long userId, LocalDate date, String type,
            List<MealItemCommand> items) {
        if (items == null || items.isEmpty() || items.size() > 20 || items.stream().anyMatch(Objects::isNull)) {
            throw new NutritionRuleException("음식은 1~20개까지 한 번에 기록할 수 있습니다.");
        }
        return items.stream()
                .map(item -> MealFoodResult.from(mealService.addFood(userId, date, toMealType(type),
                        item.foodName(), item.calories(), item.carbohydrateGrams(),
                        item.proteinGrams(), item.fatGrams())))
                .toList();
    }

    public List<NutritionCalendarDayResult> calendar(Long userId, YearMonth month) {
        Map<LocalDate, List<MealFoodResult>> byDate = mealFoodRead
                .listItemsBetween(userId, month.atDay(1), month.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(MealFoodResult::mealDate,
                        TreeMap::new, Collectors.toList()));
        return byDate.entrySet().stream()
                .map(entry -> NutritionCalendarDayResult.from(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public void deleteMealItem(Long userId, Long itemId) {
        mealService.deleteItem(
                mealService.getOwnedItem(userId, itemId));
    }

    public DailyNutritionResult daily(
            Long userId,
            LocalDate date) {
        List<MealFoodResult> items =
                mealFoodRead.listDailyItems(userId, date);
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

    private static MealType toMealType(String type) {
        return type == null ? null : MealType.valueOf(type);
    }
}
