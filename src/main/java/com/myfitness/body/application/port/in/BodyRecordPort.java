package com.myfitness.body.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyRecordPort {
    BodyRecordView create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo);

    List<BodyRecordView> list(Long userId, Instant from, Instant to);

    BodyRecordView get(Long userId, Long bodyRecordId);

    BodyRecordView update(
            Long userId,
            Long bodyRecordId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo);

    void delete(Long userId, Long bodyRecordId);

    BodyTrendView trend(Long userId, int days);
}
