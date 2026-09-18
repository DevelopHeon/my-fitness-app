package com.myfitness.workout.controller;

import com.myfitness.workout.domain.ExerciseType;
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

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse createCustom(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateExerciseRequest request) {
        return workoutApplicationService.createCustomExercise(userId, request);
    }

    @GetMapping("/{exerciseType}/{exerciseId}/previous-record")
    public PreviousExerciseRecordResponse previousRecord(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable ExerciseType exerciseType,
            @PathVariable Long exerciseId) {
        return workoutApplicationService.getPreviousExerciseRecord(
                userId, exerciseType, exerciseId);
    }
}
