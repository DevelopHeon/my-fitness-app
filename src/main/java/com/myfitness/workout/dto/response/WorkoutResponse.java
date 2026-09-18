package com.myfitness.workout.dto.response;

import com.myfitness.workout.domain.Exercise;
import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.domain.WorkoutSet;
import com.myfitness.workout.domain.WorkoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(
        Long id,
        LocalDate workoutDate,
        WorkoutStatus status,
        String memo,
        Instant startedAt,
        Instant completedAt,
        List<ExerciseEntry> exercises
) {
    public static WorkoutResponse from(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getStatus(),
                workout.getMemo(),
                workout.getStartedAt(),
                workout.getCompletedAt(),
                workout.getExercises().stream().map(ExerciseEntry::from).toList());
    }

    public record ExerciseEntry(
            Long id,
            Long exerciseId,
            String exerciseName,
            String category,
            int orderIndex,
            String memo,
            List<SetEntry> sets
    ) {
        static ExerciseEntry from(WorkoutExercise entry) {
            Exercise exercise = entry.getExercise();
            return new ExerciseEntry(
                    entry.getId(),
                    exercise.getId(),
                    exercise.getName(),
                    exercise.getCategory(),
                    entry.getOrderIndex(),
                    entry.getMemo(),
                    entry.getSets().stream().map(SetEntry::from).toList());
        }
    }

    public record SetEntry(
            Long id,
            int setNumber,
            BigDecimal weightKg,
            int reps,
            Integer durationSeconds,
            boolean completed
    ) {
        static SetEntry from(WorkoutSet set) {
            return new SetEntry(
                    set.getId(),
                    set.getSetNumber(),
                    set.getWeightKg(),
                    set.getReps(),
                    set.getDurationSeconds(),
                    set.isCompleted());
        }
    }
}
