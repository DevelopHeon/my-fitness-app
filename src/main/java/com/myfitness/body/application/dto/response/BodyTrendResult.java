package com.myfitness.body.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResult(
        BodyRecordResult latest,
        Change changeFromPrevious,
        List<BodyRecordResult> records
) {
    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}
}
