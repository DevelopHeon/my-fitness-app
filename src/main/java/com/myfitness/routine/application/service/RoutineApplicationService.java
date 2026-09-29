package com.myfitness.routine.application.service;

import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.routine.application.dto.request.ExerciseSelection;
import com.myfitness.routine.application.dto.response.RoutineResult;
import com.myfitness.routine.application.dto.response.RoutineWorkoutStartResult;
import com.myfitness.routine.application.port.in.RoutineUseCase;
import com.myfitness.routine.domain.model.Routine;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.RoutineWorkoutView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoutineApplicationService implements RoutineUseCase {
    private final RoutineService routineService;
    private final ExerciseCatalogQuery exerciseCatalogQuery;
    private final WorkoutRoutineUseCase workoutRoutineUseCase;

    public RoutineApplicationService(
            RoutineService routineService,
            ExerciseCatalogQuery exerciseCatalogQuery,
            WorkoutRoutineUseCase workoutRoutineUseCase) {
        this.routineService = routineService;
        this.exerciseCatalogQuery = exerciseCatalogQuery;
        this.workoutRoutineUseCase = workoutRoutineUseCase;
    }

    @Transactional
    public RoutineResult create(
            Long userId,
            String name,
            List<ExerciseSelection> selections) {
        List<ExerciseReference> exercises =
                resolveExercises(userId, selections);
        return RoutineResult.from(
                routineService.create(userId, name, exercises));
    }

    public List<RoutineResult> list(Long userId) {
        return routineService.list(userId).stream()
                .map(RoutineResult::from)
                .toList();
    }

    public RoutineResult get(Long userId, Long routineId) {
        return RoutineResult.from(
                routineService.getOwned(userId, routineId));
    }

    @Transactional
    public RoutineResult update(
            Long userId,
            Long routineId,
            String name,
            List<ExerciseSelection> selections) {
        Routine routine = routineService.getOwned(userId, routineId);
        List<ExerciseReference> exercises =
                resolveExercises(userId, selections);
        return RoutineResult.from(
                routineService.update(routine, name, exercises));
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

        RoutineWorkoutView result =
                workoutRoutineUseCase.startWorkout(
                        userId, workoutDate, memo, exercises);

        return new RoutineWorkoutStartResult(
                result.workout(),
                result.previousRecords());
    }

    private List<ExerciseReference> resolveExercises(
            Long userId,
            List<ExerciseSelection> selections) {
        return selections.stream()
                .map(selection -> exerciseCatalogQuery.getAvailable(
                        userId,
                        selection.exerciseType(),
                        selection.exerciseId()))
                .toList();
    }
}
