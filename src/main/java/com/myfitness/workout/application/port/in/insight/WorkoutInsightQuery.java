package com.myfitness.workout.application.port.in.insight;

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
