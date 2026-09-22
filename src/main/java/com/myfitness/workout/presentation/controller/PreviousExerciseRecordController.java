package com.myfitness.workout.presentation.controller;

import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.application.service.WorkoutApplicationService;
import com.myfitness.workout.domain.model.WorkoutExercise;
import com.myfitness.workout.presentation.dto.response.PreviousExerciseRecordResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class PreviousExerciseRecordController {
    private final WorkoutApplicationService workoutApplicationService;

    public PreviousExerciseRecordController(
            WorkoutApplicationService workoutApplicationService) {
        this.workoutApplicationService = workoutApplicationService;
    }

    @GetMapping("/{exerciseType}/{exerciseId}/previous-record")
    public PreviousExerciseRecordResponse previousRecord(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable ExerciseType exerciseType,
            @PathVariable Long exerciseId) {
        WorkoutExercise previous =
                workoutApplicationService.getPreviousExerciseRecord(
                        userId, exerciseType, exerciseId);
        return previous == null
                ? null
                : PreviousExerciseRecordResponse.from(previous);
    }
}
