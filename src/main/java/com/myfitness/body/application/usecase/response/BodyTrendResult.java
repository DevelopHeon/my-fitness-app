package com.myfitness.body.application.usecase.response;

import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResult(
        BodyRecordResult latest,
        Change changeFromPrevious,
        List<BodyRecordResult> records
) {
    public static BodyTrendResult from(
            List<BodyRecordResult> latestRecords,
            List<BodyRecordResult> records) {
        BodyRecordResult latest = latestRecords.isEmpty() ? null : latestRecords.getFirst();
        Change change = latestRecords.size() < 2
                ? null
                : Change.between(latest, latestRecords.get(1));
        return new BodyTrendResult(
                latest,
                change,
                records);
    }

    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        static Change between(BodyRecordResult current, BodyRecordResult previous) {
            return new Change(
                    current.weightKg().subtract(previous.weightKg()),
                    current.bodyFatPercentage().subtract(previous.bodyFatPercentage()),
                    current.skeletalMuscleKg().subtract(previous.skeletalMuscleKg()));
        }
    }
}
