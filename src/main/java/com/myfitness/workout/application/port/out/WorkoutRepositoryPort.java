package com.myfitness.workout.application.port.out;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepositoryPort {
    Workout save(Workout workout);
    Optional<Workout> findById(Long id);
    List<Workout> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to);
    List<Workout> findByUserIdAndStatus(Long userId, WorkoutStatus status);
}
