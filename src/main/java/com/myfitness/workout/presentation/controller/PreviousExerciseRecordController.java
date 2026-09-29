package com.myfitness.workout.presentation.controller;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.dto.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.port.in.WorkoutUseCase;
import com.myfitness.workout.presentation.dto.response.PreviousExerciseRecordResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class PreviousExerciseRecordController {
    private final WorkoutUseCase workoutUseCase;

    public PreviousExerciseRecordController(
            WorkoutUseCase workoutUseCase) {
        this.workoutUseCase = workoutUseCase;
    }

    @GetMapping("/{exerciseType}/{exerciseId}/previous-record")
    public PreviousExerciseRecordResponse previousRecord(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable ExerciseType exerciseType,
            @PathVariable Long exerciseId) {
        PreviousExerciseRecordResult previous =
                workoutUseCase.getPreviousExerciseRecord(
                        userId, exerciseType, exerciseId);
        return previous == null
                ? null
                : PreviousExerciseRecordResponse.from(previous);
    }
}
