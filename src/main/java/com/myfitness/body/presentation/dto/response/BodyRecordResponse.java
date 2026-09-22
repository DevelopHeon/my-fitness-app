package com.myfitness.body.presentation.dto.response;

import com.myfitness.body.domain.model.BodyRecord;
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
    public static BodyRecordResponse from(BodyRecord record) {
        return new BodyRecordResponse(
                record.getId(),
                record.getWeightKg(),
                record.getBodyFatPercentage(),
                record.getSkeletalMuscleKg(),
                record.getMeasuredAt(),
                record.getMemo(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
