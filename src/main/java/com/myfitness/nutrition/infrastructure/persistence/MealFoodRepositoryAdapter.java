package com.myfitness.nutrition.infrastructure.persistence;

import com.myfitness.nutrition.domain.model.MealFood;
import com.myfitness.nutrition.application.port.out.MealFoodRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class MealFoodRepositoryAdapter implements MealFoodRepositoryPort {
    private final SpringDataMealFoodRepository repository;

    public MealFoodRepositoryAdapter(SpringDataMealFoodRepository repository) {
        this.repository = repository;
    }

    @Override public MealFood save(MealFood item) { return repository.saveAndFlush(item); }
    @Override public Optional<MealFood> findById(Long id) { return repository.findById(id); }
    @Override public List<MealFood> findDailyItems(Long userId, LocalDate mealDate) {
        return repository.findAllByMealUserIdAndMealMealDateOrderByCreatedAtAsc(userId, mealDate);
    }
    @Override public List<MealFood> findUsageHistory(Long userId) {
        return repository.findAllByMealUserIdOrderByMealMealDateDescCreatedAtDesc(userId);
    }

    @Override
    public List<MealFood> findRecentUsageHistory(Long userId, int limit) {
        return repository
                .findAllByMealUserIdOrderByMealMealDateDescCreatedAtDesc(
                        userId,
                        PageRequest.of(0, Math.max(1, limit)));
    }
    @Override public void delete(MealFood item) {
        repository.delete(item);
        repository.flush();
    }
}
