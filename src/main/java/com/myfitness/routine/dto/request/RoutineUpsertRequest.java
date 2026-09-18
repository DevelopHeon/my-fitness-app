package com.myfitness.routine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoutineUpsertRequest(
        @NotBlank String name,
        @NotEmpty List<@NotNull Long> exerciseIds
) {
}
