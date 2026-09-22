package com.myfitness.exercise.presentation.controller;

import com.myfitness.exercise.application.service.ExerciseService;
import com.myfitness.exercise.presentation.dto.request.CreateExerciseRequest;
import com.myfitness.exercise.presentation.dto.response.ExerciseResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> list(
            @RequestHeader("X-User-Id") Long userId) {
        return exerciseService.list(userId).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse createCustom(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateExerciseRequest request) {
        return ExerciseResponse.from(exerciseService.createCustom(
                userId, request.name(), request.category()));
    }
}
