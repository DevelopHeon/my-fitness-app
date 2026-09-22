package com.myfitness.routine.infrastructure.persistence;

import com.myfitness.routine.domain.model.Routine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
