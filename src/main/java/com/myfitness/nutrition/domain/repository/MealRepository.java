package com.myfitness.nutrition.domain.repository;

import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealType;
import java.time.LocalDate;
import java.util.Optional;

public interface MealRepository {
    Meal save(Meal meal);
    Optional<Meal> findByUserIdAndDateAndType(
            Long userId, LocalDate date, MealType mealType);
}
