package com.myfitness.routine.infrastructure.module;

import com.myfitness.routine.application.usecase.response.RoutineWorkoutStartResult;
import com.myfitness.routine.application.port.out.RoutineWorkoutPort;
import com.myfitness.routine.domain.model.ExerciseSnapshot;
import com.myfitness.workout.application.port.in.routine.PreviousExerciseRecordView;
import com.myfitness.workout.application.port.in.routine.WorkoutRoutinePort;
import com.myfitness.workout.application.port.in.routine.WorkoutView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoutineWorkoutAdapter implements RoutineWorkoutPort {
    private final WorkoutRoutinePort workout;

    public RoutineWorkoutAdapter(WorkoutRoutinePort workout) {
        this.workout = workout;
    }

    @Override
    public RoutineWorkoutStartResult startWorkout(
            Long userId, LocalDate workoutDate, String memo, List<ExerciseSnapshot> exercises) {
        WorkoutRoutinePort.RoutineWorkoutView started = workout.startWorkout(
                userId, workoutDate, memo, exercises.stream()
                        .map(exercise -> new WorkoutRoutinePort.ExerciseSnapshot(
                                exercise.type().name(), exercise.id(), exercise.name(),
                                exercise.category().name(), exercise.ownerUserId()))
                        .toList());

        return new RoutineWorkoutStartResult(
                toWorkout(started.workout()),
                started.previousRecords().stream().map(RoutineWorkoutAdapter::toPrevious).toList());
    }

    private static RoutineWorkoutStartResult.Workout toWorkout(WorkoutView workout) {
        return new RoutineWorkoutStartResult.Workout(
                workout.id(), workout.workoutDate(), workout.status(), workout.memo(), workout.startedAt(),
                workout.completedAt(), workout.exercises().stream().map(RoutineWorkoutAdapter::toExercise).toList());
    }

    private static RoutineWorkoutStartResult.Exercise toExercise(WorkoutView.ExerciseView exercise) {
        return new RoutineWorkoutStartResult.Exercise(
                exercise.id(), exercise.exerciseType(), exercise.exerciseId(), exercise.exerciseName(),
                exercise.category(), exercise.orderIndex(), exercise.memo(), toSets(exercise.sets()));
    }

    private static RoutineWorkoutStartResult.PreviousRecord toPrevious(PreviousExerciseRecordView previous) {
        return new RoutineWorkoutStartResult.PreviousRecord(
                previous.workoutId(), previous.workoutDate(), previous.workoutExerciseId(), previous.exerciseType(),
                previous.exerciseId(), previous.exerciseName(), toSets(previous.sets()));
    }

    private static List<RoutineWorkoutStartResult.Set> toSets(List<WorkoutView.SetView> sets) {
        return sets.stream().map(set -> new RoutineWorkoutStartResult.Set(
                set.id(), set.setNumber(), set.weightKg(), set.reps(), set.durationSeconds(), set.completed())).toList();
    }
}
