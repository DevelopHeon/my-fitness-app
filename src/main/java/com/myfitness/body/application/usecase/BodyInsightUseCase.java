package com.myfitness.body.application.usecase;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import java.util.List;

public interface BodyInsightUseCase {
    List<BodyRecordResult> findAll(Long userId);

    List<BodyRecordResult> findRecent(Long userId, int limit);
}
