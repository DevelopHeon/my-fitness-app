package com.myfitness.body.infrastructure.persistence;

import com.myfitness.body.domain.model.BodyRecord;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBodyRecordRepository extends JpaRepository<BodyRecord, Long> {
    List<BodyRecord>
            findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtDescIdDesc(
                    Long userId,
                    Instant from,
                    Instant to);

    List<BodyRecord> findAllByUserIdOrderByMeasuredAtDescIdDesc(
            Long userId);

    List<BodyRecord> findAllByUserIdOrderByMeasuredAtDescIdDesc(
            Long userId,
            Pageable pageable);

    boolean existsByUserIdAndMeasuredAt(
            Long userId,
            Instant measuredAt);

    boolean existsByUserIdAndMeasuredAtAndIdNot(
            Long userId,
            Instant measuredAt,
            Long excludedId);
}
