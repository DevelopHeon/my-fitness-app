package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.request.WorkoutSetCommand;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.usecase.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.port.in.WorkoutPort;
import com.myfitness.workout.application.port.in.WorkoutCalendarDayView;
import com.myfitness.workout.application.port.in.routine.PreviousExerciseRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutinePort;
import com.myfitness.workout.application.port.in.routine.WorkoutView;
import com.myfitness.workout.application.usecase.WorkoutRoutineUseCase;
import com.myfitness.workout.application.usecase.WorkoutUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkoutPortService implements WorkoutPort, WorkoutRoutinePort {
    private final WorkoutUseCase useCase;
    private final WorkoutRoutineUseCase routine;

    public WorkoutPortService(WorkoutUseCase useCase, WorkoutRoutineUseCase routine) {
        this.useCase = useCase;
        this.routine = routine;
    }

    @Override
    public PreviousExerciseRecordView getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        return toPrevious(useCase.getPreviousExerciseRecord(userId, exerciseType.name(), exerciseId));
    }

    @Override
    public WorkoutView startWorkout(Long userId, LocalDate workoutDate, String memo) {
        return toView(useCase.startWorkout(userId, workoutDate, memo));
    }

    @Override
    public WorkoutView getWorkout(Long userId, Long workoutId) {
        return toView(useCase.getWorkout(userId, workoutId));
    }

    @Override
    public List<WorkoutView> getWorkouts(Long userId, LocalDate from, LocalDate to) {
        return useCase.getWorkouts(userId, from, to).stream().map(WorkoutPortService::toView).toList();
    }

    @Override
    public List<WorkoutCalendarDayView> getCalendar(Long userId, YearMonth month) {
        return useCase.getCalendar(userId, month).stream().map(WorkoutPortService::toDay).toList();
    }

    @Override
    public WorkoutView addExercise(
            Long userId,
            Long workoutId,
            ExerciseType exerciseType,
            Long exerciseId,
            String memo) {
        return toView(useCase.addExercise(userId, workoutId, exerciseType.name(), exerciseId, memo));
    }

    @Override
    public WorkoutView removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId) {
        return toView(useCase.removeExercise(userId, workoutId, workoutExerciseId));
    }

    @Override
    public WorkoutView addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        return toView(useCase.addSet(userId, workoutId, workoutExerciseId, weightKg, reps, durationSeconds, completed));
    }

    @Override
    public WorkoutView addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetInput> commands) {
        List<WorkoutSetCommand> inputs = commands.stream()
                .map(command -> new WorkoutSetCommand(
                        command.weightKg(), command.reps(), command.durationSeconds(), command.completed()))
                .toList();
        return toView(useCase.addSets(userId, workoutId, workoutExerciseId, inputs));
    }

    @Override
    public WorkoutView updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        return toView(useCase.updateSet(
                userId, workoutId, workoutExerciseId, setId, weightKg, reps, durationSeconds, completed));
    }

    @Override
    public WorkoutView removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId) {
        return toView(useCase.removeSet(userId, workoutId, workoutExerciseId, setId));
    }

    @Override
    public WorkoutView completeWorkout(Long userId, Long workoutId) {
        return toView(useCase.completeWorkout(userId, workoutId));
    }

    @Override
    public WorkoutView reopenWorkout(Long userId, Long workoutId) {
        return toView(useCase.reopenWorkout(userId, workoutId));
    }

    @Override
    public RoutineWorkoutView startWorkout(Long userId, LocalDate workoutDate, String memo, List<ExerciseSnapshot> exercises) {
        WorkoutRoutineUseCase.Started started = routine.startWorkout(
                userId, workoutDate, memo, exercises.stream()
                        .map(exercise -> new WorkoutRoutineUseCase.Snapshot(
                                exercise.type(), exercise.id(), exercise.name(), exercise.category(), exercise.ownerUserId()))
                        .toList());
        return new RoutineWorkoutView(
                toView(started.workout()), started.previousRecords().stream().map(WorkoutPortService::toPrevious).toList());
    }

    private static WorkoutView toView(WorkoutResult workout) {
        return new WorkoutView(
                workout.id(), workout.workoutDate(), workout.status(), workout.memo(), workout.startedAt(),
                workout.completedAt(), workout.exercises().stream().map(WorkoutPortService::toExercise).toList());
    }

    private static WorkoutView.ExerciseView toExercise(WorkoutResult.ExerciseResult exercise) {
        return new WorkoutView.ExerciseView(
                exercise.id(), exercise.exerciseType(), exercise.exerciseId(), exercise.exerciseName(),
                exercise.category(), exercise.orderIndex(), exercise.memo(),
                exercise.sets().stream().map(WorkoutPortService::toSet).toList());
    }

    private static WorkoutView.SetView toSet(WorkoutResult.SetResult set) {
        return new WorkoutView.SetView(
                set.id(), set.setNumber(), set.weightKg(), set.reps(), set.durationSeconds(), set.completed());
    }

    private static PreviousExerciseRecordView toPrevious(PreviousExerciseRecordResult previous) {
        if (previous == null) {
            return null;
        }
        return new PreviousExerciseRecordView(
                previous.workoutId(), previous.workoutDate(), previous.workoutExerciseId(),
                previous.exerciseType(), previous.exerciseId(), previous.exerciseName(),
                previous.sets().stream().map(WorkoutPortService::toSet).toList());
    }

    private static WorkoutCalendarDayView toDay(WorkoutCalendarDayResult day) {
        return new WorkoutCalendarDayView(day.date(), day.workoutCount(), day.completedCount(), day.exerciseNames());
    }
}
