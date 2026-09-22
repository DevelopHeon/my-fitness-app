package com.myfitness.exercise.infrastructure.bootstrap;

import com.myfitness.exercise.domain.model.Exercise;
import com.myfitness.exercise.application.port.out.ExerciseRepositoryPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultExerciseInitializer implements ApplicationRunner {
    private final ExerciseRepositoryPort exerciseRepository;

    public DefaultExerciseInitializer(ExerciseRepositoryPort exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DefaultExerciseCatalog.Item item : DefaultExerciseCatalog.items()) {
            if (!exerciseRepository.existsByNameIgnoreCase(item.name())) {
                exerciseRepository.save(
                        Exercise.create(item.name(), item.category(), item.sortOrder()));
            }
        }
    }
}
