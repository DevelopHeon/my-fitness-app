package com.myfitness.body.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

public record BodyRecordView(
        Long id,
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        BigDecimal skeletalMuscleKg,
        Instant measuredAt,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {}
