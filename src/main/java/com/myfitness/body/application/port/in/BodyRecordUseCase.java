package com.myfitness.body.application.port.in;

import com.myfitness.body.application.result.BodyTrendResult;
import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyRecordUseCase {
    BodyRecord create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo);

    List<BodyRecord> list(Long userId, Instant from, Instant to);

    BodyRecord get(Long userId, Long bodyRecordId);

    BodyRecord update(
            Long userId,
            Long bodyRecordId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo);

    void delete(Long userId, Long bodyRecordId);

    BodyTrendResult trend(Long userId, int days);
}
