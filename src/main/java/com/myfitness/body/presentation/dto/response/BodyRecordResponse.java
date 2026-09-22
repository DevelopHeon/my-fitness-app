package com.myfitness.body.presentation.dto.response;

import com.myfitness.body.application.result.BodyRecordResult;
import java.math.BigDecimal;
import java.time.Instant;

public record BodyRecordResponse(
        Long id,
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        BigDecimal skeletalMuscleKg,
        Instant measuredAt,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {
    public static BodyRecordResponse from(BodyRecordResult record) {
        return new BodyRecordResponse(
                record.id(),
                record.weightKg(),
                record.bodyFatPercentage(),
                record.skeletalMuscleKg(),
                record.measuredAt(),
                record.memo(),
                record.createdAt(),
                record.updatedAt());
    }
}
