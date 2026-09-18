package com.myfitness.workout.service;

import com.myfitness.workout.domain.Exercise;
import com.myfitness.workout.exception.WorkoutNotFoundException;
import com.myfitness.workout.exception.WorkoutRuleException;
import com.myfitness.workout.repository.ExerciseRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final Clock clock;

    @Autowired
    public ExerciseService(ExerciseRepository exerciseRepository) {
        this(exerciseRepository, Clock.systemDefaultZone());
    }

    ExerciseService(ExerciseRepository exerciseRepository, Clock clock) {
        this.exerciseRepository = exerciseRepository;
        this.clock = clock;
    }

    public Exercise create(Long userId, String name, String category) {
        validateUserId(userId);
        if (exerciseRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new WorkoutRuleException("이미 등록된 운동 종목입니다.");
        }
        return exerciseRepository.saveAndFlush(
                Exercise.create(userId, name, category, clock.instant()));
    }

    public List<Exercise> list(Long userId) {
        validateUserId(userId);
        return exerciseRepository.findAllByUserIdOrderByNameAsc(userId);
    }

    public Exercise getOwned(Long userId, Long exerciseId) {
        validateUserId(userId);
        return exerciseRepository.findByIdAndUserId(exerciseId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException("운동 종목"));
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WorkoutRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }
}
