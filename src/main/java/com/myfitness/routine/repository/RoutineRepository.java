package com.myfitness.routine.repository;

import com.myfitness.routine.domain.Routine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
