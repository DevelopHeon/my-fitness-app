package com.myfitness.body.application.dto.response;

import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResult(
        BodyRecordResult latest,
        Change changeFromPrevious,
        List<BodyRecordResult> records
) {
    public static BodyTrendResult from(List<BodyRecord> records) {
        BodyRecord latest = records.isEmpty() ? null : records.getFirst();
        Change change = records.size() < 2 ? null : Change.between(latest, records.get(1));
        return new BodyTrendResult(
                BodyRecordResult.from(latest),
                change,
                records.stream().map(BodyRecordResult::from).toList());
    }

    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        static Change between(BodyRecord current, BodyRecord previous) {
            return new Change(
                    current.getWeightKg().subtract(previous.getWeightKg()),
                    current.getBodyFatPercentage().subtract(previous.getBodyFatPercentage()),
                    current.getSkeletalMuscleKg().subtract(previous.getSkeletalMuscleKg()));
        }
    }
}
