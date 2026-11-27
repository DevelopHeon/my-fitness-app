package com.myfitness.body.application.service;

import com.myfitness.body.application.usecase.response.BodyRecordResult;
import com.myfitness.body.application.usecase.BodyInsightUseCase;
import com.myfitness.body.application.port.out.BodyRecordReadPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyInsightService implements BodyInsightUseCase {
    private final BodyRecordReadPort bodyRecordReadPort;

    public BodyInsightService(BodyRecordReadPort bodyRecordReadPort) {
        this.bodyRecordReadPort = bodyRecordReadPort;
    }

    @Override
    public List<BodyRecordResult> findAll(Long userId) {
        return bodyRecordReadPort.findAllByUserId(userId);
    }

    @Override
    public List<BodyRecordResult> findRecent(Long userId, int limit) {
        return bodyRecordReadPort.findRecentByUserId(userId, limit);
    }

}
