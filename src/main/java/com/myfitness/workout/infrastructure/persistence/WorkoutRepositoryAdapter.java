package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.workout.application.usecase.response.WorkoutCalendarDayResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.port.out.WorkoutReadPort;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutStatus;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutRepositoryAdapter implements WorkoutRepository, WorkoutReadPort {
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
    public List<WorkoutResult> findByUserIdAndDateRange(
            Long userId, LocalDate from, LocalDate to) {
        return repository.findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(
                userId, from, to).stream().map(WorkoutResult::from).toList();
    }

    @Override
    public List<WorkoutCalendarDayResult> findCalendar(Long userId, LocalDate from, LocalDate to) {
        return WorkoutCalendarDayResult.groupByDate(
                repository.findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(userId, from, to));
    }

    @Override
    public List<WorkoutResult> findCompleted(Long userId) {
        return repository.findAllByUserIdAndStatusOrderByWorkoutDateAscStartedAtAsc(
                userId, WorkoutStatus.COMPLETED).stream().map(WorkoutResult::from).toList();
    }

    @Override
    public List<WorkoutResult> findCompletedSince(Long userId, LocalDate from) {
        return repository
                .findAllByUserIdAndStatusAndWorkoutDateGreaterThanEqualOrderByWorkoutDateDescStartedAtDesc(
                        userId, WorkoutStatus.COMPLETED, from).stream().map(WorkoutResult::from).toList();
    }
}
