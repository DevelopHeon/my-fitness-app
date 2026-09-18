package com.myfitness.routine.controller;

import com.myfitness.routine.dto.request.*;
import com.myfitness.routine.dto.response.*;
import com.myfitness.routine.service.RoutineApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {
    private final RoutineApplicationService routineApplicationService;

    public RoutineController(RoutineApplicationService routineApplicationService) {
        this.routineApplicationService = routineApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return routineApplicationService.create(userId, request);
    }

    @GetMapping
    public List<RoutineResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return routineApplicationService.list(userId);
    }

    @GetMapping("/{routineId}")
    public RoutineResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId) {
        return routineApplicationService.get(userId, routineId);
    }

    @PutMapping("/{routineId}")
    public RoutineResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineUpsertRequest request) {
        return routineApplicationService.update(userId, routineId, request);
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
        return routineApplicationService.startWorkout(userId, routineId, actual);
    }
}
