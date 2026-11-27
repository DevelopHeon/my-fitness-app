package com.myfitness.nutrition.domain.repository;

import com.myfitness.nutrition.domain.model.MealFood;
import java.util.Optional;

public interface MealFoodRepository {
    MealFood save(MealFood item);
    Optional<MealFood> findById(Long id);
    void delete(MealFood item);
}
