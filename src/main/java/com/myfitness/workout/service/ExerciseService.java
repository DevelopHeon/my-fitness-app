package com.myfitness.workout.service;

import com.myfitness.workout.domain.*;
import com.myfitness.workout.exception.WorkoutNotFoundException;
import com.myfitness.workout.exception.WorkoutRuleException;
import com.myfitness.workout.repository.CustomExerciseRepository;
import com.myfitness.workout.repository.ExerciseRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
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

    public ExerciseReference createCustom(
            Long userId,
            String name,
            ExerciseCategory category) {
        validateUserId(userId);
        if (exerciseRepository.existsByNameIgnoreCase(name)
                || customExerciseRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new WorkoutRuleException("이미 등록된 운동 종목입니다.");
        }
        return customExerciseRepository.saveAndFlush(
                CustomExercise.create(userId, name, category, clock.instant()))
                .toReference();
    }

    public List<ExerciseReference> list(Long userId) {
        validateUserId(userId);
        List<ExerciseReference> result = new ArrayList<>();
        exerciseRepository.findAllByOrderByCategoryAscSortOrderAscNameAsc()
                .stream().map(Exercise::toReference).forEach(result::add);
        customExerciseRepository.findAllByUserIdOrderByCategoryAscNameAsc(userId)
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
                    .orElseThrow(() -> new WorkoutNotFoundException("기본 운동 종목"));
        }
        return customExerciseRepository.findByIdAndUserId(exerciseId, userId)
                .map(CustomExercise::toReference)
                .orElseThrow(() -> new WorkoutNotFoundException("커스텀 운동 종목"));
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WorkoutRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }
}
