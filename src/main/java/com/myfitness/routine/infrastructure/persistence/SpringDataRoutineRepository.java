package com.myfitness.routine.infrastructure.persistence;

import com.myfitness.routine.domain.model.Routine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRoutineRepository extends JpaRepository<Routine, Long> {
    @Override
    @EntityGraph(attributePaths = "exercises")
    Optional<Routine> findById(Long id);

    @EntityGraph(attributePaths = "exercises")
    List<Routine> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            Long userId,
            String name,
            Long id);
}
