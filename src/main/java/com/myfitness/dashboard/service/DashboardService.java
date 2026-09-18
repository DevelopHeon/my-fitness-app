package com.myfitness.dashboard.service;

import com.myfitness.body.domain.BodyRecord;
import com.myfitness.body.repository.BodyRecordRepository;
import com.myfitness.dashboard.dto.response.DashboardResponse;
import com.myfitness.dashboard.dto.response.DashboardResponse.BodyChange;
import com.myfitness.dashboard.dto.response.DashboardResponse.BodyPoint;
import com.myfitness.dashboard.dto.response.DashboardResponse.BodySummary;
import com.myfitness.dashboard.dto.response.DashboardResponse.CategoryDailyVolume;
import com.myfitness.dashboard.dto.response.DashboardResponse.DailyVolume;
import com.myfitness.dashboard.dto.response.DashboardResponse.ExerciseRecord;
import com.myfitness.dashboard.dto.response.DashboardResponse.ExerciseSummary;
import com.myfitness.dashboard.dto.response.DashboardResponse.WorkoutSummary;
import com.myfitness.workout.domain.ExerciseCategory;
import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.domain.WorkoutSet;
import com.myfitness.workout.domain.WorkoutStatus;
import com.myfitness.workout.repository.WorkoutRepository;
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
public class DashboardService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final WorkoutRepository workoutRepository;
    private final BodyRecordRepository bodyRecordRepository;
    private final Clock clock;

    @Autowired
    public DashboardService(
            WorkoutRepository workoutRepository,
            BodyRecordRepository bodyRecordRepository) {
        this(
                workoutRepository,
                bodyRecordRepository,
                Clock.systemDefaultZone());
    }

    DashboardService(
            WorkoutRepository workoutRepository,
            BodyRecordRepository bodyRecordRepository,
            Clock clock) {
        this.workoutRepository = workoutRepository;
        this.bodyRecordRepository = bodyRecordRepository;
        this.clock = clock;
    }

    public DashboardResponse getDashboard(Long userId) {
        LocalDate today = LocalDate.now(clock);
        List<Workout> completedWorkouts =
                workoutRepository
                        .findAllByUserIdAndStatusOrderByWorkoutDateAscStartedAtAsc(
                                userId,
                                WorkoutStatus.COMPLETED)
                        .stream()
                        .sorted(Comparator
                                .comparing(Workout::getWorkoutDate)
                                .thenComparing(Workout::getStartedAt))
                        .toList();

        Instant now = clock.instant();
        List<BodyRecord> bodyRecords =
                bodyRecordRepository
                        .findAllByUserIdOrderByMeasuredAtDesc(userId)
                        .stream()
                        .sorted(Comparator
                                .comparing(BodyRecord::getMeasuredAt)
                                .reversed())
                        .toList();

        return new DashboardResponse(
                today,
                buildWorkoutSummary(completedWorkouts, today),
                buildBodySummary(
                        bodyRecords,
                        now.minus(90, ChronoUnit.DAYS)),
                buildExerciseSummaries(completedWorkouts));
    }

    private static WorkoutSummary buildWorkoutSummary(
            List<Workout> workouts,
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
        Map<ExerciseCategory, Map<LocalDate, BigDecimal>> categoryDailyVolumes =
                new LinkedHashMap<>();
        for (ExerciseCategory category : ExerciseCategory.values()) {
            categoryDailyVolumes.put(category, new TreeMap<>());
        }

        for (int offset = 29; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            dailyVolumes.put(date, ZERO);
            categoryDailyVolumes.values()
                    .forEach(series -> series.put(date, ZERO));
        }

        for (Workout workout : workouts) {
            if (!isBetween(workout.getWorkoutDate(), last30Start, today)) {
                continue;
            }

            dailyVolumes.computeIfPresent(
                    workout.getWorkoutDate(),
                    (date, volume) -> volume.add(workoutVolume(workout)));

            for (ExerciseCategory category : ExerciseCategory.values()) {
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
            List<BodyRecord> records,
            Instant historyStart) {
        if (records.isEmpty()) {
            return new BodySummary(null, null, List.of());
        }

        List<BodyRecord> orderedByMeasuredAtDesc = records.stream()
                .sorted(Comparator
                        .comparing(BodyRecord::getMeasuredAt)
                        .reversed())
                .toList();

        BodyRecord latest = orderedByMeasuredAtDesc.getFirst();
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
                .sorted(Comparator.comparing(BodyRecord::getMeasuredAt))
                .map(DashboardService::bodyPoint)
                .toList();

        return new BodySummary(
                bodyPoint(latest),
                change,
                history);
    }

    private static List<ExerciseSummary> buildExerciseSummaries(
            List<Workout> workouts) {
        Map<String, ExerciseAccumulator> accumulators =
                new LinkedHashMap<>();

        for (Workout workout : workouts) {
            for (WorkoutExercise entry : workout.getExercises()) {
                List<WorkoutSet> completedSets = entry.getSets().stream()
                        .filter(WorkoutSet::isCompleted)
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

                for (WorkoutSet set : completedSets) {
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
                accumulator.category = entry.getCategory().name();
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
            List<Workout> workouts,
            LocalDate from,
            LocalDate to) {
        return (int) workouts.stream()
                .filter(workout ->
                        isBetween(workout.getWorkoutDate(), from, to))
                .count();
    }

    private static BigDecimal volumeBetween(
            List<Workout> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout ->
                        isBetween(workout.getWorkoutDate(), from, to))
                .map(DashboardService::workoutVolume)
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolume(Workout workout) {
        return workout.getExercises().stream()
                .flatMap(entry -> entry.getSets().stream())
                .filter(WorkoutSet::isCompleted)
                .filter(set -> set.getReps() > 0)
                .map(set -> DashboardCalculator.volume(
                        set.getWeightKg(),
                        set.getReps()))
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolumeByCategory(
            Workout workout,
            ExerciseCategory category) {
        return workout.getExercises().stream()
                .filter(entry -> entry.getCategory() == category)
                .flatMap(entry -> entry.getSets().stream())
                .filter(WorkoutSet::isCompleted)
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

    private static BodyPoint bodyPoint(BodyRecord record) {
        return new BodyPoint(
                record.getMeasuredAt(),
                record.getWeightKg(),
                record.getBodyFatPercentage(),
                record.getSkeletalMuscleKg());
    }

    private static final class ExerciseAccumulator {
        private final com.myfitness.workout.domain.ExerciseType exerciseType;
        private final Long exerciseId;
        private String exerciseName;
        private String category;
        private BigDecimal maxWeight = ZERO;
        private BigDecimal maxOneRepMax = ZERO;
        private LocalDate latestWorkoutDate;
        private final List<ExerciseRecord> records = new ArrayList<>();

        private ExerciseAccumulator(WorkoutExercise entry) {
            this.exerciseType = entry.getExerciseType();
            this.exerciseId = entry.getExerciseId();
            this.exerciseName = entry.getExerciseName();
            this.category = entry.getCategory().name();
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
