package com.myfitness.workout.service;

import com.myfitness.workout.domain.Exercise;
import com.myfitness.workout.repository.ExerciseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultExerciseInitializer implements ApplicationRunner {
    private final ExerciseRepository exerciseRepository;

    public DefaultExerciseInitializer(ExerciseRepository exerciseRepository) {
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
