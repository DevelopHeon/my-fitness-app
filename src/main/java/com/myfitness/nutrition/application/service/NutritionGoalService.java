package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.repository.NutritionGoalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NutritionGoalService {
    private final NutritionGoalRepository nutritionGoalRepository;
    private final Clock clock;

    @Autowired
    public NutritionGoalService(
            NutritionGoalRepository nutritionGoalRepository) {
        this(nutritionGoalRepository, Clock.systemDefaultZone());
    }

    NutritionGoalService(
            NutritionGoalRepository nutritionGoalRepository,
            Clock clock) {
        this.nutritionGoalRepository = nutritionGoalRepository;
        this.clock = clock;
    }

    public Optional<NutritionGoal> get(Long userId) {
        return nutritionGoalRepository.findByUserId(userId);
    }

    public NutritionGoal upsert(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        NutritionGoal goal = nutritionGoalRepository.findByUserId(userId)
                .orElseGet(() -> NutritionGoal.create(
                        userId,
                        calories,
                        carbohydrateGrams,
                        proteinGrams,
                        fatGrams,
                        clock.instant()));

        if (goal.getId() != null) {
            goal.update(
                    calories,
                    carbohydrateGrams,
                    proteinGrams,
                    fatGrams,
                    clock.instant());
        }

        return nutritionGoalRepository.save(goal);
    }
}
