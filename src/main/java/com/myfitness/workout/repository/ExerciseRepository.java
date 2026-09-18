package com.myfitness.workout.repository;

import com.myfitness.workout.domain.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findAllByOrderByCategoryAscSortOrderAscNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
