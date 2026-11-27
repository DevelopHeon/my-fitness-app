package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.WorkoutOperationsUseCase;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.application.usecase.request.WorkoutSetCommand;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.exception.WorkoutAccessException;
import com.myfitness.workout.application.exception.WorkoutNotFoundException;
import com.myfitness.workout.application.port.out.PreviousExerciseRecordQueryPort;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import com.myfitness.workout.domain.model.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkoutService implements WorkoutOperationsUseCase {
    private final WorkoutRepository workoutRepository;
    private final PreviousExerciseRecordQueryPort previousRecordQuery;
    private final Clock clock;

    @Autowired
    public WorkoutService(
            WorkoutRepository workoutRepository,
            PreviousExerciseRecordQueryPort previousRecordQuery) {
        this(workoutRepository, previousRecordQuery, Clock.systemDefaultZone());
    }

    WorkoutService(
            WorkoutRepository workoutRepository,
            PreviousExerciseRecordQueryPort previousRecordQuery,
            Clock clock) {
        this.workoutRepository = workoutRepository;
        this.previousRecordQuery = previousRecordQuery;
        this.clock = clock;
    }

    @Override
    public Workout start(Long userId, LocalDate workoutDate, String memo) {
        return startWithExercises(userId, workoutDate, memo, List.of());
    }

    @Override
    public Workout startWithExercises(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseSnapshot> exercises) {
        LocalDate date = workoutDate == null ? LocalDate.now(clock) : workoutDate;
        Workout workout = Workout.start(userId, date, memo, clock.instant());
        exercises.forEach(exercise -> workout.addExercise(exercise, null));
        return workoutRepository.save(workout);
    }

    @Override
    public Workout getOwned(Long userId, Long workoutId) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout"));
        if (!workout.getUserId().equals(userId)) {
            throw new WorkoutAccessException();
        }
        return workout;
    }

    @Override
    public Workout addExercise(
            Workout workout,
            ExerciseSnapshot exercise,
            String memo) {
        workout.addExercise(exercise, memo);
        return workoutRepository.save(workout);
    }

    @Override
    public Workout removeExercise(Workout workout, Long workoutExerciseId) {
        workout.removeExercise(requireWorkoutExercise(workout, workoutExerciseId));
        return workoutRepository.save(workout);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Workout removeSet(
            Workout workout,
            Long workoutExerciseId,
            Long setId) {
        WorkoutExercise entry = requireWorkoutExercise(workout, workoutExerciseId);
        entry.removeSet(requireSet(entry, setId));
        return workoutRepository.save(workout);
    }

    @Override
    public Workout complete(Workout workout) {
        workout.complete(clock.instant());
        return workoutRepository.save(workout);
    }

    @Override
    public Workout reopen(Workout workout) {
        workout.reopen();
        return workoutRepository.save(workout);
    }

    @Override
    public PreviousExerciseRecordResult getPreviousCompletedExercise(
            Long userId,
            ExerciseType exerciseType,
            Long exerciseId) {
        return previousRecordQuery.findLatestCompletedByExercise(
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
