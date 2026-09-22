package com.myfitness.body.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record BodyRecordUpsertRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 3, fraction = 2)
        BigDecimal weightKg,

        @NotNull
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal bodyFatPercentage,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 3, fraction = 2)
        BigDecimal skeletalMuscleKg,

        @NotNull Instant measuredAt,

        @Size(max = 500) String memo
) {
}
