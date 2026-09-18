package com.myfitness.workout.controller;

import com.myfitness.workout.dto.request.AddWorkoutExerciseRequest;
import com.myfitness.workout.dto.request.StartWorkoutRequest;
import com.myfitness.workout.dto.request.WorkoutSetRequest;
import com.myfitness.workout.dto.response.WorkoutResponse;
import com.myfitness.workout.service.WorkoutApplicationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutApplicationService workoutApplicationService;

    public WorkoutController(WorkoutApplicationService workoutApplicationService) {
        this.workoutApplicationService = workoutApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse start(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StartWorkoutRequest request) {
        return workoutApplicationService.startWorkout(userId, request);
    }

    @GetMapping("/{workoutId}")
    public WorkoutResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId) {
        return workoutApplicationService.getWorkout(userId, workoutId);
    }

    @GetMapping
    public List<WorkoutResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return workoutApplicationService.getWorkouts(userId, from, to);
    }

    @PostMapping("/{workoutId}/exercises")
    public WorkoutResponse addExercise(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {
        return workoutApplicationService.addExercise(userId, workoutId, request);
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}")
    public WorkoutResponse removeExercise(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId) {
        return workoutApplicationService.removeExercise(userId, workoutId, workoutExerciseId);
    }

    @PostMapping("/{workoutId}/exercises/{workoutExerciseId}/sets")
    public WorkoutResponse addSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return workoutApplicationService.addSet(userId, workoutId, workoutExerciseId, request);
    }

    @PatchMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    public WorkoutResponse updateSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return workoutApplicationService.updateSet(
                userId, workoutId, workoutExerciseId, setId, request);
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    public WorkoutResponse removeSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId) {
        return workoutApplicationService.removeSet(
                userId, workoutId, workoutExerciseId, setId);
    }

    @PatchMapping("/{workoutId}/complete")
    public WorkoutResponse complete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId) {
        return workoutApplicationService.completeWorkout(userId, workoutId);
    }
}
