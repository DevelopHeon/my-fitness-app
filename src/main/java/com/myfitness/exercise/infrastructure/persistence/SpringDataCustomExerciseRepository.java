package com.myfitness.exercise.infrastructure.persistence;

import com.myfitness.exercise.domain.model.CustomExercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCustomExerciseRepository extends JpaRepository<CustomExercise, Long> {
    List<CustomExercise> findAllByUserIdOrderByCategoryAscNameAsc(Long userId);
    Optional<CustomExercise> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
