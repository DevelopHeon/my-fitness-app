package com.myfitness.routine.domain.repository;

import com.myfitness.routine.domain.model.Routine;
import java.util.Optional;

public interface RoutineRepository {
    Routine save(Routine routine);
    Optional<Routine> findById(Long id);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
    void delete(Routine routine);
}
