package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.port.out.DashboardDataPort.ExerciseData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.SetData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.WorkoutData;
import com.myfitness.dashboard.application.result.DashboardResult.ExerciseRecord;
import com.myfitness.dashboard.application.result.DashboardResult.ExerciseSummary;
import com.myfitness.dashboard.domain.service.DashboardCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DashboardExerciseSummaryBuilder {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    List<ExerciseSummary> build(List<WorkoutData> workouts) {
        Map<String, ExerciseAccumulator> accumulators =
                new LinkedHashMap<>();

        for (WorkoutData workout : workouts) {
            for (ExerciseData entry : workout.getExercises()) {
                accumulateWorkoutExercise(
                        workout,
                        entry,
                        accumulators);
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

    private static void accumulateWorkoutExercise(
            WorkoutData workout,
            ExerciseData entry,
            Map<String, ExerciseAccumulator> accumulators) {
        List<SetData> completedSets = entry.getSets().stream()
                .filter(SetData::isCompleted)
                .filter(set -> set.getReps() > 0)
                .toList();
        if (completedSets.isEmpty()) {
            return;
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

        accumulator.updateSnapshot(
                workout,
                entry,
                recordVolume,
                recordMaxWeight,
                recordMaxOneRepMax);
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

        private void updateSnapshot(
                WorkoutData workout,
                ExerciseData entry,
                BigDecimal recordVolume,
                BigDecimal recordMaxWeight,
                BigDecimal recordMaxOneRepMax) {
            exerciseName = entry.getExerciseName();
            category = entry.getCategory();
            if (latestWorkoutDate == null
                    || workout.getWorkoutDate()
                            .isAfter(latestWorkoutDate)) {
                latestWorkoutDate = workout.getWorkoutDate();
            }
            records.add(new ExerciseRecord(
                    workout.getWorkoutDate(),
                    recordVolume,
                    recordMaxWeight,
                    recordMaxOneRepMax));
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
