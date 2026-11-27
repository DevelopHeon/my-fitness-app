package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.usecase.response.DashboardResult;
import com.myfitness.dashboard.application.port.in.DashboardQueryPort;
import com.myfitness.dashboard.application.port.in.DashboardView;
import com.myfitness.dashboard.application.usecase.DashboardQueryUseCase;
import org.springframework.stereotype.Service;

@Service
public class DashboardPortService implements DashboardQueryPort {
    private final DashboardQueryUseCase useCase;

    public DashboardPortService(DashboardQueryUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public DashboardView getDashboard(Long userId) {
        return toView(useCase.getDashboard(userId));
    }

    private static DashboardView toView(DashboardResult value) {
        return new DashboardView(
                value.generatedDate(),
                toWorkoutSummary(value.workout()),
                toBodySummary(value.body()),
                value.exercises().stream().map(DashboardPortService::toExerciseSummary).toList());
    }

    private static DashboardView.WorkoutSummary toWorkoutSummary(DashboardResult.WorkoutSummary value) {
        return new DashboardView.WorkoutSummary(
                value.last7DaysWorkoutCount(),
                value.last30DaysWorkoutCount(),
                value.last7DaysVolume(),
                value.previous7DaysVolume(),
                value.last7DaysVolumeChangePercentage(),
                value.last30DaysVolume(),
                value.previous30DaysVolume(),
                value.last30DaysVolumeChangePercentage(),
                value.dailyVolumes().stream().map(DashboardPortService::toDailyVolume).toList(),
                value.categoryDailyVolumes().stream().map(DashboardPortService::toCategoryDailyVolume).toList());
    }

    private static DashboardView.CategoryDailyVolume toCategoryDailyVolume(DashboardResult.CategoryDailyVolume value) {
        return new DashboardView.CategoryDailyVolume(
                value.category(),
                value.dailyVolumes().stream().map(DashboardPortService::toDailyVolume).toList());
    }

    private static DashboardView.DailyVolume toDailyVolume(DashboardResult.DailyVolume value) {
        return new DashboardView.DailyVolume(
                value.date(),
                value.volume());
    }

    private static DashboardView.BodySummary toBodySummary(DashboardResult.BodySummary value) {
        return new DashboardView.BodySummary(
                toBodyPoint(value.latest()),
                toBodyChange(value.changeFromPrevious()),
                value.history().stream().map(DashboardPortService::toBodyPoint).toList());
    }

    private static DashboardView.BodyPoint toBodyPoint(DashboardResult.BodyPoint value) {
        if (value == null) {
            return null;
        }
        return new DashboardView.BodyPoint(
                value.measuredAt(),
                value.weightKg(),
                value.bodyFatPercentage(),
                value.skeletalMuscleKg());
    }

    private static DashboardView.BodyChange toBodyChange(DashboardResult.BodyChange value) {
        if (value == null) {
            return null;
        }
        return new DashboardView.BodyChange(
                value.weightKg(),
                value.bodyFatPercentage(),
                value.skeletalMuscleKg());
    }

    private static DashboardView.ExerciseSummary toExerciseSummary(DashboardResult.ExerciseSummary value) {
        return new DashboardView.ExerciseSummary(
                value.exerciseType(),
                value.exerciseId(),
                value.exerciseName(),
                value.category(),
                value.maxWeightKg(),
                value.maxEstimatedOneRepMax(),
                value.latestWorkoutDate(),
                value.recentRecords().stream().map(DashboardPortService::toExerciseRecord).toList());
    }

    private static DashboardView.ExerciseRecord toExerciseRecord(DashboardResult.ExerciseRecord value) {
        return new DashboardView.ExerciseRecord(
                value.workoutDate(),
                value.volume(),
                value.maxWeightKg(),
                value.maxEstimatedOneRepMax());
    }
}
