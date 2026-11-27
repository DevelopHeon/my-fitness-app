package com.myfitness.routine.application.service;

import com.myfitness.routine.application.usecase.RoutineOperationsUseCase;
import com.myfitness.routine.application.port.out.ExerciseCatalogPort;
import com.myfitness.routine.domain.model.ExerciseSnapshot;
import com.myfitness.routine.application.usecase.request.ExerciseSelection;
import com.myfitness.routine.application.usecase.response.RoutineResult;
import com.myfitness.routine.application.usecase.response.RoutineWorkoutStartResult;
import com.myfitness.routine.application.usecase.RoutineUseCase;
import com.myfitness.routine.domain.model.Routine;
import com.myfitness.routine.domain.model.ExerciseType;
import com.myfitness.routine.application.port.out.RoutineWorkoutPort;
import com.myfitness.routine.application.port.out.RoutineReadPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoutineApplicationService implements RoutineUseCase {
    private final RoutineOperationsUseCase routineService;
    private final RoutineReadPort routineRead;
    private final ExerciseCatalogPort exerciseCatalog;
    private final RoutineWorkoutPort routineWorkout;

    public RoutineApplicationService(
            RoutineOperationsUseCase routineService,
            ExerciseCatalogPort exerciseCatalog,
            RoutineWorkoutPort routineWorkout, RoutineReadPort routineRead) {
        this.routineService = routineService;
        this.exerciseCatalog = exerciseCatalog;
        this.routineWorkout = routineWorkout;
        this.routineRead = routineRead;
    }

    @Transactional
    public RoutineResult create(
            Long userId,
            String name,
            List<ExerciseSelection> selections) {
        List<ExerciseSnapshot> exercises =
                resolveExercises(userId, selections);
        return RoutineResult.from(
                routineService.create(userId, name, exercises));
    }

    public List<RoutineResult> list(Long userId) {
        return routineRead.findAllByUserId(userId);
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
        List<ExerciseSnapshot> exercises =
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
        List<ExerciseSnapshot> exercises = routine.getExercises().stream()
                .map(entry -> entry.toReference())
                .toList();

        return routineWorkout.startWorkout(userId, workoutDate, memo, exercises);
    }

    private List<ExerciseSnapshot> resolveExercises(
            Long userId,
            List<ExerciseSelection> selections) {
        return selections.stream()
                .map(selection -> exerciseCatalog.getAvailable(
                        userId,
                        ExerciseType.valueOf(selection.exerciseType()),
                        selection.exerciseId()))
                .toList();
    }
}
