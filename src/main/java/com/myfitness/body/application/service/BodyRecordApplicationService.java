package com.myfitness.body.application.service;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import com.myfitness.body.application.usecase.response.BodyTrendResult;
import com.myfitness.body.application.exception.BodyRecordAccessException;
import com.myfitness.body.application.exception.BodyRecordNotFoundException;
import com.myfitness.body.application.port.out.BodyRecordReadPort;
import com.myfitness.body.application.usecase.BodyRecordUseCase;
import com.myfitness.body.domain.exception.BodyRecordRuleException;
import com.myfitness.body.domain.repository.BodyRecordRepository;
import com.myfitness.body.domain.model.BodyRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyRecordApplicationService implements BodyRecordUseCase {
    private final BodyRecordRepository bodyRecordRepository;
    private final BodyRecordReadPort bodyRecordReadPort;
    private final Clock clock = Clock.systemUTC();

    public BodyRecordApplicationService(
            BodyRecordRepository bodyRecordRepository,
            BodyRecordReadPort bodyRecordReadPort) {
        this.bodyRecordRepository = bodyRecordRepository;
        this.bodyRecordReadPort = bodyRecordReadPort;
    }

    @Override
    @Transactional
    public BodyRecordResult create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        Instant now = clock.instant();
        validateMeasuredAtAvailable(userId, measuredAt, null);
        BodyRecord record = BodyRecord.create(
                userId,
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo,
                now);
        return BodyRecordResult.from(bodyRecordRepository.save(record));
    }

    @Override
    @Transactional
    public BodyRecordResult update(
            Long userId,
            Long bodyRecordId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        BodyRecord record = getOwned(userId, bodyRecordId);
        validateMeasuredAtAvailable(
                record.getUserId(),
                measuredAt,
                record.getId());
        record.update(
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo,
                clock.instant());
        return BodyRecordResult.from(bodyRecordRepository.save(record));
    }

    @Override
    public List<BodyRecordResult> list(
            Long userId,
            Instant from,
            Instant to) {
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(90, ChronoUnit.DAYS) : from;
        return bodyRecordReadPort
                .findByUserIdAndMeasuredAtBetween(
                        userId, start, end);
    }

    @Override
    public BodyRecordResult get(Long userId, Long bodyRecordId) {
        return BodyRecordResult.from(getOwned(userId, bodyRecordId));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long bodyRecordId) {
        bodyRecordRepository.delete(getOwned(userId, bodyRecordId));
    }

    @Override
    public BodyTrendResult trend(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 3650));
        Instant end = clock.instant().plus(1, ChronoUnit.SECONDS);
        Instant start = end.minus(safeDays, ChronoUnit.DAYS);
        List<BodyRecordResult> records = bodyRecordReadPort.findByUserIdAndMeasuredAtBetween(userId, start, end);
        return BodyTrendResult.from(bodyRecordReadPort.findRecentByUserId(userId, 2), records);
    }

    private BodyRecord getOwned(Long userId, Long bodyRecordId) {
        BodyRecord record = bodyRecordRepository.findById(bodyRecordId)
                .orElseThrow(BodyRecordNotFoundException::new);
        if (!record.belongsTo(userId)) {
            throw new BodyRecordAccessException();
        }
        return record;
    }

    private void validateMeasuredAtAvailable(
            Long userId,
            Instant measuredAt,
            Long excludedId) {
        boolean exists = excludedId == null
                ? bodyRecordRepository.existsByUserIdAndMeasuredAt(
                        userId, measuredAt)
                : bodyRecordRepository
                        .existsByUserIdAndMeasuredAtAndIdNot(
                                userId, measuredAt, excludedId);

        if (exists) {
            throw new BodyRecordRuleException(
                    "동일한 측정 일시의 신체 기록이 이미 존재합니다.");
        }
    }
}
