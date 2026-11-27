package com.myfitness.body.application.usecase;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import com.myfitness.body.application.usecase.response.BodyTrendResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyRecordUseCase {
    BodyRecordResult create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo);

    List<BodyRecordResult> list(Long userId, Instant from, Instant to);

    BodyRecordResult get(Long userId, Long bodyRecordId);

    BodyRecordResult update(
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
