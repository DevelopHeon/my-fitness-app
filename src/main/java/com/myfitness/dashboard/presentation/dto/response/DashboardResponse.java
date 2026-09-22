package com.myfitness.dashboard.presentation.dto.response;

import com.myfitness.dashboard.application.result.DashboardResult;
import com.myfitness.exercise.domain.model.ExerciseCategory;
import com.myfitness.exercise.domain.model.ExerciseType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate generatedDate,
        WorkoutSummary workout,
        BodySummary body,
        List<ExerciseSummary> exercises
) {
    public static DashboardResponse from(DashboardResult result) {
        return new DashboardResponse(
                result.generatedDate(),
                WorkoutSummary.from(result.workout()),
                BodySummary.from(result.body()),
                result.exercises().stream()
                        .map(ExerciseSummary::from)
                        .toList());
    }

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
    ) {
        static WorkoutSummary from(DashboardResult.WorkoutSummary result) {
            return new WorkoutSummary(
                    result.last7DaysWorkoutCount(),
                    result.last30DaysWorkoutCount(),
                    result.last7DaysVolume(),
                    result.previous7DaysVolume(),
                    result.last7DaysVolumeChangePercentage(),
                    result.last30DaysVolume(),
                    result.previous30DaysVolume(),
                    result.last30DaysVolumeChangePercentage(),
                    result.dailyVolumes().stream()
                            .map(DailyVolume::from)
                            .toList(),
                    result.categoryDailyVolumes().stream()
                            .map(CategoryDailyVolume::from)
                            .toList());
        }
    }

    public record CategoryDailyVolume(
            ExerciseCategory category,
            List<DailyVolume> dailyVolumes
    ) {
        static CategoryDailyVolume from(
                DashboardResult.CategoryDailyVolume result) {
            return new CategoryDailyVolume(
                    result.category(),
                    result.dailyVolumes().stream()
                            .map(DailyVolume::from)
                            .toList());
        }
    }

    public record DailyVolume(
            LocalDate date,
            BigDecimal volume
    ) {
        static DailyVolume from(DashboardResult.DailyVolume result) {
            return new DailyVolume(result.date(), result.volume());
        }
    }

    public record BodySummary(
            BodyPoint latest,
            BodyChange changeFromPrevious,
            List<BodyPoint> history
    ) {
        static BodySummary from(DashboardResult.BodySummary result) {
            return new BodySummary(
                    result.latest() == null
                            ? null
                            : BodyPoint.from(result.latest()),
                    result.changeFromPrevious() == null
                            ? null
                            : BodyChange.from(result.changeFromPrevious()),
                    result.history().stream()
                            .map(BodyPoint::from)
                            .toList());
        }
    }

    public record BodyPoint(
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        static BodyPoint from(DashboardResult.BodyPoint result) {
            return new BodyPoint(
                    result.measuredAt(),
                    result.weightKg(),
                    result.bodyFatPercentage(),
                    result.skeletalMuscleKg());
        }
    }

    public record BodyChange(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg
    ) {
        static BodyChange from(DashboardResult.BodyChange result) {
            return new BodyChange(
                    result.weightKg(),
                    result.bodyFatPercentage(),
                    result.skeletalMuscleKg());
        }
    }

    public record ExerciseSummary(
            ExerciseType exerciseType,
            Long exerciseId,
            String exerciseName,
            String category,
            BigDecimal maxWeightKg,
            BigDecimal maxEstimatedOneRepMax,
            LocalDate latestWorkoutDate,
            List<ExerciseRecord> recentRecords
    ) {
        static ExerciseSummary from(
                DashboardResult.ExerciseSummary result) {
            return new ExerciseSummary(
                    result.exerciseType(),
                    result.exerciseId(),
                    result.exerciseName(),
                    result.category(),
                    result.maxWeightKg(),
                    result.maxEstimatedOneRepMax(),
                    result.latestWorkoutDate(),
                    result.recentRecords().stream()
                            .map(ExerciseRecord::from)
                            .toList());
        }
    }

    public record ExerciseRecord(
            LocalDate workoutDate,
            BigDecimal volume,
            BigDecimal maxWeightKg,
            BigDecimal maxEstimatedOneRepMax
    ) {
        static ExerciseRecord from(DashboardResult.ExerciseRecord result) {
            return new ExerciseRecord(
                    result.workoutDate(),
                    result.volume(),
                    result.maxWeightKg(),
                    result.maxEstimatedOneRepMax());
        }
    }
}
