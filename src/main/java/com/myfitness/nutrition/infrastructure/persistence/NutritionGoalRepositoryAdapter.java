package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.domain.repository.NutritionGoalRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class NutritionGoalRepositoryAdapter implements NutritionGoalRepository {
    private final SpringDataNutritionGoalRepository repository;

    public NutritionGoalRepositoryAdapter(SpringDataNutritionGoalRepository repository) {
        this.repository = repository;
    }

    @Override public Optional<NutritionGoal> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override public NutritionGoal save(NutritionGoal goal) {
        return repository.saveAndFlush(goal);
    }
}
