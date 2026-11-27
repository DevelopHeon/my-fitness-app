package com.myfitness.exercise.infrastructure.bootstrap;

import com.myfitness.exercise.application.port.in.DefaultExerciseInitializationPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultExerciseInitializer implements ApplicationRunner {
    private final DefaultExerciseInitializationPort initialization;

    public DefaultExerciseInitializer(DefaultExerciseInitializationPort initialization) {
        this.initialization = initialization;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialization.initialize();
    }
}
