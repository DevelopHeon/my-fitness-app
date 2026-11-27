package com.myfitness.body.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public record BodyTrendView(
        BodyRecordView latest,
        Change changeFromPrevious,
        List<BodyRecordView> records
) {
    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}
}
