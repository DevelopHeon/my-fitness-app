package com.myfitness.body.application.result;

import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResult(
        BodyRecord latest,
        Change changeFromPrevious,
        List<BodyRecord> records
) {
    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}
}
