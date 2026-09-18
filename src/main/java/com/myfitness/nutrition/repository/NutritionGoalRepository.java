package com.myfitness.nutrition.repository;

import com.myfitness.nutrition.domain.NutritionGoal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionGoalRepository extends JpaRepository<NutritionGoal, Long> {
    Optional<NutritionGoal> findByUserId(Long userId);
}
