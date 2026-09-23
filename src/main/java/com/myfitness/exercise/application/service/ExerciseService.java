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
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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

    @Override
    @Transactional
    public ExerciseReference createCustom(
            Long userId,
            String name,
            ExerciseCategory category) {
        validateUserId(userId);
        validateDuplicateName(userId, null, name);
        return customExerciseRepository.save(
                CustomExercise.create(userId, name, category, clock.instant()))
                .toReference();
    }

    @Override
    @Transactional
    public ExerciseReference updateCustom(
            Long userId,
            Long exerciseId,
            String name,
            ExerciseCategory category) {
        validateUserId(userId);
        CustomExercise exercise = getOwnedCustom(userId, exerciseId);
        validateDuplicateName(userId, exerciseId, name);
        exercise.update(name, category);
        return customExerciseRepository.save(exercise).toReference();
    }

    @Override
    @Transactional
    public void deleteCustom(Long userId, Long exerciseId) {
        validateUserId(userId);
        customExerciseRepository.delete(getOwnedCustom(userId, exerciseId));
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
