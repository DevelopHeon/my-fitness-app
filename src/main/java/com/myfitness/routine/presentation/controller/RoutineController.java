package com.myfitness.routine.presentation.controller;

import com.myfitness.routine.application.port.in.RoutinePort.SelectedExercise;
import com.myfitness.routine.application.port.in.RoutineWorkoutStartView;
import com.myfitness.routine.application.port.in.RoutinePort;
import com.myfitness.routine.presentation.dto.request.RoutineUpsertRequest;
import com.myfitness.routine.presentation.dto.request.StartRoutineWorkoutRequest;
import com.myfitness.routine.presentation.dto.response.RoutineResponse;
import com.myfitness.routine.presentation.dto.response.RoutineWorkoutStartResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {
    private final RoutinePort routinePort;

    public RoutineController(
            RoutinePort routinePort) {
        this.routinePort = routinePort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse create(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routinePort.create(
                userId,
                request.name(),
                toSelections(request)));
    }

    @GetMapping
    public List<RoutineResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return routinePort.list(userId).stream()
                .map(RoutineResponse::from)
                .toList();
    }

    @GetMapping("/{routineId}")
    public RoutineResponse get(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId) {
        return RoutineResponse.from(
                routinePort.get(userId, routineId));
    }

    @PutMapping("/{routineId}")
    public RoutineResponse update(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routinePort.update(
                userId,
                routineId,
                request.name(),
                toSelections(request)));
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId) {
        routinePort.delete(userId, routineId);
    }

    @PostMapping("/{routineId}/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineWorkoutStartResponse startWorkout(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId,
            @RequestBody(required = false) StartRoutineWorkoutRequest request) {
        StartRoutineWorkoutRequest actual = request == null
                ? new StartRoutineWorkoutRequest(null, null)
                : request;
        RoutineWorkoutStartView result =
                routinePort.startWorkout(
                        userId,
                        routineId,
                        actual.workoutDate(),
                        actual.memo());
        return RoutineWorkoutStartResponse.from(result);
    }

    private static List<SelectedExercise> toSelections(
            RoutineUpsertRequest request) {
        return request.exercises().stream()
                .map(exercise -> new SelectedExercise(
                        exercise.exerciseType(),
                        exercise.exerciseId()))
                .toList();
    }
}
