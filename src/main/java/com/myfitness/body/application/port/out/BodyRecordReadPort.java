package com.myfitness.body.application.port.out;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import java.time.Instant;
import java.util.List;

public interface BodyRecordReadPort {
    List<BodyRecordResult> findByUserIdAndMeasuredAtBetween(
            Long userId,
            Instant from,
            Instant to);

    List<BodyRecordResult> findAllByUserId(Long userId);

    List<BodyRecordResult> findRecentByUserId(Long userId, int limit);
}
