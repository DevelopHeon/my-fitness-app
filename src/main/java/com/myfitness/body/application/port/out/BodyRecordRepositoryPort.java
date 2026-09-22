package com.myfitness.body.application.port.out;

import com.myfitness.body.domain.model.BodyRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BodyRecordRepositoryPort {
    BodyRecord save(BodyRecord record);
    Optional<BodyRecord> findById(Long id);
    List<BodyRecord> findByUserIdAndMeasuredAtBetween(
            Long userId, Instant from, Instant to);
    List<BodyRecord> findAllByUserId(Long userId);
    void delete(BodyRecord record);
}
