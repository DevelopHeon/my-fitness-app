package com.myfitness.routine.application.service;

import com.myfitness.routine.domain.model.Routine;
import com.myfitness.routine.application.exception.RoutineAccessException;
import com.myfitness.routine.application.exception.RoutineNotFoundException;
import com.myfitness.routine.domain.exception.RoutineRuleException;
import com.myfitness.routine.domain.repository.RoutineRepository;
import com.myfitness.exercise.domain.model.ExerciseReference;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final Clock clock;

    @Autowired
    public RoutineService(RoutineRepository routineRepository) {
        this(routineRepository, Clock.systemDefaultZone());
    }

    RoutineService(RoutineRepository routineRepository, Clock clock) {
        this.routineRepository = routineRepository;
        this.clock = clock;
    }

    public Routine create(
            Long userId,
            String name,
            List<ExerciseReference> exercises) {
        if (routineRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new RoutineRuleException("이미 등록된 루틴 이름입니다.");
        }
        return routineRepository.save(
                Routine.create(userId, name, exercises, clock.instant()));
    }

    public Routine update(
            Routine routine,
            String name,
            List<ExerciseReference> exercises) {
        if (routineRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(
                routine.getUserId(), name, routine.getId())) {
            throw new RoutineRuleException("이미 등록된 루틴 이름입니다.");
        }
        routine.update(name, exercises, clock.instant());
        return routineRepository.save(routine);
    }

    public Routine getOwned(Long userId, Long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(RoutineNotFoundException::new);
        if (!routine.belongsTo(userId)) {
            throw new RoutineAccessException();
        }
        return routine;
    }

    public List<Routine> list(Long userId) {
        return routineRepository.findAllByUserId(userId);
    }

    public void delete(Routine routine) {
        routineRepository.delete(routine);
    }
}
