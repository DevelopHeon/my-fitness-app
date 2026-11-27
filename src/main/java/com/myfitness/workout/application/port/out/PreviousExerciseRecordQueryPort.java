package com.myfitness.workout.application.port.out;

import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import java.util.Optional;

public interface PreviousExerciseRecordQueryPort {
    Optional<PreviousExerciseRecordResult> findLatestCompletedByExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId);
}
