package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealType;
import com.myfitness.nutrition.application.port.out.MealRepositoryPort;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MealRepositoryAdapter implements MealRepositoryPort {
    private final SpringDataMealRepository repository;

    public MealRepositoryAdapter(SpringDataMealRepository repository) {
        this.repository = repository;
    }

    @Override public Meal save(Meal meal) { return repository.saveAndFlush(meal); }
    @Override public Optional<Meal> findByUserIdAndDateAndType(
            Long userId, LocalDate date, MealType mealType) {
        return repository.findByUserIdAndMealDateAndMealType(userId, date, mealType);
    }
}
