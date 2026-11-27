package com.myfitness.ai.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BodyContextPort {
    List<BodyInsight> findRecent(Long userId, int limit);

    record BodyInsight(
            Long id,
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}
}
