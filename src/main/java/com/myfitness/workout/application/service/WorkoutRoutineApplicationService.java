package com.myfitness.workout.application.service;

import com.myfitness.exercise.domain.model.ExerciseReference;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.ExerciseView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.PreviousRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.RoutineWorkoutView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.SetView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutineUseCase.WorkoutView;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutRoutineApplicationService implements WorkoutRoutineUseCase {
    private final WorkoutService workoutService;

    public WorkoutRoutineApplicationService(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @Override
    @Transactional
    public RoutineWorkoutView startWorkout(
            Long userId,
            LocalDate workoutDate,
            String memo,
            List<ExerciseReference> exercises) {
        Workout workout = workoutService.startWithExercises(
                userId, workoutDate, memo, exercises);

        List<PreviousRecordView> previousRecords =
                workout.getExercises().stream()
                        .map(entry -> workoutService.getPreviousCompletedExercise(
                                userId,
                                entry.getExerciseType(),
                                entry.getExerciseId()))
                        .filter(previous -> previous != null)
                        .map(WorkoutRoutineApplicationService::toPreviousRecordView)
                        .toList();

        return new RoutineWorkoutView(
                toWorkoutView(workout),
                previousRecords);
    }

    private static WorkoutView toWorkoutView(Workout workout) {
        return new WorkoutView(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getStatus().name(),
                workout.getMemo(),
                workout.getStartedAt(),
                workout.getCompletedAt(),
                workout.getExercises().stream()
                        .map(WorkoutRoutineApplicationService::toExerciseView)
                        .toList());
    }

    private static ExerciseView toExerciseView(WorkoutExercise entry) {
        return new ExerciseView(
                entry.getId(),
                entry.getExerciseType(),
                entry.getExerciseId(),
                entry.getExerciseName(),
                entry.getCategory().name(),
                entry.getOrderIndex(),
                entry.getMemo(),
                entry.getSets().stream()
                        .map(WorkoutRoutineApplicationService::toSetView)
                        .toList());
    }

    private static SetView toSetView(
            com.myfitness.workout.domain.model.WorkoutSet set) {
        return new SetView(
                set.getId(),
                set.getSetNumber(),
                set.getWeightKg(),
                set.getReps(),
                set.getDurationSeconds(),
                set.isCompleted());
    }

    private static PreviousRecordView toPreviousRecordView(
            WorkoutExercise entry) {
        return new PreviousRecordView(
                entry.getWorkout().getId(),
                entry.getWorkout().getWorkoutDate(),
                entry.getId(),
                entry.getExerciseType(),
                entry.getExerciseId(),
                entry.getExerciseName(),
                entry.getSets().stream()
                        .map(WorkoutRoutineApplicationService::toSetView)
                        .toList());
    }
}
