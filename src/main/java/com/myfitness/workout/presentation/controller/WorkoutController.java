package com.myfitness.workout.presentation.controller;

import com.myfitness.workout.application.port.in.WorkoutPort.WorkoutSetInput;
import com.myfitness.workout.application.port.in.WorkoutPort;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {
    private final WorkoutPort workoutPort;

    public WorkoutController(
            WorkoutPort workoutPort) {
        this.workoutPort = workoutPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse start(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody StartWorkoutRequest request) {
        return WorkoutResponse.from(
                workoutPort.startWorkout(
                        userId,
                        request.workoutDate(),
                        request.memo()));
    }

    @GetMapping("/{workoutId}")
    public WorkoutResponse get(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutPort.getWorkout(userId, workoutId));
    }

    @GetMapping
    public List<WorkoutResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return workoutPort
                .getWorkouts(userId, from, to)
                .stream()
                .map(WorkoutResponse::from)
                .toList();
    }

    @GetMapping("/calendar")
    public List<WorkoutCalendarDayResponse> calendar(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam String month) {
        return workoutPort
                .getCalendar(userId, YearMonth.parse(month))
                .stream()
                .map(WorkoutCalendarDayResponse::from)
                .toList();
    }

    @PostMapping("/{workoutId}/exercises")
    public WorkoutResponse addExercise(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {
        return WorkoutResponse.from(
                workoutPort.addExercise(
                        userId,
                        workoutId,
                        request.exerciseType(),
                        request.exerciseId(),
                        request.memo()));
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}")
    public WorkoutResponse removeExercise(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId) {
        return WorkoutResponse.from(
                workoutPort.removeExercise(
                        userId, workoutId, workoutExerciseId));
    }

    @PostMapping("/{workoutId}/exercises/{workoutExerciseId}/sets")
    public WorkoutResponse addSet(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return WorkoutResponse.from(
                workoutPort.addSet(
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
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody WorkoutSetBatchRequest request) {
        List<WorkoutSetInput> commands = request.sets().stream()
                .map(set -> new WorkoutSetInput(
                        set.weightKg(),
                        set.reps(),
                        set.durationSeconds(),
                        set.completed()))
                .toList();
        return WorkoutResponse.from(
                workoutPort.addSets(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        commands));
    }

    @PatchMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    public WorkoutResponse updateSet(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId,
            @Valid @RequestBody WorkoutSetRequest request) {
        return WorkoutResponse.from(
                workoutPort.updateSet(
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
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long setId) {
        return WorkoutResponse.from(
                workoutPort.removeSet(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        setId));
    }

    @PatchMapping("/{workoutId}/complete")
    public WorkoutResponse complete(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutPort.completeWorkout(
                        userId, workoutId));
    }

    @PatchMapping("/{workoutId}/reopen")
    public WorkoutResponse reopen(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long workoutId) {
        return WorkoutResponse.from(
                workoutPort.reopenWorkout(
                        userId, workoutId));
    }
}
