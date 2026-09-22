package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.Food;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFoodRepository extends JpaRepository<Food, Long> {
    List<Food> findAllByUserIdOrderByNameAsc(Long userId);
    List<Food> findAllByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(
            Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
