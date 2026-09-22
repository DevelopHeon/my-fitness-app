package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.application.exception.NutritionAccessException;
import com.myfitness.nutrition.application.exception.NutritionNotFoundException;
import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import com.myfitness.nutrition.application.port.out.MealFoodRepositoryPort;
import com.myfitness.nutrition.application.port.out.MealRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MealService {
    private final MealRepositoryPort mealRepository;
    private final MealFoodRepositoryPort mealFoodRepository;
    private final Clock clock;

    @Autowired
    public MealService(
            MealRepositoryPort mealRepository,
            MealFoodRepositoryPort mealFoodRepository) {
        this(mealRepository, mealFoodRepository, Clock.systemDefaultZone());
    }

    MealService(
            MealRepositoryPort mealRepository,
            MealFoodRepositoryPort mealFoodRepository,
            Clock clock) {
        this.mealRepository = mealRepository;
        this.mealFoodRepository = mealFoodRepository;
        this.clock = clock;
    }

    public MealFood addFood(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Food food,
            BigDecimal servings) {
        validateMealDate(mealDate);

        Meal meal = mealRepository
                .findByUserIdAndDateAndType(
                        userId, mealDate, mealType)
                .orElseGet(() -> mealRepository.save(
                        Meal.create(
                                userId,
                                mealDate,
                                mealType,
                                clock.instant())));

        MealFood item = MealFood.fromFood(
                meal, food, servings, clock.instant());
        meal.touch(clock.instant());
        mealRepository.save(meal);
        return mealFoodRepository.save(item);
    }

    public MealFood getOwnedItem(Long userId, Long itemId) {
        MealFood item = mealFoodRepository.findById(itemId)
                .orElseThrow(() ->
                        new NutritionNotFoundException(
                                "식단 항목을 찾을 수 없습니다."));
        if (!item.getMeal().belongsTo(userId)) {
            throw new NutritionAccessException(
                    "다른 사용자의 식단 항목에 접근할 수 없습니다.");
        }
        return item;
    }

    public MealFood updateItem(MealFood item, BigDecimal servings) {
        item.updateServings(servings, clock.instant());
        item.getMeal().touch(clock.instant());
        mealRepository.save(item.getMeal());
        return mealFoodRepository.save(item);
    }

    public void deleteItem(MealFood item) {
        Meal meal = item.getMeal();
        mealFoodRepository.delete(item);
        meal.touch(clock.instant());
        mealRepository.save(meal);
    }

    public List<MealFood> listDailyItems(
            Long userId,
            LocalDate mealDate) {
        return mealFoodRepository
                .findDailyItems(
                        userId,
                        mealDate);
    }

    public List<MealFood> listUsageHistory(Long userId) {
        return mealFoodRepository
                .findUsageHistory(
                        userId);
    }

    private void validateMealDate(LocalDate mealDate) {
        if (mealDate == null) {
            throw new NutritionRuleException("식사 날짜는 필수입니다.");
        }
        if (mealDate.isAfter(LocalDate.now(clock))) {
            throw new NutritionRuleException(
                    "미래 날짜로 식단을 기록할 수 없습니다.");
        }
    }
}
