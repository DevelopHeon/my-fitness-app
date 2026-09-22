package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.Food;
import com.myfitness.nutrition.application.port.out.FoodRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FoodRepositoryAdapter implements FoodRepositoryPort {
    private final SpringDataFoodRepository repository;

    public FoodRepositoryAdapter(SpringDataFoodRepository repository) {
        this.repository = repository;
    }

    @Override public Food save(Food food) { return repository.saveAndFlush(food); }
    @Override public Optional<Food> findById(Long id) { return repository.findById(id); }
    @Override public List<Food> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByNameAsc(userId);
    }
    @Override public List<Food> searchByUserIdAndName(Long userId, String name) {
        return repository.findAllByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(userId, name);
    }
    @Override public boolean existsByUserIdAndNameIgnoreCase(Long userId, String name) {
        return repository.existsByUserIdAndNameIgnoreCase(userId, name);
    }
    @Override public boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            Long userId, String name, Long id) {
        return repository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id);
    }
    @Override public void delete(Food food) {
        repository.delete(food);
        repository.flush();
    }
}
