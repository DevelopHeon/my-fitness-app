package com.myfitness.exercise.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.exercise.application.port.in.ExerciseManagementUseCase;
import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.presentation.dto.request.CreateExerciseRequest;
import com.myfitness.exercise.presentation.dto.response.ExerciseResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseManagementUseCase exerciseManagementUseCase;
    private final ExerciseCatalogQuery exerciseCatalogQuery;

    public ExerciseController(
            ExerciseManagementUseCase exerciseManagementUseCase,
            ExerciseCatalogQuery exerciseCatalogQuery) {
        this.exerciseManagementUseCase = exerciseManagementUseCase;
        this.exerciseCatalogQuery = exerciseCatalogQuery;
    }

    @GetMapping
    public List<ExerciseResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return exerciseCatalogQuery.list(userId).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse createCustom(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody CreateExerciseRequest request) {
        return ExerciseResponse.from(exerciseManagementUseCase.createCustom(
                userId, request.name(), request.category()));
    }
}
