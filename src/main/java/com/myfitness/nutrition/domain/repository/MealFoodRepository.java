package com.myfitness.nutrition.domain.repository;

import com.myfitness.nutrition.domain.model.MealFood;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealFoodRepository {
    MealFood save(MealFood item);
    Optional<MealFood> findById(Long id);
    List<MealFood> findDailyItems(Long userId, LocalDate mealDate);
    List<MealFood> findUsageHistory(Long userId);
    void delete(MealFood item);
}
