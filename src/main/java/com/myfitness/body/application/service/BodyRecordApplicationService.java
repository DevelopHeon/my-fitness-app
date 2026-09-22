package com.myfitness.body.application.service;

import com.myfitness.body.application.result.BodyTrendResult;
import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyRecordApplicationService {
    private final BodyRecordService bodyRecordService;

    public BodyRecordApplicationService(
            BodyRecordService bodyRecordService) {
        this.bodyRecordService = bodyRecordService;
    }

    @Transactional
    public BodyRecord create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        return bodyRecordService.create(
                userId,
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo);
    }

    public List<BodyRecord> list(
            Long userId,
            Instant from,
            Instant to) {
        return bodyRecordService.list(userId, from, to);
    }

    public BodyRecord get(Long userId, Long bodyRecordId) {
        return bodyRecordService.getOwned(userId, bodyRecordId);
    }

    @Transactional
    public BodyRecord update(
            Long userId,
            Long bodyRecordId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        BodyRecord record =
                bodyRecordService.getOwned(userId, bodyRecordId);
        return bodyRecordService.update(
                record,
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo);
    }

    @Transactional
    public void delete(Long userId, Long bodyRecordId) {
        bodyRecordService.delete(
                bodyRecordService.getOwned(userId, bodyRecordId));
    }

    public BodyTrendResult trend(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 3650));
        List<BodyRecord> records =
                bodyRecordService.listRecent(userId, safeDays);

        BodyRecord latest =
                records.isEmpty() ? null : records.getFirst();
        BodyTrendResult.Change change = records.size() < 2
                ? null
                : new BodyTrendResult.Change(
                        subtract(
                                records.getFirst().getWeightKg(),
                                records.get(1).getWeightKg()),
                        subtract(
                                records.getFirst().getBodyFatPercentage(),
                                records.get(1).getBodyFatPercentage()),
                        subtract(
                                records.getFirst().getSkeletalMuscleKg(),
                                records.get(1).getSkeletalMuscleKg()));

        return new BodyTrendResult(latest, change, records);
    }

    private static BigDecimal subtract(
            BigDecimal current,
            BigDecimal previous) {
        return current.subtract(previous);
    }
}
