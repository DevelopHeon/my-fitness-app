package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.Meal;
import com.myfitness.nutrition.domain.model.MealType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMealRepository extends JpaRepository<Meal, Long> {
    Optional<Meal> findByUserIdAndMealDateAndMealType(
            Long userId, LocalDate mealDate, MealType mealType);
}
