package com.myfitness.routine.application.service;

import com.myfitness.exercise.application.service.ExerciseService;
import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.routine.application.command.ExerciseSelection;
import com.myfitness.routine.application.result.RoutineWorkoutStartResult;
import com.myfitness.routine.domain.model.Routine;
import com.myfitness.workout.application.service.WorkoutService;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoutineApplicationService {
    private final RoutineService routineService;
    private final ExerciseService exerciseService;
    private final WorkoutService workoutService;

    public RoutineApplicationService(
            RoutineService routineService,
            ExerciseService exerciseService,
            WorkoutService workoutService) {
        this.routineService = routineService;
        this.exerciseService = exerciseService;
        this.workoutService = workoutService;
    }

    @Transactional
    public Routine create(
            Long userId,
            String name,
            List<ExerciseSelection> selections) {
        List<ExerciseReference> exercises =
                resolveExercises(userId, selections);
        return routineService.create(userId, name, exercises);
    }

    public List<Routine> list(Long userId) {
        return routineService.list(userId);
    }

    public Routine get(Long userId, Long routineId) {
        return routineService.getOwned(userId, routineId);
    }

    @Transactional
    public Routine update(
            Long userId,
            Long routineId,
            String name,
            List<ExerciseSelection> selections) {
        Routine routine = routineService.getOwned(userId, routineId);
        List<ExerciseReference> exercises =
                resolveExercises(userId, selections);
        return routineService.update(routine, name, exercises);
    }

    @Transactional
    public void delete(Long userId, Long routineId) {
        routineService.delete(routineService.getOwned(userId, routineId));
    }

    @Transactional
    public RoutineWorkoutStartResult startWorkout(
            Long userId,
            Long routineId,
            LocalDate workoutDate,
            String memo) {
        Routine routine = routineService.getOwned(userId, routineId);
        List<ExerciseReference> exercises = routine.getExercises().stream()
                .map(entry -> entry.toReference())
                .toList();

        Workout workout = workoutService.startWithExercises(
                userId, workoutDate, memo, exercises);

        List<WorkoutExercise> previousRecords =
                workout.getExercises().stream()
                        .map(entry ->
                                workoutService.getPreviousCompletedExercise(
                                        userId,
                                        entry.getExerciseType(),
                                        entry.getExerciseId()))
                        .filter(previous -> previous != null)
                        .toList();

        return new RoutineWorkoutStartResult(
                workout, previousRecords);
    }

    private List<ExerciseReference> resolveExercises(
            Long userId,
            List<ExerciseSelection> selections) {
        return selections.stream()
                .map(selection -> exerciseService.getAvailable(
                        userId,
                        selection.exerciseType(),
                        selection.exerciseId()))
                .toList();
    }
}
