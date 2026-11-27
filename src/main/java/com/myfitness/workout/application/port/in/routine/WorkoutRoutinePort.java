package com.myfitness.workout.application.port.in.routine;

import java.time.LocalDate;
import java.util.List;

public interface WorkoutRoutinePort {
    RoutineWorkoutView startWorkout(Long userId, LocalDate workoutDate, String memo, List<ExerciseSnapshot> exercises);

    record ExerciseSnapshot(String type, Long id, String name, String category, Long ownerUserId) {}

    record RoutineWorkoutView(WorkoutView workout, List<PreviousExerciseRecordView> previousRecords) {}
}
