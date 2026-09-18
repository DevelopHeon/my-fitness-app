package com.myfitness.workout.repository;

import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.domain.WorkoutStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {

    @EntityGraph(attributePaths = {"exercise", "sets", "workout"})
    Optional<WorkoutExercise> findFirstByWorkout_UserIdAndExercise_IdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
            Long userId, Long exerciseId, WorkoutStatus status);
}
