package com.myfitness.architecture.fixture.application.service;

import com.myfitness.architecture.fixture.application.port.out.FixtureOutputPort;
import com.myfitness.architecture.fixture.domain.NormalDomain;
import com.myfitness.architecture.fixture.domain.repository.NormalRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ForbiddenPortService {
    NormalDomain load();

    NormalRepository repository();

    FixtureOutputPort output();

    NormalPortService service();
}
