package com.myfitness.body.infrastructure.persistence;

import com.myfitness.body.domain.model.BodyRecord;
import com.myfitness.body.domain.repository.BodyRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BodyRecordRepositoryAdapter implements BodyRecordRepository {
    private final SpringDataBodyRecordRepository repository;

    public BodyRecordRepositoryAdapter(SpringDataBodyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public BodyRecord save(BodyRecord record) {
        return repository.saveAndFlush(record);
    }

    @Override
    public Optional<BodyRecord> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<BodyRecord> findByUserIdAndMeasuredAtBetween(
            Long userId, Instant from, Instant to) {
        return repository.findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                userId, from, to);
    }

    @Override
    public List<BodyRecord> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByMeasuredAtDesc(userId);
    }

    @Override
    public void delete(BodyRecord record) {
        repository.delete(record);
        repository.flush();
    }
}
