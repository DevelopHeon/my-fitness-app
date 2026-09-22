package com.myfitness.workout.application.service;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.exercise.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.*;
import com.myfitness.workout.application.command.WorkoutSetCommand;
import com.myfitness.workout.application.exception.WorkoutAccessException;
import com.myfitness.workout.application.exception.WorkoutNotFoundException;
import com.myfitness.workout.application.port.out.WorkoutExerciseRepositoryPort;
import com.myfitness.workout.application.port.out.WorkoutRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkoutService {
    private final WorkoutRepositoryPort workoutRepository;
    private final WorkoutExerciseRepositoryPort workoutExerciseRepository;
    private final Clock clock;

    @Autowired
    public WorkoutService(
            WorkoutRepositoryPort workoutRepository,
            WorkoutExerciseRepositoryPort workoutExerciseRepository) {
        this(workoutRepository, workoutExerciseRepository, Clock.systemDefaultZone());
    }

    WorkoutService(
            WorkoutRepositoryPort workoutRepository,
            WorkoutExerciseRepositoryPort workoutExerciseRepository,
            Clock clock) {
        this.workoutRepository = workoutRepository;
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.clock = clock;
    }

    public Workout start(Long userId, LocalDate workoutDate, String memo) {
        return startWithExercises(userId, workoutDate, memo, List.of());
    }

    public Workout startWithExercises(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseReference> exercises) {
        LocalDate date = workoutDate == null ? LocalDate.now(clock) : workoutDate;
        Workout workout = Workout.start(userId, date, memo, clock.instant());
        exercises.forEach(exercise -> workout.addExercise(exercise, null));
        return workoutRepository.save(workout);
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
        return workoutRepository.findByUserIdAndDateRange(userId, start, end);
    }

    public Workout addExercise(
            Workout workout,
            ExerciseReference exercise,
            String memo) {
        workout.addExercise(exercise, memo);
        return workoutRepository.save(workout);
    }

    public Workout removeExercise(Workout workout, Long workoutExerciseId) {
        workout.removeExercise(requireWorkoutExercise(workout, workoutExerciseId));
        return workoutRepository.save(workout);
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
        return workoutRepository.save(workout);
    }

    public Workout addSets(
            Workout workout,
            Long workoutExerciseId,
            List<WorkoutSetCommand> commands) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        commands.forEach(command -> entry.addSet(
                command.weightKg(),
                command.reps(),
                command.durationSeconds(),
                command.completed()));
        return workoutRepository.save(workout);
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
        return workoutRepository.save(workout);
    }

    public Workout removeSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        entry.removeSet(requireSet(entry, setId));
        return workoutRepository.save(workout);
    }

    public Workout complete(Workout workout) {
        workout.complete(clock.instant());
        return workoutRepository.save(workout);
    }

    public Workout reopen(Workout workout) {
        workout.reopen();
        return workoutRepository.save(workout);
    }

    public WorkoutExercise getPreviousCompletedExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        return workoutExerciseRepository.findLatestCompletedByExercise(
                        userId, exerciseType, exerciseId)
                .orElse(null);
    }

    private static WorkoutExercise requireWorkoutExercise(
            Workout workout,
            Long workoutExerciseId) {
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
