package com.myfitness.body.application.service;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.body.application.usecase.BodyInsightUseCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BodyInsightPortService implements BodyInsightQuery {
    private final BodyInsightUseCase useCase;

    public BodyInsightPortService(BodyInsightUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public List<BodyInsight> findAll(Long userId) {
        return useCase.findAll(userId).stream().map(BodyInsightPortService::toInsight).toList();
    }

    @Override
    public List<BodyInsight> findRecent(Long userId, int limit) {
        return useCase.findRecent(userId, limit).stream().map(BodyInsightPortService::toInsight).toList();
    }

    private static BodyInsight toInsight(BodyRecordResult record) {
        return new BodyInsight(
                record.id(),
                record.measuredAt(),
                record.weightKg(),
                record.bodyFatPercentage(),
                record.skeletalMuscleKg());
    }
}
