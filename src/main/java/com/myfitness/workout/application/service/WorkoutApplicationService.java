package com.myfitness.workout.application.service;

import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.dto.request.WorkoutSetCommand;
import com.myfitness.workout.application.dto.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.dto.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.dto.response.WorkoutResult;
import com.myfitness.workout.application.port.in.WorkoutUseCase;
import com.myfitness.workout.domain.model.Workout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutApplicationService implements WorkoutUseCase {
    private final WorkoutService workoutService;
    private final ExerciseCatalogQuery exerciseCatalogQuery;

    public WorkoutApplicationService(
            WorkoutService workoutService,
            ExerciseCatalogQuery exerciseCatalogQuery) {
        this.workoutService = workoutService;
        this.exerciseCatalogQuery = exerciseCatalogQuery;
    }

    public PreviousExerciseRecordResult getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        exerciseCatalogQuery.getAvailable(userId, exerciseType, exerciseId);
        return PreviousExerciseRecordResult.from(
                workoutService.getPreviousCompletedExercise(
                        userId, exerciseType, exerciseId));
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
        return workoutService.list(userId, from, to).stream()
                .map(WorkoutResult::from)
                .toList();
    }

    public List<WorkoutCalendarDayResult> getCalendar(
            Long userId,
            YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<LocalDate, List<Workout>> byDate =
                workoutService.list(userId, from, to).stream()
                        .collect(Collectors.groupingBy(
                                Workout::getWorkoutDate,
                                TreeMap::new,
                                Collectors.toList()));

        return byDate.entrySet().stream()
                .map(entry -> WorkoutCalendarDayResult.from(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public WorkoutResult addExercise(
            Long userId,
            Long workoutId,
            ExerciseType exerciseType,
            Long exerciseId,
            String memo) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        ExerciseReference exercise = exerciseCatalogQuery.getAvailable(
                userId, exerciseType, exerciseId);
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
