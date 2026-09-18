package com.myfitness.workout.service;

import com.myfitness.workout.domain.*;
import com.myfitness.workout.dto.request.*;
import com.myfitness.workout.dto.response.*;
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
public class WorkoutApplicationService {
    private final WorkoutService workoutService;
    private final ExerciseService exerciseService;

    public WorkoutApplicationService(
            WorkoutService workoutService,
            ExerciseService exerciseService) {
        this.workoutService = workoutService;
        this.exerciseService = exerciseService;
    }

    @Transactional
    public ExerciseResponse createCustomExercise(
            Long userId,
            CreateExerciseRequest request) {
        return ExerciseResponse.from(exerciseService.createCustom(
                userId, request.name(), request.category()));
    }

    public List<ExerciseResponse> getExercises(Long userId) {
        return exerciseService.list(userId).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    public PreviousExerciseRecordResponse getPreviousExerciseRecord(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        exerciseService.getAvailable(userId, exerciseType, exerciseId);
        WorkoutExercise previous = workoutService.getPreviousCompletedExercise(
                userId, exerciseType, exerciseId);
        return previous == null ? null : PreviousExerciseRecordResponse.from(previous);
    }

    @Transactional
    public WorkoutResponse startWorkout(Long userId, StartWorkoutRequest request) {
        return WorkoutResponse.from(
                workoutService.start(userId, request.workoutDate(), request.memo()));
    }

    public WorkoutResponse getWorkout(Long userId, Long workoutId) {
        return WorkoutResponse.from(workoutService.getOwned(userId, workoutId));
    }

    public List<WorkoutResponse> getWorkouts(
            Long userId,
            LocalDate from,
            LocalDate to) {
        return workoutService.list(userId, from, to).stream()
                .map(WorkoutResponse::from)
                .toList();
    }

    public List<WorkoutCalendarDayResponse> getCalendar(
            Long userId,
            YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<LocalDate, List<Workout>> byDate = workoutService.list(userId, from, to)
                .stream()
                .collect(Collectors.groupingBy(
                        Workout::getWorkoutDate,
                        TreeMap::new,
                        Collectors.toList()));

        return byDate.entrySet().stream()
                .map(entry -> {
                    LinkedHashSet<String> exerciseNames = new LinkedHashSet<>();
                    int completedCount = 0;
                    for (Workout workout : entry.getValue()) {
                        if (workout.getStatus() == WorkoutStatus.COMPLETED) {
                            completedCount++;
                        }
                        workout.getExercises().stream()
                                .map(WorkoutExercise::getExerciseName)
                                .forEach(exerciseNames::add);
                    }
                    return new WorkoutCalendarDayResponse(
                            entry.getKey(),
                            entry.getValue().size(),
                            completedCount,
                            List.copyOf(exerciseNames));
                })
                .toList();
    }

    @Transactional
    public WorkoutResponse addExercise(
            Long userId,
            Long workoutId,
            AddWorkoutExerciseRequest request) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        ExerciseReference exercise = exerciseService.getAvailable(
                userId, request.exerciseType(), request.exerciseId());
        return WorkoutResponse.from(
                workoutService.addExercise(workout, exercise, request.memo()));
    }

    @Transactional
    public WorkoutResponse removeExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(
                workoutService.removeExercise(workout, workoutExerciseId));
    }

    @Transactional
    public WorkoutResponse addSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            WorkoutSetRequest request) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(workoutService.addSet(
                workout,
                workoutExerciseId,
                request.weightKg(),
                request.reps(),
                request.durationSeconds(),
                request.completed()));
    }

    @Transactional
    public WorkoutResponse addSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            WorkoutSetBatchRequest request) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(workoutService.addSets(
                workout,
                workoutExerciseId,
                request.sets()));
    }

    @Transactional
    public WorkoutResponse updateSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId,
            WorkoutSetRequest request) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(workoutService.updateSet(
                workout,
                workoutExerciseId,
                setId,
                request.weightKg(),
                request.reps(),
                request.durationSeconds(),
                request.completed()));
    }

    @Transactional
    public WorkoutResponse removeSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long setId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(
                workoutService.removeSet(workout, workoutExerciseId, setId));
    }

    @Transactional
    public WorkoutResponse completeWorkout(Long userId, Long workoutId) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        return WorkoutResponse.from(workoutService.complete(workout));
    }
}
