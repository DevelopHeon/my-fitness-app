package com.myfitness.workout.service;

import com.myfitness.workout.domain.Exercise;
import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.domain.WorkoutSet;
import com.myfitness.workout.domain.WorkoutStatus;
import com.myfitness.workout.exception.WorkoutAccessException;
import com.myfitness.workout.exception.WorkoutNotFoundException;
import com.myfitness.workout.repository.WorkoutExerciseRepository;
import com.myfitness.workout.repository.WorkoutRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final Clock clock;

    @Autowired
    public WorkoutService(
            WorkoutRepository workoutRepository,
            WorkoutExerciseRepository workoutExerciseRepository) {
        this(workoutRepository, workoutExerciseRepository, Clock.systemDefaultZone());
    }

    WorkoutService(
            WorkoutRepository workoutRepository,
            WorkoutExerciseRepository workoutExerciseRepository,
            Clock clock) {
        this.workoutRepository = workoutRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.clock = clock;
    }

    public Workout start(Long userId, LocalDate workoutDate, String memo) {
        LocalDate date = workoutDate == null ? LocalDate.now(clock) : workoutDate;
        return workoutRepository.saveAndFlush(
                Workout.start(userId, date, memo, clock.instant()));
    }

    public Workout getOwned(Long userId, Long workoutId) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout"));
        if (!workout.getUserId().equals(userId)) {
            throw new WorkoutAccessException();
        }
        return workout;
    }

    public List<Workout> list(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        return workoutRepository.findAllByUserIdAndWorkoutDateBetweenOrderByStartedAtDesc(
                userId, start, end);
    }

    public Workout addExercise(Workout workout, Exercise exercise, String memo) {
        workout.addExercise(exercise, memo);
        return workoutRepository.saveAndFlush(workout);
    }

    public Workout removeExercise(Workout workout, Long workoutExerciseId) {
        workout.removeExercise(requireWorkoutExercise(workout, workoutExerciseId));
        return workoutRepository.saveAndFlush(workout);
    }

    public Workout addSet(
            Workout workout,
            Long workoutExerciseId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        entry.addSet(weightKg, reps, durationSeconds, completed);
        return workoutRepository.saveAndFlush(workout);
    }

    public Workout updateSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        WorkoutSet set = requireSet(entry, setId);
        entry.updateSet(set, weightKg, reps, durationSeconds, completed);
        return workoutRepository.saveAndFlush(workout);
    }

    public Workout removeSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        entry.removeSet(requireSet(entry, setId));
        return workoutRepository.saveAndFlush(workout);
    }

    public Workout complete(Workout workout) {
        workout.complete(clock.instant());
        return workoutRepository.saveAndFlush(workout);
    }

    public WorkoutExercise getPreviousCompletedExercise(Long userId, Long exerciseId) {
        return workoutExerciseRepository
                .findFirstByWorkout_UserIdAndExercise_IdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
                        userId, exerciseId, WorkoutStatus.COMPLETED)
                .orElse(null);
    }

    private static WorkoutExercise requireWorkoutExercise(Workout workout, Long workoutExerciseId) {
        return workout.getExercises().stream()
                .filter(entry -> entry.getId().equals(workoutExerciseId))
                .findFirst()
                .orElseThrow(() -> new WorkoutNotFoundException("WorkoutExercise"));
    }

    private static WorkoutSet requireSet(WorkoutExercise entry, Long setId) {
        return entry.getSets().stream()
                .filter(set -> set.getId().equals(setId))
                .findFirst()
                .orElseThrow(() -> new WorkoutNotFoundException("WorkoutSet"));
    }
}
