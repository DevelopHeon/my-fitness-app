package com.myfitness.workout.presentation.controller;

import com.myfitness.workout.application.port.in.WorkoutPort.ExerciseType;
import com.myfitness.workout.application.port.in.routine.PreviousExerciseRecordView;
import com.myfitness.workout.application.port.in.WorkoutPort;
import com.myfitness.workout.presentation.dto.response.PreviousExerciseRecordResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class PreviousExerciseRecordController {
    private final WorkoutPort workoutPort;

    public PreviousExerciseRecordController(
            WorkoutPort workoutPort) {
        this.workoutPort = workoutPort;
    }

    @GetMapping("/{exerciseType}/{exerciseId}/previous-record")
    public PreviousExerciseRecordResponse previousRecord(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable ExerciseType exerciseType,
            @PathVariable Long exerciseId) {
        PreviousExerciseRecordView previous =
                workoutPort.getPreviousExerciseRecord(
                        userId, exerciseType, exerciseId);
        return previous == null
                ? null
                : PreviousExerciseRecordResponse.from(previous);
    }
}
