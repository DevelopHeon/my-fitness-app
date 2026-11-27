package com.myfitness.exercise.infrastructure.persistence;

import com.myfitness.exercise.domain.model.Exercise;
import com.myfitness.exercise.domain.repository.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ExerciseRepositoryAdapter implements ExerciseRepository {
    private final SpringDataExerciseRepository repository;

    public ExerciseRepositoryAdapter(SpringDataExerciseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Exercise> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Exercise> findAllOrdered() {
        return repository.findAllByOrderByCategoryAscSortOrderAscNameAsc();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public Exercise save(Exercise exercise) {
        return repository.saveAndFlush(exercise);
    }
}
