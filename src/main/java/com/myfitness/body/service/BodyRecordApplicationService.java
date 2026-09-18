package com.myfitness.body.service;

import com.myfitness.body.domain.BodyRecord;
import com.myfitness.body.dto.request.BodyRecordUpsertRequest;
import com.myfitness.body.dto.response.BodyRecordResponse;
import com.myfitness.body.dto.response.BodyTrendResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyRecordApplicationService {
    private final BodyRecordService bodyRecordService;

    public BodyRecordApplicationService(BodyRecordService bodyRecordService) {
        this.bodyRecordService = bodyRecordService;
    }

    @Transactional
    public BodyRecordResponse create(
            Long userId,
            BodyRecordUpsertRequest request) {
        return BodyRecordResponse.from(bodyRecordService.create(
                userId,
                request.weightKg(),
                request.bodyFatPercentage(),
                request.skeletalMuscleKg(),
                request.measuredAt(),
                request.memo()));
    }

    public List<BodyRecordResponse> list(
            Long userId,
            Instant from,
            Instant to) {
        return bodyRecordService.list(userId, from, to).stream()
                .map(BodyRecordResponse::from)
                .toList();
    }

    public BodyRecordResponse get(Long userId, Long bodyRecordId) {
        return BodyRecordResponse.from(
                bodyRecordService.getOwned(userId, bodyRecordId));
    }

    @Transactional
    public BodyRecordResponse update(
            Long userId,
            Long bodyRecordId,
            BodyRecordUpsertRequest request) {
        BodyRecord record = bodyRecordService.getOwned(userId, bodyRecordId);
        return BodyRecordResponse.from(bodyRecordService.update(
                record,
                request.weightKg(),
                request.bodyFatPercentage(),
                request.skeletalMuscleKg(),
                request.measuredAt(),
                request.memo()));
    }

    @Transactional
    public void delete(Long userId, Long bodyRecordId) {
        bodyRecordService.delete(
                bodyRecordService.getOwned(userId, bodyRecordId));
    }

    public BodyTrendResponse trend(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 3650));

        List<BodyRecordResponse> records =
                bodyRecordService.listRecent(userId, safeDays).stream()
                        .map(BodyRecordResponse::from)
                        .toList();

        BodyRecordResponse latest = records.isEmpty() ? null : records.getFirst();
        BodyTrendResponse.Change change = records.size() < 2
                ? null
                : new BodyTrendResponse.Change(
                        subtract(
                                records.getFirst().weightKg(),
                                records.get(1).weightKg()),
                        subtract(
                                records.getFirst().bodyFatPercentage(),
                                records.get(1).bodyFatPercentage()),
                        subtract(
                                records.getFirst().skeletalMuscleKg(),
                                records.get(1).skeletalMuscleKg()));

        return new BodyTrendResponse(latest, change, records);
    }

    private static BigDecimal subtract(BigDecimal current, BigDecimal previous) {
        return current.subtract(previous);
    }
}
