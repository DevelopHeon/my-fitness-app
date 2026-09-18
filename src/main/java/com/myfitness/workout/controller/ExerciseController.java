package com.myfitness.workout.controller;

import com.myfitness.workout.dto.request.CreateExerciseRequest;
import com.myfitness.workout.dto.response.ExerciseResponse;
import com.myfitness.workout.dto.response.PreviousExerciseRecordResponse;
import com.myfitness.workout.service.WorkoutApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final WorkoutApplicationService workoutApplicationService;

    public ExerciseController(WorkoutApplicationService workoutApplicationService) {
        this.workoutApplicationService = workoutApplicationService;
    }

    @GetMapping
    public List<ExerciseResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return workoutApplicationService.getExercises(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateExerciseRequest request) {
        return workoutApplicationService.createExercise(userId, request);
    }

    @GetMapping("/{exerciseId}/previous-record")
    public PreviousExerciseRecordResponse previousRecord(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long exerciseId) {
        return workoutApplicationService.getPreviousExerciseRecord(userId, exerciseId);
    }
}
