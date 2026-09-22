package com.myfitness.routine.presentation.dto.request;

import com.myfitness.routine.presentation.dto.request.ExerciseReferenceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoutineUpsertRequest(
        @NotBlank String name,
        @NotEmpty List<@Valid ExerciseReferenceRequest> exercises
) {
}
