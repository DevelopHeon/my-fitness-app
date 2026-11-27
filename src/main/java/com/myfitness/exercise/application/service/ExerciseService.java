package com.myfitness.exercise.application.service;

import com.myfitness.exercise.domain.model.*;
import com.myfitness.exercise.application.usecase.ExerciseManagementUseCase;
import com.myfitness.exercise.application.usecase.ExerciseCatalogUseCase;
import com.myfitness.exercise.application.usecase.ExerciseValue;
import com.myfitness.exercise.application.usecase.DefaultExerciseInitializationUseCase;
import com.myfitness.exercise.application.exception.ExerciseNotFoundException;
import com.myfitness.exercise.domain.exception.ExerciseRuleException;
import com.myfitness.exercise.domain.repository.CustomExerciseRepository;
import com.myfitness.exercise.domain.repository.ExerciseRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExerciseService implements ExerciseManagementUseCase,
        ExerciseCatalogUseCase, DefaultExerciseInitializationUseCase {
    private final ExerciseRepository exerciseRepository;
    private final CustomExerciseRepository customExerciseRepository;
    private final Clock clock;

    @Autowired
    public ExerciseService(
            ExerciseRepository exerciseRepository,
            CustomExerciseRepository customExerciseRepository) {
        this(exerciseRepository, customExerciseRepository, Clock.systemDefaultZone());
    }

    ExerciseService(
            ExerciseRepository exerciseRepository,
            CustomExerciseRepository customExerciseRepository,
            Clock clock) {
        this.exerciseRepository = exerciseRepository;
        this.customExerciseRepository = customExerciseRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void initialize() {
        for (DefaultExerciseCatalog.Item item : DefaultExerciseCatalog.items()) {
            if (!exerciseRepository.existsByNameIgnoreCase(item.name())) {
                exerciseRepository.save(
                        Exercise.create(item.name(), item.category(), item.sortOrder()));
            }
        }
    }

    @Override
    @Transactional
    public ExerciseValue createCustom(
            Long userId,
            String name,
            String category) {
        validateUserId(userId);
        validateDuplicateName(userId, null, name);
        return toValue(customExerciseRepository.save(
                CustomExercise.create(userId, name, ExerciseCategory.valueOf(category), clock.instant()))
                .toReference());
    }

    @Override
    @Transactional
    public ExerciseValue updateCustom(
            Long userId,
            Long exerciseId,
            String name,
            String category) {
        validateUserId(userId);
        CustomExercise exercise = getOwnedCustom(userId, exerciseId);
        validateDuplicateName(userId, exerciseId, name);
        exercise.update(name, ExerciseCategory.valueOf(category));
        return toValue(customExerciseRepository.save(exercise).toReference());
    }

    @Override
    @Transactional
    public void deleteCustom(Long userId, Long exerciseId) {
        validateUserId(userId);
        customExerciseRepository.delete(getOwnedCustom(userId, exerciseId));
    }

    public List<ExerciseValue> list(Long userId) {
        validateUserId(userId);
        List<ExerciseValue> result = new ArrayList<>();
        exerciseRepository.findAllOrdered()
                .stream().map(Exercise::toReference).map(ExerciseService::toValue).forEach(result::add);
        customExerciseRepository.findAllByUserIdOrdered(userId)
                .stream().map(CustomExercise::toReference).map(ExerciseService::toValue).forEach(result::add);
        return result;
    }

    public ExerciseValue getAvailable(
            Long userId,
            String type,
            Long exerciseId) {
        validateUserId(userId);
        if (ExerciseType.DEFAULT.name().equals(type)) {
            return exerciseRepository.findById(exerciseId)
                    .map(Exercise::toReference)
                    .map(ExerciseService::toValue)
                    .orElseThrow(() -> new ExerciseNotFoundException("기본 운동 종목"));
        }
        return customExerciseRepository.findByIdAndUserId(exerciseId, userId)
                .map(CustomExercise::toReference)
                .map(ExerciseService::toValue)
                .orElseThrow(() -> new ExerciseNotFoundException("커스텀 운동 종목"));
    }

    private static ExerciseValue toValue(ExerciseReference exercise) {
        return new ExerciseValue(
                exercise.id(), exercise.type().name(), exercise.name(), exercise.category().name(),
                exercise.ownerUserId());
    }

    private CustomExercise getOwnedCustom(Long userId, Long exerciseId) {
        if (exerciseId == null || exerciseId <= 0) {
            throw new ExerciseNotFoundException("커스텀 운동 종목");
        }
        return customExerciseRepository.findByIdAndUserId(exerciseId, userId)
                .orElseThrow(() -> new ExerciseNotFoundException("커스텀 운동 종목"));
    }

    private void validateDuplicateName(
            Long userId,
            Long exerciseId,
            String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        boolean duplicated = exerciseRepository.existsByNameIgnoreCase(name)
                || (exerciseId == null
                        ? customExerciseRepository.existsByUserIdAndNameIgnoreCase(
                                userId,
                                name)
                        : customExerciseRepository
                                .existsByUserIdAndNameIgnoreCaseAndIdNot(
                                        userId,
                                        name,
                                        exerciseId));
        if (duplicated) {
            throw new ExerciseRuleException("이미 등록된 운동 종목입니다.");
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ExerciseRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }
}
