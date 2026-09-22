package com.myfitness.nutrition.domain.repository;

import com.myfitness.nutrition.domain.model.Food;
import java.util.List;
import java.util.Optional;

public interface FoodRepository {
    Food save(Food food);
    Optional<Food> findById(Long id);
    List<Food> findAllByUserId(Long userId);
    List<Food> searchByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
    void delete(Food food);
}
