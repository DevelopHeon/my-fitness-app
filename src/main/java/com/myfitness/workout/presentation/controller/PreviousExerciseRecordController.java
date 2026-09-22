package com.myfitness.workout.presentation.controller;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.port.in.WorkoutUseCase;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.presentation.dto.response.PreviousExerciseRecordResponse;
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
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable ExerciseType exerciseType,
            @PathVariable Long exerciseId) {
        WorkoutExercise previous =
                workoutUseCase.getPreviousExerciseRecord(
                        userId, exerciseType, exerciseId);
        return previous == null
                ? null
                : PreviousExerciseRecordResponse.from(previous);
    }
}
