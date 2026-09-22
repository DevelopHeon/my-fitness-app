package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataNutritionGoalRepository extends JpaRepository<NutritionGoal, Long> {
    Optional<NutritionGoal> findByUserId(Long userId);
}
