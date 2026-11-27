package com.myfitness.body.application.service;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import com.myfitness.body.application.usecase.response.BodyTrendResult;
import com.myfitness.body.application.port.in.BodyRecordPort;
import com.myfitness.body.application.port.in.BodyRecordView;
import com.myfitness.body.application.port.in.BodyTrendView;
import com.myfitness.body.application.usecase.BodyRecordUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BodyRecordPortService implements BodyRecordPort {
    private final BodyRecordUseCase useCase;

    public BodyRecordPortService(BodyRecordUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public BodyRecordView create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        return toView(useCase.create(userId, weightKg, bodyFatPercentage, skeletalMuscleKg, measuredAt, memo));
    }

    @Override
    public List<BodyRecordView> list(Long userId, Instant from, Instant to) {
        return useCase.list(userId, from, to).stream().map(BodyRecordPortService::toView).toList();
    }

    @Override
    public BodyRecordView get(Long userId, Long bodyRecordId) {
        return toView(useCase.get(userId, bodyRecordId));
    }

    @Override
    public BodyRecordView update(
            Long userId,
            Long bodyRecordId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo) {
        return toView(useCase.update(
                userId, bodyRecordId, weightKg, bodyFatPercentage, skeletalMuscleKg, measuredAt, memo));
    }

    @Override
    public void delete(Long userId, Long bodyRecordId) {
        useCase.delete(userId, bodyRecordId);
    }

    @Override
    public BodyTrendView trend(Long userId, int days) {
        BodyTrendResult trend = useCase.trend(userId, days);
        BodyTrendResult.Change change = trend.changeFromPrevious();
        return new BodyTrendView(
                trend.latest() == null ? null : toView(trend.latest()),
                change == null ? null : new BodyTrendView.Change(
                        change.weightKg(), change.bodyFatPercentage(), change.skeletalMuscleKg()),
                trend.records().stream().map(BodyRecordPortService::toView).toList());
    }

    private static BodyRecordView toView(BodyRecordResult record) {
        return new BodyRecordView(
                record.id(), record.weightKg(), record.bodyFatPercentage(), record.skeletalMuscleKg(),
                record.measuredAt(), record.memo(), record.createdAt(), record.updatedAt());
    }
}
