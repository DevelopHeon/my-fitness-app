package com.myfitness.body.repository;

import com.myfitness.body.domain.BodyRecord;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyRecordRepository extends JpaRepository<BodyRecord, Long> {
    List<BodyRecord> findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            Long userId,
            Instant from,
            Instant to);
}
