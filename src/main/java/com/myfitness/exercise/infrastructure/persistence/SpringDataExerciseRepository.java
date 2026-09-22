package com.myfitness.exercise.infrastructure.persistence;

import com.myfitness.exercise.domain.model.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findAllByOrderByCategoryAscSortOrderAscNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
