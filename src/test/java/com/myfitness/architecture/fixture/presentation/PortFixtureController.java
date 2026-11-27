package com.myfitness.architecture.fixture.presentation;

import com.myfitness.architecture.fixture.application.port.in.FixturePort;
import com.myfitness.architecture.fixture.application.usecase.InternalUseCase;

public interface PortFixtureController {
    FixturePort port();

    interface BypassController {
        InternalUseCase internal();
    }

    class BypassPortImplementation implements FixturePort {
        @Override
        public long load() {
            return 1L;
        }
    }
}
