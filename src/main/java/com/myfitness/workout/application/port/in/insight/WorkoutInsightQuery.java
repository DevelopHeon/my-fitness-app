package com.myfitness.workout.application.port.in.insight;

import com.myfitness.workout.application.dto.response.WorkoutResult;
import com.myfitness.workout.application.dto.response.WorkoutResult.ExerciseResult;
import com.myfitness.workout.application.dto.response.WorkoutResult.SetResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutInsightQuery {
    List<WorkoutInsight> findCompletedWorkouts(Long userId);

    List<WorkoutInsight> findCompletedSince(Long userId, LocalDate from);

    record WorkoutInsight(
            LocalDate workoutDate,
            Instant startedAt,
            List<ExerciseInsight> exercises
    ) {
        public static WorkoutInsight from(WorkoutResult workout) {
            return new WorkoutInsight(
                    workout.workoutDate(),
                    workout.startedAt(),
                    workout.exercises().stream().map(ExerciseInsight::from).toList());
        }

        public LocalDate getWorkoutDate() {
            return workoutDate;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public List<ExerciseInsight> getExercises() {
            return exercises;
        }
    }

    record ExerciseInsight(
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            List<SetInsight> sets
    ) {
        public static ExerciseInsight from(ExerciseResult exercise) {
            return new ExerciseInsight(
                    exercise.exerciseType().name(),
                    exercise.exerciseId(),
                    exercise.exerciseName(),
                    exercise.category(),
                    exercise.sets().stream().map(SetInsight::from).toList());
        }

        public String getExerciseType() {
            return exerciseType;
        }

        public Long getExerciseId() {
            return exerciseId;
        }

        public String getExerciseName() {
            return exerciseName;
        }

        public String getCategory() {
            return category;
        }

        public List<SetInsight> getSets() {
            return sets;
        }
    }

    record SetInsight(
            BigDecimal weightKg,
            int reps,
            boolean completed
    ) {
        public static SetInsight from(SetResult set) {
            return new SetInsight(
                    set.weightKg(),
                    set.reps(),
                    set.completed());
        }

        public BigDecimal getWeightKg() {
            return weightKg;
        }

        public int getReps() {
            return reps;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
