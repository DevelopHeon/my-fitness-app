package com.myfitness.exercise.application.service;

import com.myfitness.exercise.application.port.in.DefaultExerciseInitializationPort;
import com.myfitness.exercise.application.usecase.DefaultExerciseInitializationUseCase;
import org.springframework.stereotype.Service;

@Service
public class DefaultExerciseInitializationPortService implements DefaultExerciseInitializationPort {
    private final DefaultExerciseInitializationUseCase useCase;

    public DefaultExerciseInitializationPortService(DefaultExerciseInitializationUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void initialize() {
        useCase.initialize();
    }
}
