package com.myfitness.exercise.application.service;

import com.myfitness.exercise.domain.model.*;
import com.myfitness.exercise.application.port.in.ExerciseManagementUseCase;
import com.myfitness.exercise.application.port.in.catalog.ExerciseCatalogQuery;
import com.myfitness.exercise.application.exception.ExerciseNotFoundException;
import com.myfitness.exercise.domain.exception.ExerciseRuleException;
import com.myfitness.exercise.application.port.out.CustomExerciseRepositoryPort;
import com.myfitness.exercise.application.port.out.ExerciseRepositoryPort;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService implements ExerciseManagementUseCase, ExerciseCatalogQuery {
    private final ExerciseRepositoryPort exerciseRepository;
    private final CustomExerciseRepositoryPort customExerciseRepository;
    private final Clock clock;

    @Autowired
    public ExerciseService(
            ExerciseRepositoryPort exerciseRepository,
            CustomExerciseRepositoryPort customExerciseRepository) {
        this(exerciseRepository, customExerciseRepository, Clock.systemDefaultZone());
    }

    ExerciseService(
            ExerciseRepositoryPort exerciseRepository,
            CustomExerciseRepositoryPort customExerciseRepository,
            Clock clock) {
        this.exerciseRepository = exerciseRepository;
        this.customExerciseRepository = customExerciseRepository;
        this.clock = clock;
    }

    public ExerciseReference createCustom(
            Long userId,
            String name,
            ExerciseCategory category) {
        validateUserId(userId);
        if (exerciseRepository.existsByNameIgnoreCase(name)
                || customExerciseRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ExerciseRuleException("이미 등록된 운동 종목입니다.");
        }
        return customExerciseRepository.save(
                CustomExercise.create(userId, name, category, clock.instant()))
                .toReference();
    }

    public List<ExerciseReference> list(Long userId) {
        validateUserId(userId);
        List<ExerciseReference> result = new ArrayList<>();
        exerciseRepository.findAllOrdered()
                .stream().map(Exercise::toReference).forEach(result::add);
        customExerciseRepository.findAllByUserIdOrdered(userId)
                .stream().map(CustomExercise::toReference).forEach(result::add);
        return result;
    }

    public ExerciseReference getAvailable(
            Long userId,
            ExerciseType type,
            Long exerciseId) {
        validateUserId(userId);
        if (type == ExerciseType.DEFAULT) {
            return exerciseRepository.findById(exerciseId)
                    .map(Exercise::toReference)
                    .orElseThrow(() -> new ExerciseNotFoundException("기본 운동 종목"));
        }
        return customExerciseRepository.findByIdAndUserId(exerciseId, userId)
                .map(CustomExercise::toReference)
                .orElseThrow(() -> new ExerciseNotFoundException("커스텀 운동 종목"));
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ExerciseRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }
}
