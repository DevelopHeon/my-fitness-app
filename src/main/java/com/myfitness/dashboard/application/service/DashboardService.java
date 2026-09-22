package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.domain.service.DashboardCalculator;
import com.myfitness.dashboard.application.port.in.DashboardQueryUseCase;
import com.myfitness.dashboard.application.port.out.DashboardDataPort;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.BodyData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.ExerciseData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.SetData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.WorkoutData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.DashboardSourceData;
import com.myfitness.dashboard.application.result.DashboardResult;
import com.myfitness.dashboard.application.result.DashboardResult.BodyChange;
import com.myfitness.dashboard.application.result.DashboardResult.BodyPoint;
import com.myfitness.dashboard.application.result.DashboardResult.BodySummary;
import com.myfitness.dashboard.application.result.DashboardResult.CategoryDailyVolume;
import com.myfitness.dashboard.application.result.DashboardResult.DailyVolume;
import com.myfitness.dashboard.application.result.DashboardResult.ExerciseRecord;
import com.myfitness.dashboard.application.result.DashboardResult.ExerciseSummary;
import com.myfitness.dashboard.application.result.DashboardResult.WorkoutSummary;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService implements DashboardQueryUseCase {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final DashboardDataPort dashboardDataPort;
    private final Clock clock;

    @Autowired
    public DashboardService(DashboardDataPort dashboardDataPort) {
        this(dashboardDataPort, Clock.systemDefaultZone());
    }

    DashboardService(
            DashboardDataPort dashboardDataPort,
            Clock clock) {
        this.dashboardDataPort = dashboardDataPort;
        this.clock = clock;
    }

    public DashboardResult getDashboard(Long userId) {
        LocalDate today = LocalDate.now(clock);
        DashboardSourceData source = dashboardDataPort.load(userId);

        List<WorkoutData> completedWorkouts =
                source.completedWorkouts().stream()
                        .sorted(Comparator
                                .comparing(WorkoutData::getWorkoutDate)
                                .thenComparing(WorkoutData::getStartedAt))
                        .toList();

        Instant now = clock.instant();
        List<BodyData> bodyRecords =
                source.bodyRecords().stream()
                        .sorted(Comparator
                                .comparing(BodyData::getMeasuredAt)
                                .reversed())
                        .toList();

        return new DashboardResult(
                today,
                buildWorkoutSummary(completedWorkouts, today),
                buildBodySummary(
                        bodyRecords,
                        now.minus(90, ChronoUnit.DAYS)),
                buildExerciseSummaries(completedWorkouts));
    }

    private static WorkoutSummary buildWorkoutSummary(
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
                new LinkedHashMap<>();
        for (String category : List.of(
                "CHEST", "SHOULDER", "BACK", "ARM", "ABS", "LEGS")) {
            categoryDailyVolumes.put(category, new TreeMap<>());
        }

        for (int offset = 29; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            dailyVolumes.put(date, ZERO);
            categoryDailyVolumes.values()
                    .forEach(series -> series.put(date, ZERO));
        }

        for (WorkoutData workout : workouts) {
            if (!isBetween(workout.getWorkoutDate(), last30Start, today)) {
                continue;
            }

            dailyVolumes.computeIfPresent(
                    workout.getWorkoutDate(),
                    (date, volume) -> volume.add(workoutVolume(workout)));

            for (String category : categoryDailyVolumes.keySet()) {
                Map<LocalDate, BigDecimal> series =
                        categoryDailyVolumes.get(category);
                series.computeIfPresent(
                        workout.getWorkoutDate(),
                        (date, volume) -> volume.add(
                                workoutVolumeByCategory(
                                        workout,
                                        category)));
            }
        }

        List<CategoryDailyVolume> categorySeries =
                categoryDailyVolumes.entrySet().stream()
                        .map(entry -> new CategoryDailyVolume(
                                entry.getKey(),
                                entry.getValue().entrySet().stream()
                                        .map(day -> new DailyVolume(
                                                day.getKey(),
                                                day.getValue()))
                                        .toList()))
                        .toList();

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
                dailyVolumes.entrySet().stream()
                        .map(entry -> new DailyVolume(
                                entry.getKey(),
                                entry.getValue()))
                        .toList(),
                categorySeries);
    }

    private static BodySummary buildBodySummary(
            List<BodyData> records,
            Instant historyStart) {
        if (records.isEmpty()) {
            return new BodySummary(null, null, List.of());
        }

        List<BodyData> orderedByMeasuredAtDesc = records.stream()
                .sorted(Comparator
                        .comparing(BodyData::getMeasuredAt)
                        .reversed())
                .toList();

        BodyData latest = orderedByMeasuredAtDesc.getFirst();
        BodyChange change = orderedByMeasuredAtDesc.size() < 2
                ? null
                : new BodyChange(
                        latest.getWeightKg().subtract(
                                orderedByMeasuredAtDesc.get(1).getWeightKg()),
                        latest.getBodyFatPercentage().subtract(
                                orderedByMeasuredAtDesc.get(1)
                                        .getBodyFatPercentage()),
                        latest.getSkeletalMuscleKg().subtract(
                                orderedByMeasuredAtDesc.get(1)
                                        .getSkeletalMuscleKg()));

        List<BodyPoint> history = orderedByMeasuredAtDesc.stream()
                .filter(record ->
                        !record.getMeasuredAt().isBefore(historyStart))
                .sorted(Comparator.comparing(BodyData::getMeasuredAt))
                .map(DashboardService::bodyPoint)
                .toList();

        return new BodySummary(
                bodyPoint(latest),
                change,
                history);
    }

    private static List<ExerciseSummary> buildExerciseSummaries(
            List<WorkoutData> workouts) {
        Map<String, ExerciseAccumulator> accumulators =
                new LinkedHashMap<>();

        for (WorkoutData workout : workouts) {
            for (ExerciseData entry : workout.getExercises()) {
                List<SetData> completedSets = entry.getSets().stream()
                        .filter(SetData::isCompleted)
                        .filter(set -> set.getReps() > 0)
                        .toList();
                if (completedSets.isEmpty()) {
                    continue;
                }

                String key = entry.getExerciseType() + ":" + entry.getExerciseId();
                ExerciseAccumulator accumulator =
                        accumulators.computeIfAbsent(
                                key,
                                ignored -> new ExerciseAccumulator(entry));

                BigDecimal recordVolume = ZERO;
                BigDecimal recordMaxWeight = ZERO;
                BigDecimal recordMaxOneRepMax = ZERO;

                for (SetData set : completedSets) {
                    BigDecimal setVolume = DashboardCalculator.volume(
                            set.getWeightKg(),
                            set.getReps());
                    BigDecimal oneRepMax =
                            DashboardCalculator.estimatedOneRepMax(
                                    set.getWeightKg(),
                                    set.getReps());

                    recordVolume = recordVolume.add(setVolume);
                    recordMaxWeight =
                            recordMaxWeight.max(set.getWeightKg());
                    recordMaxOneRepMax =
                            recordMaxOneRepMax.max(oneRepMax);

                    accumulator.maxWeight =
                            accumulator.maxWeight.max(set.getWeightKg());
                    accumulator.maxOneRepMax =
                            accumulator.maxOneRepMax.max(oneRepMax);
                }

                accumulator.exerciseName = entry.getExerciseName();
                accumulator.category = entry.getCategory();
                if (accumulator.latestWorkoutDate == null
                        || workout.getWorkoutDate()
                                .isAfter(accumulator.latestWorkoutDate)) {
                    accumulator.latestWorkoutDate = workout.getWorkoutDate();
                }
                accumulator.records.add(new ExerciseRecord(
                        workout.getWorkoutDate(),
                        recordVolume,
                        recordMaxWeight,
                        recordMaxOneRepMax));
            }
        }

        return accumulators.values().stream()
                .map(ExerciseAccumulator::toResponse)
                .sorted(Comparator
                        .comparing(
                                ExerciseSummary::latestWorkoutDate,
                                Comparator.reverseOrder())
                        .thenComparing(ExerciseSummary::exerciseName))
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
                .map(DashboardService::workoutVolume)
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

    private static BodyPoint bodyPoint(BodyData record) {
        return new BodyPoint(
                record.getMeasuredAt(),
                record.getWeightKg(),
                record.getBodyFatPercentage(),
                record.getSkeletalMuscleKg());
    }

    private static final class ExerciseAccumulator {
        private final String exerciseType;
        private final Long exerciseId;
        private String exerciseName;
        private String category;
        private BigDecimal maxWeight = ZERO;
        private BigDecimal maxOneRepMax = ZERO;
        private LocalDate latestWorkoutDate;
        private final List<ExerciseRecord> records = new ArrayList<>();

        private ExerciseAccumulator(ExerciseData entry) {
            this.exerciseType = entry.getExerciseType();
            this.exerciseId = entry.getExerciseId();
            this.exerciseName = entry.getExerciseName();
            this.category = entry.getCategory();
        }

        private ExerciseSummary toResponse() {
            List<ExerciseRecord> orderedRecords = records.stream()
                    .sorted(Comparator.comparing(
                            ExerciseRecord::workoutDate))
                    .toList();
            int fromIndex = Math.max(0, orderedRecords.size() - 8);
            return new ExerciseSummary(
                    exerciseType,
                    exerciseId,
                    exerciseName,
                    category,
                    maxWeight,
                    maxOneRepMax,
                    latestWorkoutDate,
                    List.copyOf(orderedRecords.subList(
                            fromIndex,
                            orderedRecords.size())));
        }
    }
}
