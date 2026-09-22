package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataWorkoutExerciseRepository
        extends JpaRepository<WorkoutExercise, Long> {
    @EntityGraph(attributePaths = {"sets", "workout"})
    Optional<WorkoutExercise>
            findFirstByWorkout_UserIdAndExerciseTypeAndExerciseIdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
                    Long userId,
                    ExerciseType exerciseType,
                    Long exerciseId,
                    WorkoutStatus status);
}
