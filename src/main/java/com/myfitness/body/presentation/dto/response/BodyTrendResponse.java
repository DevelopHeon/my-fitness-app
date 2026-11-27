package com.myfitness.body.presentation.dto.response;

import com.myfitness.body.application.port.in.BodyTrendView;
import java.math.BigDecimal;
import java.util.List;

public record BodyTrendResponse(
        BodyRecordResponse latest,
        Change changeFromPrevious,
        List<BodyRecordResponse> records
) {
    public static BodyTrendResponse from(BodyTrendView result) {
        return new BodyTrendResponse(
                result.latest() == null
                        ? null
                        : BodyRecordResponse.from(result.latest()),
                result.changeFromPrevious() == null
                        ? null
                        : new Change(
                                result.changeFromPrevious().weightKg(),
                                result.changeFromPrevious()
                                        .bodyFatPercentage(),
                                result.changeFromPrevious()
                                        .skeletalMuscleKg()),
                result.records().stream()
                        .map(BodyRecordResponse::from)
                        .toList());
    }

    public record Change(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}
}
