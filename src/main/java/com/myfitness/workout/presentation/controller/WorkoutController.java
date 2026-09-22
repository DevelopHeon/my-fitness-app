package com.myfitness.workout.presentation.controller;

import com.myfitness.workout.application.command.WorkoutSetCommand;
import com.myfitness.workout.application.port.in.WorkoutUseCase;
import com.myfitness.workout.presentation.dto.request.AddWorkoutExerciseRequest;
import com.myfitness.workout.presentation.dto.request.StartWorkoutRequest;
import com.myfitness.workout.presentation.dto.request.WorkoutSetBatchRequest;
import com.myfitness.workout.presentation.dto.request.WorkoutSetRequest;
import com.myfitness.workout.presentation.dto.response.WorkoutCalendarDayResponse;
import com.myfitness.workout.presentation.dto.response.WorkoutResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {
    private final WorkoutUseCase workoutUseCase;

    public WorkoutController(
            WorkoutUseCase workoutUseCase) {
        this.workoutUseCase = workoutUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse start(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StartWorkoutRequest request) {
        return WorkoutResponse.from(
                workoutUseCase.startWorkout(
                        userId,
                        request.workoutDate(),
                        request.memo()));
    }

    @GetMapping("/{workoutId}")
    public WorkoutResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutUseCase.getWorkout(userId, workoutId));
    }

    @GetMapping
    public List<WorkoutResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return workoutUseCase
                .getWorkouts(userId, from, to)
                .stream()
                .map(WorkoutResponse::from)
                .toList();
    }

    @GetMapping("/calendar")
    public List<WorkoutCalendarDayResponse> calendar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String month) {
        return workoutUseCase
                .getCalendar(userId, YearMonth.parse(month))
                .stream()
                .map(WorkoutCalendarDayResponse::from)
                .toList();
    }

    @PostMapping("/{workoutId}/exercises")
    public WorkoutResponse addExercise(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {
        return WorkoutResponse.from(
                workoutUseCase.addExercise(
                        userId,
                        workoutId,
                        request.exerciseType(),
                        request.exerciseId(),
                        request.memo()));
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}")
    public WorkoutResponse removeExercise(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId) {
        return WorkoutResponse.from(
                workoutUseCase.removeExercise(
                        userId, workoutId, workoutExerciseId));
    }

    @PostMapping("/{workoutId}/exercises/{workoutExerciseId}/sets")
    public WorkoutResponse addSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return WorkoutResponse.from(
                workoutUseCase.addSet(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        request.weightKg(),
                        request.reps(),
                        request.durationSeconds(),
                        request.completed()));
    }

    @PostMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/batch")
    public WorkoutResponse addSets(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody WorkoutSetBatchRequest request) {
        List<WorkoutSetCommand> commands = request.sets().stream()
                .map(set -> new WorkoutSetCommand(
                        set.weightKg(),
                        set.reps(),
                        set.durationSeconds(),
                        set.completed()))
                .toList();
        return WorkoutResponse.from(
                workoutUseCase.addSets(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        commands));
    }

    @PatchMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    public WorkoutResponse updateSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return WorkoutResponse.from(
                workoutUseCase.updateSet(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        setId,
                        request.weightKg(),
                        request.reps(),
                        request.durationSeconds(),
                        request.completed()));
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    public WorkoutResponse removeSet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId) {
        return WorkoutResponse.from(
                workoutUseCase.removeSet(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        setId));
    }

    @PatchMapping("/{workoutId}/complete")
    public WorkoutResponse complete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutUseCase.completeWorkout(
                        userId, workoutId));
    }

    @PatchMapping("/{workoutId}/reopen")
    public WorkoutResponse reopen(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutUseCase.reopenWorkout(
                        userId, workoutId));
    }
}
