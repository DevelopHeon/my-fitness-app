package com.myfitness.workout.service;

import com.myfitness.workout.domain.Exercise;
import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.dto.request.AddWorkoutExerciseRequest;
import com.myfitness.workout.dto.request.CreateExerciseRequest;
import com.myfitness.workout.dto.request.StartWorkoutRequest;
import com.myfitness.workout.dto.request.WorkoutSetRequest;
import com.myfitness.workout.dto.response.ExerciseResponse;
import com.myfitness.workout.dto.response.PreviousExerciseRecordResponse;
import com.myfitness.workout.dto.response.WorkoutResponse;
import java.time.LocalDate;
import java.util.List;
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
    public ExerciseResponse createExercise(Long userId, CreateExerciseRequest request) {
        return ExerciseResponse.from(
                exerciseService.create(userId, request.name(), request.category()));
    }

    public List<ExerciseResponse> getExercises(Long userId) {
        return exerciseService.list(userId).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    public PreviousExerciseRecordResponse getPreviousExerciseRecord(Long userId, Long exerciseId) {
        exerciseService.getOwned(userId, exerciseId);
        WorkoutExercise previous = workoutService.getPreviousCompletedExercise(userId, exerciseId);
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

    public List<WorkoutResponse> getWorkouts(Long userId, LocalDate from, LocalDate to) {
        return workoutService.list(userId, from, to).stream()
                .map(WorkoutResponse::from)
                .toList();
    }

    @Transactional
    public WorkoutResponse addExercise(
            Long userId,
            Long workoutId,
            AddWorkoutExerciseRequest request) {
        Workout workout = workoutService.getOwned(userId, workoutId);
        Exercise exercise = exerciseService.getOwned(userId, request.exerciseId());
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
