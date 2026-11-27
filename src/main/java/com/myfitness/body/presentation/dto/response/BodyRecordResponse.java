package com.myfitness.body.presentation.dto.response;

import com.myfitness.body.application.port.in.BodyRecordView;
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
    public static BodyRecordResponse from(BodyRecordView record) {
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
