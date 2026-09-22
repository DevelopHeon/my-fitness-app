package com.myfitness.exercise.application.port.out;

import com.myfitness.exercise.domain.model.Exercise;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepositoryPort {
    Optional<Exercise> findById(Long id);
    List<Exercise> findAllOrdered();
    boolean existsByNameIgnoreCase(String name);
    Exercise save(Exercise exercise);
}
