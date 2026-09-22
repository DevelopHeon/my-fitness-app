package com.myfitness.body.application.port.in.insight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyInsightQuery {
    List<BodyInsight> findAll(Long userId);

    record BodyInsight(
            Long id,
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
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
