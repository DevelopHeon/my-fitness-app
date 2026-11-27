package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.domain.model.WorkoutStatus;
import com.myfitness.workout.application.port.out.PreviousExerciseRecordQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutExerciseRepositoryAdapter implements PreviousExerciseRecordQueryPort {
    private final SpringDataWorkoutExerciseRepository repository;

    public WorkoutExerciseRepositoryAdapter(SpringDataWorkoutExerciseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PreviousExerciseRecordResult> findLatestCompletedByExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        return repository
                .findFirstByWorkout_UserIdAndExerciseTypeAndExerciseIdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
                        userId, exerciseType, exerciseId, WorkoutStatus.COMPLETED)
                .map(PreviousExerciseRecordResult::from);
    }
}
