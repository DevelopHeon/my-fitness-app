package com.myfitness.exercise.infrastructure.persistence;

import com.myfitness.exercise.domain.model.CustomExercise;
import com.myfitness.exercise.application.port.out.CustomExerciseRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CustomExerciseRepositoryAdapter implements CustomExerciseRepositoryPort {
    private final SpringDataCustomExerciseRepository repository;

    public CustomExerciseRepositoryAdapter(SpringDataCustomExerciseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CustomExercise> findByIdAndUserId(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId);
    }

    @Override
    public List<CustomExercise> findAllByUserIdOrdered(Long userId) {
        return repository.findAllByUserIdOrderByCategoryAscNameAsc(userId);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCase(Long userId, String name) {
        return repository.existsByUserIdAndNameIgnoreCase(userId, name);
    }

    @Override
    public CustomExercise save(CustomExercise exercise) {
        return repository.saveAndFlush(exercise);
    }
}
