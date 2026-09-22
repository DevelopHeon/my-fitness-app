package com.myfitness.body.infrastructure.persistence;

import com.myfitness.body.domain.model.BodyRecord;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBodyRecordRepository extends JpaRepository<BodyRecord, Long> {
    List<BodyRecord> findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            Long userId, Instant from, Instant to);
    List<BodyRecord> findAllByUserIdOrderByMeasuredAtDesc(Long userId);
}
