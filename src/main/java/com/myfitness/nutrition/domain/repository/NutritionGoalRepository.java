package com.myfitness.nutrition.domain.repository;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.util.Optional;

public interface NutritionGoalRepository {
    Optional<NutritionGoal> findByUserId(Long userId);
    NutritionGoal save(NutritionGoal goal);
}
