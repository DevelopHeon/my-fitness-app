package com.myfitness.routine.dto.request;

import com.myfitness.workout.dto.request.ExerciseReferenceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoutineUpsertRequest(
        @NotBlank String name,
        @NotEmpty List<@Valid ExerciseReferenceRequest> exercises
) {
}
