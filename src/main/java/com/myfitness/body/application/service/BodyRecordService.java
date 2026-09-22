package com.myfitness.body.application.service;

import com.myfitness.body.domain.model.BodyRecord;
import com.myfitness.body.application.exception.BodyRecordAccessException;
import com.myfitness.body.application.exception.BodyRecordNotFoundException;
import com.myfitness.body.application.port.out.BodyRecordRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BodyRecordService {
    private final BodyRecordRepositoryPort bodyRecordRepository;
    private final Clock clock;

    @Autowired
    public BodyRecordService(BodyRecordRepositoryPort bodyRecordRepository) {
        this(bodyRecordRepository, Clock.systemUTC());
    }

    BodyRecordService(BodyRecordRepositoryPort bodyRecordRepository, Clock clock) {
        this.bodyRecordRepository = bodyRecordRepository;
        this.clock = clock;
    }

    public BodyRecord create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        Instant now = clock.instant();
        return bodyRecordRepository.save(BodyRecord.create(
                userId,
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo,
                now));
    }

    public BodyRecord update(
            BodyRecord record,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        record.update(
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo,
                clock.instant());
        return bodyRecordRepository.save(record);
    }

    public BodyRecord getOwned(Long userId, Long bodyRecordId) {
        BodyRecord record = bodyRecordRepository.findById(bodyRecordId)
                .orElseThrow(BodyRecordNotFoundException::new);
        if (!record.belongsTo(userId)) {
            throw new BodyRecordAccessException();
        }
        return record;
    }

    public List<BodyRecord> list(
            Long userId,
            Instant from,
            Instant to) {
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(90, ChronoUnit.DAYS) : from;
        return bodyRecordRepository
                .findByUserIdAndMeasuredAtBetween(
                        userId, start, end);
    }

    public List<BodyRecord> listRecent(Long userId, int days) {
        Instant end = clock.instant().plus(1, ChronoUnit.SECONDS);
        Instant start = end.minus(days, ChronoUnit.DAYS);
        return bodyRecordRepository
                .findByUserIdAndMeasuredAtBetween(
                        userId, start, end);
    }

    public void delete(BodyRecord record) {
        bodyRecordRepository.delete(record);
    }
}
