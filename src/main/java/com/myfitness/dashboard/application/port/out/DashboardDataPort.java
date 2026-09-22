package com.myfitness.dashboard.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DashboardDataPort {
    DashboardSourceData load(Long userId);

    record DashboardSourceData(
            List<WorkoutData> completedWorkouts,
            List<BodyData> bodyRecords
    ) {}

    record WorkoutData(
            LocalDate workoutDate,
            Instant startedAt,
            List<ExerciseData> exercises
    ) {
        public LocalDate getWorkoutDate() {
            return workoutDate;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public List<ExerciseData> getExercises() {
            return exercises;
        }
    }

    record ExerciseData(
            String exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            List<SetData> sets
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

        public List<SetData> getSets() {
            return sets;
        }
    }

    record SetData(
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

    record BodyData(
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        public Instant getMeasuredAt() {
            return measuredAt;
        }

        public BigDecimal getWeightKg() {
            return weightKg;
        }

        public BigDecimal getBodyFatPercentage() {
            return bodyFatPercentage;
        }

        public BigDecimal getSkeletalMuscleKg() {
            return skeletalMuscleKg;
        }
    }
}
