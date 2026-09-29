package com.myfitness.routine.presentation.controller;

import com.myfitness.routine.application.dto.request.ExerciseSelection;
import com.myfitness.routine.application.dto.response.RoutineWorkoutStartResult;
import com.myfitness.routine.application.port.in.RoutineUseCase;
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
    private final RoutineUseCase routineUseCase;

    public RoutineController(
            RoutineUseCase routineUseCase) {
        this.routineUseCase = routineUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse create(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routineUseCase.create(
                userId,
                request.name(),
                toSelections(request)));
    }

    @GetMapping
    public List<RoutineResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return routineUseCase.list(userId).stream()
                .map(RoutineResponse::from)
                .toList();
    }

    @GetMapping("/{routineId}")
    public RoutineResponse get(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId) {
        return RoutineResponse.from(
                routineUseCase.get(userId, routineId));
    }

    @PutMapping("/{routineId}")
    public RoutineResponse update(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return RoutineResponse.from(routineUseCase.update(
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
        routineUseCase.delete(userId, routineId);
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
        RoutineWorkoutStartResult result =
                routineUseCase.startWorkout(
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
