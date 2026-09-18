package com.myfitness.nutrition.repository;

import com.myfitness.nutrition.domain.Meal;
import com.myfitness.nutrition.domain.MealType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {
    Optional<Meal> findByUserIdAndMealDateAndMealType(
            Long userId,
            LocalDate mealDate,
            MealType mealType);

    List<Meal> findAllByUserIdAndMealDateOrderByMealTypeAsc(
            Long userId,
            LocalDate mealDate);

    List<Meal> findAllByUserIdOrderByMealDateDescUpdatedAtDesc(Long userId);
}
