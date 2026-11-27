package com.myfitness.architecture.fixture.application.service;

import com.myfitness.body.application.port.in.insight.BodyInsightQuery;

public interface ForeignPortConsumer {
    BodyInsightQuery otherModule();
}
