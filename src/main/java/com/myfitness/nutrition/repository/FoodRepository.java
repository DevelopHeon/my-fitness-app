package com.myfitness.nutrition.repository;

import com.myfitness.nutrition.domain.Food;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findAllByUserIdOrderByNameAsc(Long userId);

    List<Food> findAllByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long userId,
            String name);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            Long userId,
            String name,
            Long id);

    Optional<Food> findById(Long id);
}
