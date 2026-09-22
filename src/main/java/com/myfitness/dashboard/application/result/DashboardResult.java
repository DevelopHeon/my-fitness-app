package com.myfitness.dashboard.application.result;

import com.myfitness.exercise.domain.model.ExerciseCategory;
import com.myfitness.exercise.domain.model.ExerciseType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResult(
        LocalDate generatedDate,
        WorkoutSummary workout,
        BodySummary body,
        List<ExerciseSummary> exercises
) {
    public record WorkoutSummary(
            int last7DaysWorkoutCount,
            int last30DaysWorkoutCount,
            BigDecimal last7DaysVolume,
            BigDecimal previous7DaysVolume,
            BigDecimal last7DaysVolumeChangePercentage,
            BigDecimal last30DaysVolume,
            BigDecimal previous30DaysVolume,
            BigDecimal last30DaysVolumeChangePercentage,
            List<DailyVolume> dailyVolumes,
            List<CategoryDailyVolume> categoryDailyVolumes
    ) {}

    public record CategoryDailyVolume(
            ExerciseCategory category,
            List<DailyVolume> dailyVolumes
    ) {}

    public record DailyVolume(
            LocalDate date,
            BigDecimal volume
    ) {}

    public record BodySummary(
            BodyPoint latest,
            BodyChange changeFromPrevious,
            List<BodyPoint> history
    ) {}

    public record BodyPoint(
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}

    public record BodyChange(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {}

    public record ExerciseSummary(
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            BigDecimal maxWeightKg,
            BigDecimal maxEstimatedOneRepMax,
            LocalDate latestWorkoutDate,
            List<ExerciseRecord> recentRecords
    ) {}

    public record ExerciseRecord(
            LocalDate workoutDate,
            BigDecimal volume,
            BigDecimal maxWeightKg,
            BigDecimal maxEstimatedOneRepMax
    ) {}
}
