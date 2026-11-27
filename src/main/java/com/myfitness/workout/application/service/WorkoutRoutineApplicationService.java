package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.WorkoutOperationsUseCase;
import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.usecase.WorkoutRoutineUseCase;
import com.myfitness.workout.domain.model.ExerciseCategory;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.Workout;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutRoutineApplicationService implements WorkoutRoutineUseCase {
    private final WorkoutOperationsUseCase workoutService;

    public WorkoutRoutineApplicationService(WorkoutOperationsUseCase workoutService) {
        this.workoutService = workoutService;
    }

    @Override
    @Transactional
    public Started startWorkout(Long userId, LocalDate workoutDate, String memo, List<Snapshot> exercises) {
        List<ExerciseSnapshot> snapshots = exercises.stream()
                .map(exercise -> new ExerciseSnapshot(
                        ExerciseType.valueOf(exercise.type()), exercise.id(), exercise.name(),
                        ExerciseCategory.valueOf(exercise.category()), exercise.ownerUserId()))
                .toList();
        Workout workout = workoutService.startWithExercises(userId, workoutDate, memo, snapshots);
        List<PreviousExerciseRecordResult> previousRecords = workout.getExercises().stream()
                .map(entry -> workoutService.getPreviousCompletedExercise(
                        userId, entry.getExerciseType(), entry.getExerciseId()))
                .filter(Objects::nonNull)
                .toList();

        return new Started(WorkoutResult.from(workout), previousRecords);
    }
}
