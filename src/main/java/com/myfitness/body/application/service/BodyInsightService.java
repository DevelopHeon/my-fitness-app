package com.myfitness.body.application.service;

import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.body.application.port.out.BodyRecordRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyInsightService implements BodyInsightQuery {
    private final BodyRecordRepositoryPort bodyRecordRepositoryPort;

    public BodyInsightService(BodyRecordRepositoryPort bodyRecordRepositoryPort) {
        this.bodyRecordRepositoryPort = bodyRecordRepositoryPort;
    }

    @Override
    public List<BodyInsight> findAll(Long userId) {
        return toInsights(bodyRecordRepositoryPort.findAllByUserId(userId));
    }

    @Override
    public List<BodyInsight> findRecent(Long userId, int limit) {
        return toInsights(
                bodyRecordRepositoryPort.findRecentByUserId(userId, limit));
    }

    private static List<BodyInsight> toInsights(
            List<com.myfitness.body.domain.model.BodyRecord> records) {
        return records.stream()
                .map(record -> new BodyInsight(
                        record.getId(),
                        record.getMeasuredAt(),
                        record.getWeightKg(),
                        record.getBodyFatPercentage(),
                        record.getSkeletalMuscleKg()))
                .toList();
    }
}
