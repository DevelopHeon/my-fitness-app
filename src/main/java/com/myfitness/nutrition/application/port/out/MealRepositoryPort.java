package com.myfitness.nutrition.application.port.out;

import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealType;
import java.time.LocalDate;
import java.util.Optional;

public interface MealRepositoryPort {
    Meal save(Meal meal);
    Optional<Meal> findByUserIdAndDateAndType(
            Long userId, LocalDate date, MealType mealType);
}
