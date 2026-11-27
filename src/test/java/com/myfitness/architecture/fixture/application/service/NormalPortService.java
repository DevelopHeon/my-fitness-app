package com.myfitness.architecture.fixture.application.service;

import com.myfitness.architecture.fixture.application.port.in.FixturePort;

public class NormalPortService implements FixturePort {
    @Override
    public long load() {
        return 1L;
    }
}
