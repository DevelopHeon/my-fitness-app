package com.myfitness.body.application.port.in.insight;

import com.myfitness.body.application.dto.response.BodyRecordResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyInsightQuery {
    List<BodyInsight> findAll(Long userId);

    List<BodyInsight> findRecent(Long userId, int limit);

    record BodyInsight(
            Long id,
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        public static BodyInsight from(BodyRecordResult record) {
            return new BodyInsight(
                    record.id(),
                    record.measuredAt(),
                    record.weightKg(),
                    record.bodyFatPercentage(),
                    record.skeletalMuscleKg());
        }

        public Long getId() {
            return id;
        }

        public Instant getMeasuredAt() {
            return measuredAt;
        }

        public BigDecimal getWeightKg() {
            return weightKg;
        }

        public BigDecimal getBodyFatPercentage() {
            return bodyFatPercentage;
        }

        public BigDecimal getSkeletalMuscleKg() {
            return skeletalMuscleKg;
        }
    }
}
