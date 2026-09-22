package com.myfitness.nutrition.application.port.out;

import com.myfitness.nutrition.domain.model.MealFood;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealFoodRepositoryPort {
    MealFood save(MealFood item);
    Optional<MealFood> findById(Long id);
    List<MealFood> findDailyItems(Long userId, LocalDate mealDate);
    List<MealFood> findUsageHistory(Long userId);
    List<MealFood> findRecentUsageHistory(Long userId, int limit);
    void delete(MealFood item);
}
