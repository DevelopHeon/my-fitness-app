package com.myfitness.routine.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface RoutinePort {
    RoutineView create(
            Long userId,
            String name,
            List<SelectedExercise> selections);

    List<RoutineView> list(Long userId);

    RoutineView get(Long userId, Long routineId);

    RoutineView update(
            Long userId,
            Long routineId,
            String name,
            List<SelectedExercise> selections);

    void delete(Long userId, Long routineId);

    RoutineWorkoutStartView startWorkout(
            Long userId,
            Long routineId,
            LocalDate workoutDate,
            String memo);

    enum ExerciseType {
        DEFAULT, CUSTOM
    }

    record SelectedExercise(ExerciseType exerciseType, Long exerciseId) {}
}
