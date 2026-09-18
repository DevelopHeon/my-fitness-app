package com.myfitness.workout.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkoutSetBatchRequest(
        @NotEmpty
        @Size(max = 20)
        List<@Valid WorkoutSetRequest> sets
) {
}
