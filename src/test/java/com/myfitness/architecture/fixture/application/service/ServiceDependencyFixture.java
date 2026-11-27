package com.myfitness.architecture.fixture.application.service;

import com.myfitness.architecture.fixture.application.usecase.InternalUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

public final class ServiceDependencyFixture {
    private ServiceDependencyFixture() {}

    @Service
    @Profile("architecture-fixture")
    public static class BusinessService implements InternalUseCase {
        @Override
        public long load() {
            return 1L;
        }
    }

    @Service
    @Profile("architecture-fixture")
    public record UsesContract(InternalUseCase operation) {}

    @Service
    @Profile("architecture-fixture")
    public record BypassesContract(BusinessService operation) {}
}
