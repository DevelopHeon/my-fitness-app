package com.myfitness.architecture.fixture.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

@org.springframework.data.repository.NoRepositoryBean
public interface ForbiddenRepository extends JpaRepository<String, Long> {
}
