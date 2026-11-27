package com.myfitness.routine.application.service;

import com.myfitness.routine.application.usecase.request.ExerciseSelection;
import com.myfitness.routine.application.usecase.response.RoutineResult;
import com.myfitness.routine.application.usecase.response.RoutineWorkoutStartResult;
import com.myfitness.routine.application.port.in.RoutinePort;
import com.myfitness.routine.application.port.in.RoutineView;
import com.myfitness.routine.application.port.in.RoutineWorkoutStartView;
import com.myfitness.routine.application.usecase.RoutineUseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoutinePortService implements RoutinePort {
    private final RoutineUseCase useCase;

    public RoutinePortService(RoutineUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public RoutineView create(
            Long userId,
            String name,
            List<SelectedExercise> selections) {
        return toView(useCase.create(userId, name, toSelections(selections)));
    }

    @Override
    public List<RoutineView> list(Long userId) {
        return useCase.list(userId).stream().map(RoutinePortService::toView).toList();
    }

    @Override
    public RoutineView get(Long userId, Long routineId) {
        return toView(useCase.get(userId, routineId));
    }

    @Override
    public RoutineView update(
            Long userId,
            Long routineId,
            String name,
            List<SelectedExercise> selections) {
        return toView(useCase.update(userId, routineId, name, toSelections(selections)));
    }

    @Override
    public void delete(Long userId, Long routineId) {
        useCase.delete(userId, routineId);
    }

    @Override
    public RoutineWorkoutStartView startWorkout(
            Long userId,
            Long routineId,
            LocalDate workoutDate,
            String memo) {
        return toWorkoutStart(useCase.startWorkout(userId, routineId, workoutDate, memo));
    }

    private static List<ExerciseSelection> toSelections(List<SelectedExercise> selections) {
        return selections.stream().map(exercise ->
                new ExerciseSelection(exercise.exerciseType().name(), exercise.exerciseId())).toList();
    }

    private static RoutineView toView(RoutineResult routine) {
        return new RoutineView(
                routine.id(), routine.name(), routine.createdAt(), routine.updatedAt(),
                routine.exercises().stream().map(exercise -> new RoutineView.ExerciseView(
                        exercise.id(), exercise.exerciseType(), exercise.exerciseId(), exercise.exerciseName(),
                        exercise.category(), exercise.orderIndex())).toList());
    }

    private static RoutineWorkoutStartView toWorkoutStart(RoutineWorkoutStartResult result) {
        RoutineWorkoutStartResult.Workout workout = result.workout();
        return new RoutineWorkoutStartView(
                new RoutineWorkoutStartView.Workout(
                        workout.id(), workout.workoutDate(), workout.status(), workout.memo(), workout.startedAt(),
                        workout.completedAt(), workout.exercises().stream().map(exercise ->
                                new RoutineWorkoutStartView.Exercise(
                                        exercise.id(), exercise.exerciseType(), exercise.exerciseId(),
                                        exercise.exerciseName(), exercise.category(), exercise.orderIndex(),
                                        exercise.memo(), toSets(exercise.sets()))).toList()),
                result.previousRecords().stream().map(previous -> new RoutineWorkoutStartView.PreviousRecord(
                        previous.workoutId(), previous.workoutDate(), previous.workoutExerciseId(),
                        previous.exerciseType(), previous.exerciseId(), previous.exerciseName(),
                        toSets(previous.sets()))).toList());
    }

    private static List<RoutineWorkoutStartView.Set> toSets(List<RoutineWorkoutStartResult.Set> sets) {
        return sets.stream().map(set -> new RoutineWorkoutStartView.Set(
                set.id(), set.setNumber(), set.weightKg(), set.reps(), set.durationSeconds(), set.completed())).toList();
    }
}
