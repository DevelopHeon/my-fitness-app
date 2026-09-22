package com.myfitness.nutrition.application.port.out;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.util.Optional;

public interface NutritionGoalRepositoryPort {
    Optional<NutritionGoal> findByUserId(Long userId);
    NutritionGoal save(NutritionGoal goal);
}
