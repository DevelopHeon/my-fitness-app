package com.myfitness.workout.repository;

import com.myfitness.workout.domain.CustomExercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomExerciseRepository extends JpaRepository<CustomExercise, Long> {
    List<CustomExercise> findAllByUserIdOrderByCategoryAscNameAsc(Long userId);
    Optional<CustomExercise> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
