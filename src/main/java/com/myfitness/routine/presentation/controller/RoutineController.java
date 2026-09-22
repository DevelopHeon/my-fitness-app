package com.myfitness.routine.presentation.controller;

import com.myfitness.routine.application.command.ExerciseSelection;
import com.myfitness.routine.application.result.RoutineWorkoutStartResult;
import com.myfitness.routine.application.service.RoutineApplicationService;
import com.myfitness.routine.presentation.dto.request.RoutineUpsertRequest;
import com.myfitness.routine.presentation.dto.request.StartRoutineWorkoutRequest;
import com.myfitness.routine.presentation.dto.response.RoutineResponse;
import com.myfitness.routine.presentation.dto.response.RoutineWorkoutStartResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {
    private final RoutineApplicationService routineApplicationService;

    public RoutineController(
            RoutineApplicationService routineApplicationService) {
        this.routineApplicationService = routineApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routineApplicationService.create(
                userId,
                request.name(),
                toSelections(request)));
    }

    @GetMapping
    public List<RoutineResponse> list(
            @RequestHeader("X-User-Id") Long userId) {
        return routineApplicationService.list(userId).stream()
                .map(RoutineResponse::from)
                .toList();
    }

    @GetMapping("/{routineId}")
    public RoutineResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId) {
        return RoutineResponse.from(
                routineApplicationService.get(userId, routineId));
    }

    @PutMapping("/{routineId}")
    public RoutineResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routineApplicationService.update(
                userId,
                routineId,
                request.name(),
                toSelections(request)));
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId) {
        routineApplicationService.delete(userId, routineId);
    }

    @PostMapping("/{routineId}/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineWorkoutStartResponse startWorkout(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId,
            @RequestBody(required = false) StartRoutineWorkoutRequest request) {
        StartRoutineWorkoutRequest actual = request == null
                ? new StartRoutineWorkoutRequest(null, null)
                : request;
        RoutineWorkoutStartResult result =
                routineApplicationService.startWorkout(
                        userId,
                        routineId,
                        actual.workoutDate(),
                        actual.memo());
        return RoutineWorkoutStartResponse.from(result);
    }

    private static List<ExerciseSelection> toSelections(
            RoutineUpsertRequest request) {
        return request.exercises().stream()
                .map(exercise -> new ExerciseSelection(
                        exercise.exerciseType(),
                        exercise.exerciseId()))
                .toList();
    }
}
