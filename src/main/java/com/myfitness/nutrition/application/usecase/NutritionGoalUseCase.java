package com.myfitness.nutrition.application.usecase;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.math.BigDecimal;
import java.util.Optional;

public interface NutritionGoalUseCase {
    Optional<NutritionGoal> get(Long userId);

    NutritionGoal upsert(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams);
}
