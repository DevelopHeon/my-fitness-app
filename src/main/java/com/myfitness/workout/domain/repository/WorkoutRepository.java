package com.myfitness.workout.domain.repository;

import com.myfitness.workout.domain.model.Workout;
import java.util.Optional;

public interface WorkoutRepository {
    Workout save(Workout workout);
    Optional<Workout> findById(Long id);
}
