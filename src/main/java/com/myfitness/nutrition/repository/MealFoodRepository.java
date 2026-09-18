package com.myfitness.nutrition.repository;

import com.myfitness.nutrition.domain.MealFood;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    Optional<MealFood> findById(Long id);

    List<MealFood> findAllByMealUserIdAndMealMealDateOrderByCreatedAtAsc(
            Long userId,
            LocalDate mealDate);

    List<MealFood> findAllByMealUserIdOrderByMealMealDateDescCreatedAtDesc(
            Long userId);
}
