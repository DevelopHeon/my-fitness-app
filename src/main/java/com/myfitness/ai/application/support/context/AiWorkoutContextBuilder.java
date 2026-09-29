package com.myfitness.ai.application.support.context;

import static com.myfitness.ai.application.support.context.AiContextFormat.number;

import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.ExerciseInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.SetInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.WorkoutInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class AiWorkoutContextBuilder {
    private static final BigDecimal THIRTY = new BigDecimal("30");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String NL = System.lineSeparator();

    private final WorkoutInsightQuery workoutInsightQuery;
    private final Clock clock;

    @Autowired
    AiWorkoutContextBuilder(WorkoutInsightQuery workoutInsightQuery) {
        this(workoutInsightQuery, Clock.systemDefaultZone());
    }

    AiWorkoutContextBuilder(
            WorkoutInsightQuery workoutInsightQuery,
            Clock clock) {
        this.workoutInsightQuery = workoutInsightQuery;
        this.clock = clock;
    }

    AiContextSection build(Long userId, String question) {
        LocalDate today = LocalDate.now(clock);
        WorkoutPeriod period = WorkoutPeriod.from(today);

        List<WorkoutInsight> workouts = recentWorkouts(
                userId,
                period.last30Start());

        if (workouts.isEmpty()) {
            return AiContextSection.of(
                    "[운동 기록]" + NL + "- 최근 30일 완료 운동 기록 없음",
                    AiContextType.WORKOUT_SUMMARY);
        }

        StringBuilder text = buildSummary(workouts, period);
        String exerciseName = findExerciseName(workouts, question);
        if (exerciseName == null) {
            return AiContextSection.of(
                    text.toString().trim(),
                    AiContextType.WORKOUT_SUMMARY);
        }

        appendExerciseHistory(text, workouts, exerciseName);
        return AiContextSection.of(
                text.toString().trim(),
                AiContextType.WORKOUT_SUMMARY,
                AiContextType.EXERCISE_HISTORY);
    }

    private List<WorkoutInsight> recentWorkouts(
            Long userId,
            LocalDate from) {
        return workoutInsightQuery.findCompletedSince(userId, from)
                .stream()
                .sorted(Comparator
                        .comparing(WorkoutInsight::workoutDate)
                        .thenComparing(WorkoutInsight::startedAt)
                        .reversed())
                .toList();
    }

    private static StringBuilder buildSummary(
            List<WorkoutInsight> workouts,
            WorkoutPeriod period) {
        long last7Count = countBetween(
                workouts,
                period.last7Start(),
                period.today());
        long last30Count = countBetween(
                workouts,
                period.last30Start(),
                period.today());
        BigDecimal last7Volume = volumeBetween(
                workouts,
                period.last7Start(),
                period.today());
        BigDecimal previous7Volume = volumeBetween(
                workouts,
                period.previous7Start(),
                period.previous7End());

        StringBuilder text = new StringBuilder()
                .append("[운동 기록]").append(NL)
                .append("- 최근 7일 운동 횟수: ")
                .append(last7Count).append("회").append(NL)
                .append("- 최근 30일 운동 횟수: ")
                .append(last30Count).append("회").append(NL)
                .append("- 최근 7일 Volume: ")
                .append(number(last7Volume)).append(NL)
                .append("- 직전 7일 Volume: ")
                .append(number(previous7Volume)).append(NL);

        BigDecimal change = changePercentage(
                last7Volume,
                previous7Volume);
        if (change != null) {
            text.append("- 직전 7일 대비 Volume 변화: ")
                    .append(number(change))
                    .append("%")
                    .append(NL);
        }

        appendRecentWorkouts(text, workouts);
        return text;
    }

    private static void appendRecentWorkouts(
            StringBuilder text,
            List<WorkoutInsight> workouts) {
        text.append("- 최근 운동:").append(NL);
        workouts.stream().limit(3).forEach(workout -> {
            String names = workout.exercises().stream()
                    .map(ExerciseInsight::exerciseName)
                    .distinct()
                    .limit(6)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("운동 종목 없음");
            text.append("  - ")
                    .append(workout.workoutDate())
                    .append(": ")
                    .append(names)
                    .append(NL);
        });
    }

    private static void appendExerciseHistory(
            StringBuilder text,
            List<WorkoutInsight> workouts,
            String exerciseName) {
        List<ExerciseRecord> records =
                findExerciseRecords(workouts, exerciseName);

        BigDecimal maxWeight = ZERO;
        BigDecimal maxOneRepMax = ZERO;
        for (ExerciseRecord record : records) {
            maxWeight = maxWeight.max(
                    maxCompletedWeight(record.exercise()));
            maxOneRepMax = maxOneRepMax.max(
                    maxEstimatedOneRepMax(record.exercise()));
        }

        text.append("- 질문 관련 종목: ")
                .append(exerciseName)
                .append(NL)
                .append("  - 최근 기록 기준 최고 중량: ")
                .append(number(maxWeight))
                .append("kg")
                .append(NL)
                .append("  - Epley 추정 1RM 최고: ")
                .append(number(maxOneRepMax))
                .append("kg")
                .append(NL);

        records.forEach(record -> text.append("  - ")
                .append(record.date())
                .append(": ")
                .append(setSummary(record.exercise().sets()))
                .append(NL));
    }

    private static List<ExerciseRecord> findExerciseRecords(
            List<WorkoutInsight> workouts,
            String exerciseName) {
        return workouts.stream()
                .flatMap(workout -> workout.exercises().stream()
                        .filter(exercise ->
                                exercise.exerciseName()
                                        .equals(exerciseName))
                        .map(exercise -> new ExerciseRecord(
                                workout.workoutDate(),
                                exercise)))
                .limit(5)
                .toList();
    }

    private static BigDecimal maxCompletedWeight(
            ExerciseInsight exercise) {
        return exercise.sets().stream()
                .filter(SetInsight::completed)
                .filter(set -> set.reps() > 0)
                .map(SetInsight::weightKg)
                .filter(weight -> weight != null)
                .max(BigDecimal::compareTo)
                .orElse(ZERO);
    }

    private static BigDecimal maxEstimatedOneRepMax(
            ExerciseInsight exercise) {
        return exercise.sets().stream()
                .filter(SetInsight::completed)
                .map(set -> estimatedOneRepMax(
                        set.weightKg(),
                        set.reps()))
                .max(BigDecimal::compareTo)
                .orElse(ZERO);
    }

    private static long countBetween(
            List<WorkoutInsight> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout ->
                        between(workout.workoutDate(), from, to))
                .count();
    }

    private static BigDecimal volumeBetween(
            List<WorkoutInsight> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout ->
                        between(workout.workoutDate(), from, to))
                .map(AiWorkoutContextBuilder::workoutVolume)
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolume(
            WorkoutInsight workout) {
        return workout.exercises().stream()
                .flatMap(exercise -> exercise.sets().stream())
                .filter(SetInsight::completed)
                .filter(set -> set.reps() > 0)
                .map(set -> {
                    BigDecimal weight =
                            set.weightKg() == null ? ZERO : set.weightKg();
                    return weight.multiply(
                            BigDecimal.valueOf(set.reps()));
                })
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal changePercentage(
            BigDecimal current,
            BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal estimatedOneRepMax(
            BigDecimal weight,
            int reps) {
        if (weight == null || weight.signum() <= 0 || reps <= 0) {
            return ZERO;
        }
        return weight.multiply(
                        BigDecimal.ONE.add(
                                BigDecimal.valueOf(reps)
                                        .divide(
                                                THIRTY,
                                                8,
                                                RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String findExerciseName(
            List<WorkoutInsight> workouts,
            String question) {
        if (question == null) {
            return null;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        return workouts.stream()
                .flatMap(workout -> workout.exercises().stream())
                .map(ExerciseInsight::exerciseName)
                .filter(name -> normalized.contains(
                        name.toLowerCase(Locale.ROOT)))
                .distinct()
                .findFirst()
                .orElse(null);
    }

    private static String setSummary(List<SetInsight> sets) {
        return sets.stream()
                .filter(SetInsight::completed)
                .limit(5)
                .map(set -> number(set.weightKg())
                        + "kg x " + set.reps())
                .reduce((a, b) -> a + ", " + b)
                .orElse("완료 세트 없음");
    }

    private static boolean between(
            LocalDate date,
            LocalDate from,
            LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private record WorkoutPeriod(
            LocalDate today,
            LocalDate last7Start,
            LocalDate previous7Start,
            LocalDate previous7End,
            LocalDate last30Start
    ) {
        static WorkoutPeriod from(LocalDate today) {
            return new WorkoutPeriod(
                    today,
                    today.minusDays(6),
                    today.minusDays(13),
                    today.minusDays(7),
                    today.minusDays(29));
        }
    }

    private record ExerciseRecord(
            LocalDate date,
            ExerciseInsight exercise) {}
}
