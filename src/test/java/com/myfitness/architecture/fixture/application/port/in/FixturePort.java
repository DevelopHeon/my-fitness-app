package com.myfitness.architecture.fixture.application.port.in;

import com.myfitness.architecture.fixture.application.usecase.InternalUseCase;

public interface FixturePort {
    long load();

    interface ForbiddenPort {
        InternalUseCase internal();
    }
}
