package com.myfitness.body.application.dto.response;

import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.time.Instant;

public record BodyRecordResult(
        Long id,
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        BigDecimal skeletalMuscleKg,
        Instant measuredAt,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {
    public static BodyRecordResult from(BodyRecord record) {
        if (record == null) {
            return null;
        }
        return new BodyRecordResult(
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
