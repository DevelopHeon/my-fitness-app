package com.myfitness.exercise.domain.repository;

import com.myfitness.exercise.domain.model.CustomExercise;
import java.util.List;
import java.util.Optional;

public interface CustomExerciseRepository {
    Optional<CustomExercise> findByIdAndUserId(Long id, Long userId);
    List<CustomExercise> findAllByUserIdOrdered(Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    CustomExercise save(CustomExercise exercise);
}
