package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutStatus;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutRepositoryAdapter implements WorkoutRepository {
    private final SpringDataWorkoutRepository repository;

    public WorkoutRepositoryAdapter(SpringDataWorkoutRepository repository) {
        this.repository = repository;
    }

    @Override
    public Workout save(Workout workout) {
        return repository.saveAndFlush(workout);
    }

    @Override
    public Optional<Workout> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Workout> findByUserIdAndDateRange(
            Long userId, LocalDate from, LocalDate to) {
        return repository.findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(
                userId, from, to);
    }

    @Override
    public List<Workout> findByUserIdAndStatus(
            Long userId, WorkoutStatus status) {
        return repository.findAllByUserIdAndStatusOrderByWorkoutDateAscStartedAtAsc(
                userId, status);
    }
}
