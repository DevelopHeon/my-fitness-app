package com.myfitness.body.domain.repository;

import com.myfitness.body.domain.model.BodyRecord;
import java.time.Instant;
import java.util.Optional;

public interface BodyRecordRepository {
    BodyRecord save(BodyRecord record);

    Optional<BodyRecord> findById(Long id);

    boolean existsByUserIdAndMeasuredAt(
            Long userId,
            Instant measuredAt);

    boolean existsByUserIdAndMeasuredAtAndIdNot(
            Long userId,
            Instant measuredAt,
            Long excludedId);

    void delete(BodyRecord record);
}
