package com.myfitness.routine.service;

import com.myfitness.routine.domain.Routine;
import com.myfitness.routine.exception.*;
import com.myfitness.routine.repository.RoutineRepository;
import com.myfitness.workout.domain.Exercise;
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

    public Routine create(Long userId, String name, List<Exercise> exercises) {
        if (routineRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new RoutineRuleException("이미 등록된 루틴 이름입니다.");
        }
        return routineRepository.saveAndFlush(Routine.create(userId, name, exercises, clock.instant()));
    }

    public Routine update(Routine routine, String name, List<Exercise> exercises) {
        if (routineRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(
                routine.getUserId(), name, routine.getId())) {
            throw new RoutineRuleException("이미 등록된 루틴 이름입니다.");
        }
        routine.update(name, exercises, clock.instant());
        return routineRepository.saveAndFlush(routine);
    }

    public Routine getOwned(Long userId, Long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(RoutineNotFoundException::new);
        if (!routine.belongsTo(userId)) throw new RoutineAccessException();
        return routine;
    }

    public List<Routine> list(Long userId) {
        return routineRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
    }

    public void delete(Routine routine) {
        routineRepository.delete(routine);
        routineRepository.flush();
    }
}
