package com.myfitness.body.infrastructure.persistence;

import com.myfitness.body.domain.exception.BodyRecordRuleException;
import com.myfitness.body.domain.model.BodyRecord;
import com.myfitness.body.application.port.out.BodyRecordRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class BodyRecordRepositoryAdapter implements BodyRecordRepositoryPort {
    private final SpringDataBodyRecordRepository repository;

    public BodyRecordRepositoryAdapter(SpringDataBodyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public BodyRecord save(BodyRecord record) {
        try {
            return repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new BodyRecordRuleException(
                    "동일한 측정 일시의 신체 기록이 이미 존재합니다.");
        }
    }

    @Override
    public Optional<BodyRecord> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<BodyRecord> findByUserIdAndMeasuredAtBetween(
            Long userId, Instant from, Instant to) {
        return repository
                .findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtDescIdDesc(
                        userId, from, to);
    }

    @Override
    public List<BodyRecord> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByMeasuredAtDescIdDesc(
                userId);
    }

    @Override
    public List<BodyRecord> findRecentByUserId(
            Long userId,
            int limit) {
        return repository.findAllByUserIdOrderByMeasuredAtDescIdDesc(
                userId,
                PageRequest.of(0, Math.max(1, limit)));
    }


    @Override
    public boolean existsByUserIdAndMeasuredAt(
            Long userId,
            Instant measuredAt) {
        return repository.existsByUserIdAndMeasuredAt(
                userId, measuredAt);
    }

    @Override
    public boolean existsByUserIdAndMeasuredAtAndIdNot(
            Long userId,
            Instant measuredAt,
            Long excludedId) {
        return repository.existsByUserIdAndMeasuredAtAndIdNot(
                userId, measuredAt, excludedId);
    }

    @Override
    public void delete(BodyRecord record) {
        repository.delete(record);
        repository.flush();
    }
}
