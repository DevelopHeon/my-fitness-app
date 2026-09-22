package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.port.out.DashboardDataPort.SetData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.WorkoutData;
import com.myfitness.dashboard.application.result.DashboardResult.CategoryDailyVolume;
import com.myfitness.dashboard.application.result.DashboardResult.DailyVolume;
import com.myfitness.dashboard.application.result.DashboardResult.WorkoutSummary;
import com.myfitness.dashboard.domain.service.DashboardCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class DashboardWorkoutSummaryBuilder {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final List<String> CATEGORIES = List.of(
            "CHEST", "SHOULDER", "BACK", "ARM", "ABS", "LEGS");

    WorkoutSummary build(
            List<WorkoutData> workouts,
            LocalDate today) {
        LocalDate last7Start = today.minusDays(6);
        LocalDate previous7Start = today.minusDays(13);
        LocalDate previous7End = today.minusDays(7);
        LocalDate last30Start = today.minusDays(29);
        LocalDate previous30Start = today.minusDays(59);
        LocalDate previous30End = today.minusDays(30);

        BigDecimal last7Volume =
                volumeBetween(workouts, last7Start, today);
        BigDecimal previous7Volume =
                volumeBetween(workouts, previous7Start, previous7End);
        BigDecimal last30Volume =
                volumeBetween(workouts, last30Start, today);
        BigDecimal previous30Volume =
                volumeBetween(workouts, previous30Start, previous30End);

        Map<LocalDate, BigDecimal> dailyVolumes = new TreeMap<>();
        Map<String, Map<LocalDate, BigDecimal>> categoryDailyVolumes =
                initializeCategoryDailyVolumes(today, dailyVolumes);

        accumulateLast30Days(
                workouts,
                last30Start,
                today,
                dailyVolumes,
                categoryDailyVolumes);

        return new WorkoutSummary(
                countBetween(workouts, last7Start, today),
                countBetween(workouts, last30Start, today),
                last7Volume,
                previous7Volume,
                DashboardCalculator.changePercentage(
                        last7Volume,
                        previous7Volume),
                last30Volume,
                previous30Volume,
                DashboardCalculator.changePercentage(
                        last30Volume,
                        previous30Volume),
                toDailyVolumes(dailyVolumes),
                toCategoryDailyVolumes(categoryDailyVolumes));
    }

    private static Map<String, Map<LocalDate, BigDecimal>>
            initializeCategoryDailyVolumes(
                    LocalDate today,
                    Map<LocalDate, BigDecimal> dailyVolumes) {
        Map<String, Map<LocalDate, BigDecimal>> categoryDailyVolumes =
                new LinkedHashMap<>();
        CATEGORIES.forEach(category ->
                categoryDailyVolumes.put(category, new TreeMap<>()));

        for (int offset = 29; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            dailyVolumes.put(date, ZERO);
            categoryDailyVolumes.values()
                    .forEach(series -> series.put(date, ZERO));
        }
        return categoryDailyVolumes;
    }

    private static void accumulateLast30Days(
            List<WorkoutData> workouts,
            LocalDate from,
            LocalDate to,
            Map<LocalDate, BigDecimal> dailyVolumes,
            Map<String, Map<LocalDate, BigDecimal>> categoryDailyVolumes) {
        for (WorkoutData workout : workouts) {
            if (!isBetween(workout.getWorkoutDate(), from, to)) {
                continue;
            }

            dailyVolumes.computeIfPresent(
                    workout.getWorkoutDate(),
                    (date, volume) -> volume.add(workoutVolume(workout)));

            for (String category : CATEGORIES) {
                categoryDailyVolumes.get(category)
                        .computeIfPresent(
                                workout.getWorkoutDate(),
                                (date, volume) -> volume.add(
                                        workoutVolumeByCategory(
                                                workout,
                                                category)));
            }
        }
    }

    private static List<DailyVolume> toDailyVolumes(
            Map<LocalDate, BigDecimal> dailyVolumes) {
        return dailyVolumes.entrySet().stream()
                .map(entry -> new DailyVolume(
                        entry.getKey(),
                        entry.getValue()))
                .toList();
    }

    private static List<CategoryDailyVolume> toCategoryDailyVolumes(
            Map<String, Map<LocalDate, BigDecimal>> categoryDailyVolumes) {
        return categoryDailyVolumes.entrySet().stream()
                .map(entry -> new CategoryDailyVolume(
                        entry.getKey(),
                        toDailyVolumes(entry.getValue())))
                .toList();
    }

    private static int countBetween(
            List<WorkoutData> workouts,
            LocalDate from,
            LocalDate to) {
        return (int) workouts.stream()
                .filter(workout ->
                        isBetween(workout.getWorkoutDate(), from, to))
                .count();
    }

    private static BigDecimal volumeBetween(
            List<WorkoutData> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout ->
                        isBetween(workout.getWorkoutDate(), from, to))
                .map(DashboardWorkoutSummaryBuilder::workoutVolume)
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolume(WorkoutData workout) {
        return workout.getExercises().stream()
                .flatMap(entry -> entry.getSets().stream())
                .filter(SetData::isCompleted)
                .filter(set -> set.getReps() > 0)
                .map(set -> DashboardCalculator.volume(
                        set.getWeightKg(),
                        set.getReps()))
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolumeByCategory(
            WorkoutData workout,
            String category) {
        return workout.getExercises().stream()
                .filter(entry -> entry.getCategory().equals(category))
                .flatMap(entry -> entry.getSets().stream())
                .filter(SetData::isCompleted)
                .filter(set -> set.getReps() > 0)
                .map(set -> DashboardCalculator.volume(
                        set.getWeightKg(),
                        set.getReps()))
                .reduce(ZERO, BigDecimal::add);
    }

    private static boolean isBetween(
            LocalDate date,
            LocalDate from,
            LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
