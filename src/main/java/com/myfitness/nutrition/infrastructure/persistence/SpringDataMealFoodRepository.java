package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.MealFood;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMealFoodRepository extends JpaRepository<MealFood, Long> {
    List<MealFood> findAllByMealUserIdAndMealMealDateOrderByCreatedAtAsc(
            Long userId, LocalDate mealDate);
    List<MealFood> findAllByMealUserIdOrderByMealMealDateDescCreatedAtDesc(Long userId);

    List<MealFood> findAllByMealUserIdOrderByMealMealDateDescCreatedAtDesc(
            Long userId,
            Pageable pageable);
}
