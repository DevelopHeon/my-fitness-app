package com.myfitness.workout.application.service;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.command.WorkoutSetCommand;
import com.myfitness.workout.application.port.in.WorkoutUseCase;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.ExerciseView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.PreviousRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.RoutineWorkoutView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.SetView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.WorkoutView;
import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.workout.application.result.WorkoutCalendarDayResult;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutApplicationService implements WorkoutUseCase, WorkoutRoutineUseCase {
    private final WorkoutService workoutService;
    private final ExerciseCatalogQuery exerciseCatalogQuery;

    public WorkoutApplicationService(
            WorkoutService workoutService,
            ExerciseCatalogQuery exerciseCatalogQuery) {
        this.workoutService = workoutService;
        this.exerciseCatalogQuery = exerciseCatalogQuery;
    }

    public WorkoutExercise getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        exerciseCatalogQuery.getAvailable(userId, exerciseType, exerciseId);
        return workoutService.getPreviousCompletedExercise(
                userId, exerciseType, exerciseId);
    }

    @Transactional
    public Workout startWorkout(
            Long userId,
            LocalDate workoutDate,
            String memo) {
        return workoutService.start(userId, workoutDate, memo);
    }

    public Workout getWorkout(Long userId, Long workoutId) {
        return workoutService.getOwned(userId, workoutId);
    }

    public List<Workout> getWorkouts(
            Long userId,
            LocalDate from,
            LocalDate to) {
        return workoutService.list(userId, from, to);
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
                .map(entry -> {
                    LinkedHashSet<String> exerciseNames =
                            new LinkedHashSet<>();
                    int completedCount = 0;
                    for (Workout workout : entry.getValue()) {
                        if (workout.getStatus() == WorkoutStatus.COMPLETED) {
                            completedCount++;
                        }
                        workout.getExercises().stream()
                                .map(WorkoutExercise::getExerciseName)
                                .forEach(exerciseNames::add);
                    }
                    return new WorkoutCalendarDayResult(
                            entry.getKey(),
                            entry.getValue().size(),
                            completedCount,
                            List.copyOf(exerciseNames));
                })
                .toList();
    }

    @Transactional
    public Workout addExercise(
            Long userId,
            Long workoutId,
            ExerciseType exerciseType,
            Long exerciseId,
            String memo) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        ExerciseReference exercise = exerciseCatalogQuery.getAvailable(
                userId, exerciseType, exerciseId);
        return workoutService.addExercise(workout, exercise, memo);
    }

    @Transactional
    public Workout removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return workoutService.removeExercise(workout, workoutExerciseId);
    }

    @Transactional
    public Workout addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return workoutService.addSet(
                workout,
                workoutExerciseId,
                weightKg,
                reps,
                durationSeconds,
                completed);
    }

    @Transactional
    public Workout addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return workoutService.addSets(
                workout,
                workoutExerciseId,
                commands);
    }

    @Transactional
    public Workout updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return workoutService.updateSet(
                workout,
                workoutExerciseId,
                setId,
                weightKg,
                reps,
                durationSeconds,
                completed);
    }

    @Transactional
    public Workout removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return workoutService.removeSet(
                workout, workoutExerciseId, setId);
    }

    @Transactional
    public Workout completeWorkout(Long userId, Long workoutId) {
        return workoutService.complete(
                workoutService.getOwned(userId, workoutId));
    }

    @Transactional
    public Workout reopenWorkout(Long userId, Long workoutId) {
        return workoutService.reopen(
                workoutService.getOwned(userId, workoutId));
    }

    @Override
    @Transactional
    public RoutineWorkoutView startWorkout(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseReference> exercises) {
        Workout workout = workoutService.startWithExercises(
                userId, workoutDate, memo, exercises);

        List<PreviousRecordView> previousRecords =
                workout.getExercises().stream()
                        .map(entry -> workoutService.getPreviousCompletedExercise(
                                userId,
                                entry.getExerciseType(),
                                entry.getExerciseId()))
                        .filter(previous -> previous != null)
                        .map(WorkoutApplicationService::toPreviousRecordView)
                        .toList();

        return new RoutineWorkoutView(
                toWorkoutView(workout),
                previousRecords);
    }

    private static WorkoutView toWorkoutView(Workout workout) {
        return new WorkoutView(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getStatus().name(),
                workout.getMemo(),
                workout.getStartedAt(),
                workout.getCompletedAt(),
                workout.getExercises().stream()
                        .map(WorkoutApplicationService::toExerciseView)
                        .toList());
    }

    private static ExerciseView toExerciseView(WorkoutExercise entry) {
        return new ExerciseView(
                entry.getId(),
                entry.getExerciseType(),
                entry.getExerciseId(),
                entry.getExerciseName(),
                entry.getCategory().name(),
                entry.getOrderIndex(),
                entry.getMemo(),
                entry.getSets().stream()
                        .map(WorkoutApplicationService::toSetView)
                        .toList());
    }

    private static SetView toSetView(
            com.myfitness.workout.domain.model.WorkoutSet set) {
        return new SetView(
                set.getId(),
                set.getSetNumber(),
                set.getWeightKg(),
                set.getReps(),
                set.getDurationSeconds(),
                set.isCompleted());
    }

    private static PreviousRecordView toPreviousRecordView(
            WorkoutExercise entry) {
        return new PreviousRecordView(
                entry.getWorkout().getId(),
                entry.getWorkout().getWorkoutDate(),
                entry.getId(),
                entry.getExerciseType(),
                entry.getExerciseId(),
                entry.getExerciseName(),
                entry.getSets().stream()
                        .map(WorkoutApplicationService::toSetView)
                        .toList());
    }

}
