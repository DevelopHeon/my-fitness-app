package com.myfitness.nutrition.service;

import com.myfitness.nutrition.domain.Food;
import com.myfitness.nutrition.domain.ServingUnit;
import com.myfitness.nutrition.exception.NutritionAccessException;
import com.myfitness.nutrition.exception.NutritionNotFoundException;
import com.myfitness.nutrition.exception.NutritionRuleException;
import com.myfitness.nutrition.repository.FoodRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FoodService {
    private final FoodRepository foodRepository;
    private final Clock clock;

    @Autowired
    public FoodService(FoodRepository foodRepository) {
        this(foodRepository, Clock.systemDefaultZone());
    }

    FoodService(FoodRepository foodRepository, Clock clock) {
        this.foodRepository = foodRepository;
        this.clock = clock;
    }

    public Food create(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        if (foodRepository.existsByUserIdAndNameIgnoreCase(
                userId, name.trim())) {
            throw new NutritionRuleException("이미 등록된 음식 이름입니다.");
        }
        return foodRepository.saveAndFlush(Food.create(
                userId,
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams,
                clock.instant()));
    }

    public List<Food> list(Long userId, String query) {
        if (query == null || query.isBlank()) {
            return foodRepository.findAllByUserIdOrderByNameAsc(userId);
        }
        return foodRepository
                .findAllByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(
                        userId,
                        query.trim());
    }

    public Food getOwned(Long userId, Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() ->
                        new NutritionNotFoundException("음식을 찾을 수 없습니다."));
        if (!food.belongsTo(userId)) {
            throw new NutritionAccessException("다른 사용자의 음식에 접근할 수 없습니다.");
        }
        return food;
    }

    public Food update(
            Food food,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        if (foodRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(
                food.getUserId(), name.trim(), food.getId())) {
            throw new NutritionRuleException("이미 등록된 음식 이름입니다.");
        }

        food.update(
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams,
                clock.instant());
        return foodRepository.saveAndFlush(food);
    }

    public void delete(Food food) {
        foodRepository.delete(food);
        foodRepository.flush();
    }
}
