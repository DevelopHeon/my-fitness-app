package com.myfitness.workout.repository;

import com.myfitness.workout.domain.Workout;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(
            Long userId, LocalDate from, LocalDate to);
}
