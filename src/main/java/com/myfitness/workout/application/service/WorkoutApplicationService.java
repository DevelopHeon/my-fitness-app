package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.WorkoutOperationsUseCase;
import com.myfitness.workout.application.port.out.ExerciseCatalogPort;
import com.myfitness.workout.application.port.out.WorkoutReadPort;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.application.usecase.request.WorkoutSetCommand;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.usecase.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.usecase.WorkoutUseCase;
import com.myfitness.workout.domain.model.Workout;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutApplicationService implements WorkoutUseCase {
    private final WorkoutOperationsUseCase workoutService;
    private final WorkoutReadPort workoutRead;
    private final Clock clock = Clock.systemDefaultZone();
    private final ExerciseCatalogPort exerciseCatalog;

    public WorkoutApplicationService(
            WorkoutOperationsUseCase workoutService,
            ExerciseCatalogPort exerciseCatalog, WorkoutReadPort workoutRead) {
        this.workoutService = workoutService;
        this.exerciseCatalog = exerciseCatalog;
        this.workoutRead = workoutRead;
    }

    public PreviousExerciseRecordResult getPreviousExerciseRecord(
            Long userId,
            String exerciseType,
            Long exerciseId) {
        ExerciseType type = ExerciseType.valueOf(exerciseType);
        exerciseCatalog.getAvailable(userId, type, exerciseId);
        return workoutService.getPreviousCompletedExercise(userId, type, exerciseId);
    }

    @Transactional
    public WorkoutResult startWorkout(
            Long userId,
            LocalDate workoutDate,
            String memo) {
        return WorkoutResult.from(
                workoutService.start(userId, workoutDate, memo));
    }

    public WorkoutResult getWorkout(Long userId, Long workoutId) {
        return WorkoutResult.from(
                workoutService.getOwned(userId, workoutId));
    }

    public List<WorkoutResult> getWorkouts(
            Long userId,
            LocalDate from,
            LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        return workoutRead.findByUserIdAndDateRange(userId, start, end);
    }

    public List<WorkoutCalendarDayResult> getCalendar(
            Long userId,
            YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        return workoutRead.findCalendar(userId, from, to);
    }

    @Transactional
    public WorkoutResult addExercise(
            Long userId,
            Long workoutId,
            String exerciseType,
            Long exerciseId,
            String memo) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        ExerciseSnapshot exercise = exerciseCatalog.getAvailable(
                userId, ExerciseType.valueOf(exerciseType), exerciseId);
        return WorkoutResult.from(
                workoutService.addExercise(workout, exercise, memo));
    }

    @Transactional
    public WorkoutResult removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResult.from(
                workoutService.removeExercise(workout, workoutExerciseId));
    }

    @Transactional
    public WorkoutResult addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResult.from(
                workoutService.addSet(
                        workout,
                        workoutExerciseId,
                        weightKg,
                        reps,
                        durationSeconds,
                        completed));
    }

    @Transactional
    public WorkoutResult addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResult.from(
                workoutService.addSets(
                        workout,
                        workoutExerciseId,
                        commands));
    }

    @Transactional
    public WorkoutResult updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResult.from(
                workoutService.updateSet(
                        workout,
                        workoutExerciseId,
                        setId,
                        weightKg,
                        reps,
                        durationSeconds,
                        completed));
    }

    @Transactional
    public WorkoutResult removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResult.from(
                workoutService.removeSet(
                        workout, workoutExerciseId, setId));
    }

    @Transactional
    public WorkoutResult completeWorkout(Long userId, Long workoutId) {
        return WorkoutResult.from(
                workoutService.complete(
                        workoutService.getOwned(userId, workoutId)));
    }

    @Transactional
    public WorkoutResult reopenWorkout(Long userId, Long workoutId) {
        return WorkoutResult.from(
                workoutService.reopen(
                        workoutService.getOwned(userId, workoutId)));
    }

}
