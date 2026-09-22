package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutStatus;
import com.myfitness.workout.domain.repository.WorkoutExerciseRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutExerciseRepositoryAdapter implements WorkoutExerciseRepository {
    private final SpringDataWorkoutExerciseRepository repository;

    public WorkoutExerciseRepositoryAdapter(SpringDataWorkoutExerciseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkoutExercise> findLatestCompletedByExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        return repository
                .findFirstByWorkout_UserIdAndExerciseTypeAndExerciseIdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
                        userId, exerciseType, exerciseId, WorkoutStatus.COMPLETED);
    }
}
