package com.myfitness.workout.application.usecase;

import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutRoutineUseCase {
    Started startWorkout(Long userId, LocalDate workoutDate, String memo, List<Snapshot> exercises);

    record Snapshot(String type, Long id, String name, String category, Long ownerUserId) {}

    record Started(WorkoutResult workout, List<PreviousExerciseRecordResult> previousRecords) {}
}
