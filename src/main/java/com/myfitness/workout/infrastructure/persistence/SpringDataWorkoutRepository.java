package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataWorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(
            Long userId, LocalDate from, LocalDate to);

    List<Workout> findAllByUserIdAndStatusOrderByWorkoutDateAscStartedAtAsc(
            Long userId, WorkoutStatus status);

    List<Workout>
            findAllByUserIdAndStatusAndWorkoutDateGreaterThanEqualOrderByWorkoutDateDescStartedAtDesc(
                    Long userId,
                    WorkoutStatus status,
                    LocalDate from);
}
