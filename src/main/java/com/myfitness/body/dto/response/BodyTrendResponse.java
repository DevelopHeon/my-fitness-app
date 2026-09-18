package com.myfitness.body.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResponse(
        BodyRecordResponse latest,
        Change changeFromPrevious,
        List<BodyRecordResponse> records
) {
    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
    }
}
