package com.myfitness.ai.application.context;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.application.context.AiContextSelector.AiContextArea;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery.BodyInsight;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.MacroInsight;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.NutritionDayInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.ExerciseInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.SetInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.WorkoutInsight;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiContextBuilder {
    private static final BigDecimal THIRTY = new BigDecimal("30");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String NL = System.lineSeparator();

    private final WorkoutInsightQuery workoutInsightQuery;
    private final BodyInsightQuery bodyInsightQuery;
    private final NutritionInsightQuery nutritionInsightQuery;
    private final AiContextSelector contextSelector;
    private final Clock clock;

    @Autowired
    public AiContextBuilder(
            WorkoutInsightQuery workoutInsightQuery,
            BodyInsightQuery bodyInsightQuery,
            NutritionInsightQuery nutritionInsightQuery,
            AiContextSelector contextSelector) {
        this(
                workoutInsightQuery,
                bodyInsightQuery,
                nutritionInsightQuery,
                contextSelector,
                Clock.systemDefaultZone());
    }

    AiContextBuilder(
            WorkoutInsightQuery workoutInsightQuery,
            BodyInsightQuery bodyInsightQuery,
            NutritionInsightQuery nutritionInsightQuery,
            AiContextSelector contextSelector,
            Clock clock) {
        this.workoutInsightQuery = workoutInsightQuery;
        this.bodyInsightQuery = bodyInsightQuery;
        this.nutritionInsightQuery = nutritionInsightQuery;
        this.contextSelector = contextSelector;
        this.clock = clock;
    }

    public AiContextBundle build(
            Long userId,
            AiQueryType queryType,
            AiClientContext clientContext,
            String question) {
        Set<AiContextArea> areas = contextSelector.select(
                queryType,
                clientContext,
                question);
        if (areas.isEmpty()) {
            return AiContextBundle.empty();
        }

        List<String> sections = new ArrayList<>();
        Set<AiContextType> types = new LinkedHashSet<>();

        if (areas.contains(AiContextArea.WORKOUT)) {
            sections.add(workoutContext(userId, question, types));
        }
        if (areas.contains(AiContextArea.BODY)) {
            sections.add(bodyContext(userId, types));
        }
        if (areas.contains(AiContextArea.NUTRITION)) {
            sections.add(nutritionContext(userId, clientContext, types));
        }

        return new AiContextBundle(
                String.join(
                        NL + NL,
                        sections.stream()
                                .filter(section -> !section.isBlank())
                                .toList()),
                List.copyOf(types));
    }

    private String workoutContext(
            Long userId,
            String question,
            Set<AiContextType> types) {
        List<WorkoutInsight> workouts =
                workoutInsightQuery.findCompletedWorkouts(userId).stream()
                        .sorted(Comparator
                                .comparing(WorkoutInsight::workoutDate)
                                .thenComparing(WorkoutInsight::startedAt)
                                .reversed())
                        .toList();

        types.add(AiContextType.WORKOUT_SUMMARY);
        if (workouts.isEmpty()) {
            return "[운동 기록]" + NL + "- 완료된 운동 기록 없음";
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate last7Start = today.minusDays(6);
        LocalDate previous7Start = today.minusDays(13);
        LocalDate previous7End = today.minusDays(7);
        LocalDate last30Start = today.minusDays(29);

        long last7Count = countBetween(workouts, last7Start, today);
        long last30Count = countBetween(workouts, last30Start, today);
        BigDecimal last7Volume = volumeBetween(workouts, last7Start, today);
        BigDecimal previous7Volume =
                volumeBetween(workouts, previous7Start, previous7End);

        StringBuilder builder = new StringBuilder();
        builder.append("[운동 기록]").append(NL)
                .append("- 최근 7일 운동 횟수: ")
                .append(last7Count).append("회").append(NL)
                .append("- 최근 30일 운동 횟수: ")
                .append(last30Count).append("회").append(NL)
                .append("- 최근 7일 Volume: ")
                .append(number(last7Volume)).append(NL)
                .append("- 직전 7일 Volume: ")
                .append(number(previous7Volume)).append(NL);

        BigDecimal change = changePercentage(last7Volume, previous7Volume);
        if (change != null) {
            builder.append("- 직전 7일 대비 Volume 변화: ")
                    .append(number(change))
                    .append("%")
                    .append(NL);
        }

        builder.append("- 최근 운동:").append(NL);
        workouts.stream().limit(3).forEach(workout -> {
            String names = workout.exercises().stream()
                    .map(ExerciseInsight::exerciseName)
                    .distinct()
                    .limit(6)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("운동 종목 없음");
            builder.append("  - ")
                    .append(workout.workoutDate())
                    .append(": ")
                    .append(names)
                    .append(NL);
        });

        String exerciseName = findExerciseName(workouts, question);
        if (exerciseName != null) {
            types.add(AiContextType.EXERCISE_HISTORY);
            builder.append("- 질문 관련 종목: ")
                    .append(exerciseName)
                    .append(NL);

            List<ExerciseRecord> records = workouts.stream()
                    .flatMap(workout -> workout.exercises().stream()
                            .filter(exercise ->
                                    exercise.exerciseName()
                                            .equals(exerciseName))
                            .map(exercise -> new ExerciseRecord(
                                    workout.workoutDate(),
                                    exercise)))
                    .limit(5)
                    .toList();

            BigDecimal maxWeight = ZERO;
            BigDecimal maxOneRepMax = ZERO;
            for (ExerciseRecord record : records) {
                BigDecimal recordMaxWeight = record.exercise().sets().stream()
                        .filter(SetInsight::completed)
                        .filter(set -> set.reps() > 0)
                        .map(SetInsight::weightKg)
                        .filter(weight -> weight != null)
                        .max(BigDecimal::compareTo)
                        .orElse(ZERO);
                BigDecimal recordMaxOneRepMax = record.exercise().sets().stream()
                        .filter(SetInsight::completed)
                        .map(set -> estimatedOneRepMax(
                                set.weightKg(), set.reps()))
                        .max(BigDecimal::compareTo)
                        .orElse(ZERO);
                maxWeight = maxWeight.max(recordMaxWeight);
                maxOneRepMax = maxOneRepMax.max(recordMaxOneRepMax);
            }

            builder.append("  - 최근 기록 기준 최고 중량: ")
                    .append(number(maxWeight))
                    .append("kg")
                    .append(NL)
                    .append("  - Epley 추정 1RM 최고: ")
                    .append(number(maxOneRepMax))
                    .append("kg")
                    .append(NL);
            records.forEach(record -> builder.append("  - ")
                    .append(record.date())
                    .append(": ")
                    .append(setSummary(record.exercise().sets()))
                    .append(NL));
        }

        return builder.toString().trim();
    }

    private String bodyContext(
            Long userId,
            Set<AiContextType> types) {
        types.add(AiContextType.BODY_TREND);
        List<BodyInsight> records = bodyInsightQuery.findAll(userId).stream()
                .sorted(Comparator
                        .comparing(BodyInsight::measuredAt)
                        .reversed())
                .toList();

        if (records.isEmpty()) {
            return "[신체 기록]" + NL + "- 신체 기록 없음";
        }

        BodyInsight latest = records.getFirst();
        BodyInsight previous = records.size() > 1 ? records.get(1) : null;
        LocalDate latestDate = LocalDate.ofInstant(
                latest.measuredAt(), clock.getZone());

        StringBuilder builder = new StringBuilder()
                .append("[신체 기록]").append(NL)
                .append("- 최신 측정일: ")
                .append(latestDate).append(NL)
                .append("- 체중: ")
                .append(number(latest.weightKg())).append("kg").append(NL)
                .append("- 체지방률: ")
                .append(number(latest.bodyFatPercentage())).append("%").append(NL)
                .append("- 골격근량: ")
                .append(number(latest.skeletalMuscleKg())).append("kg").append(NL);

        if (previous != null) {
            builder.append("- 직전 기록 대비: 체중 ")
                    .append(signed(latest.weightKg()
                            .subtract(previous.weightKg())))
                    .append("kg, 체지방률 ")
                    .append(signed(latest.bodyFatPercentage()
                            .subtract(previous.bodyFatPercentage())))
                    .append("%, 골격근량 ")
                    .append(signed(latest.skeletalMuscleKg()
                            .subtract(previous.skeletalMuscleKg())))
                    .append("kg");
        }

        return builder.toString().trim();
    }

    private String nutritionContext(
            Long userId,
            AiClientContext clientContext,
            Set<AiContextType> types) {
        LocalDate date = clientContext != null
                && clientContext.selectedDate() != null
                ? clientContext.selectedDate()
                : LocalDate.now(clock);

        NutritionDayInsight day = nutritionInsightQuery.getDay(userId, date);
        types.add(AiContextType.NUTRITION_DAY);

        StringBuilder builder = new StringBuilder()
                .append("[영양 기록 ")
                .append(day.date())
                .append("]")
                .append(NL)
                .append("- 섭취: ")
                .append(macro(day.consumed()))
                .append(NL);

        if (day.goal() != null) {
            types.add(AiContextType.NUTRITION_GOAL);
            builder.append("- 목표: ")
                    .append(macro(day.goal()))
                    .append(NL)
                    .append("- 남은 목표: ")
                    .append(macro(day.remaining()))
                    .append(NL);
        } else {
            builder.append("- 영양 목표 미등록").append(NL);
        }

        if (!day.recentFoods().isEmpty()) {
            builder.append("- 최근 음식: ")
                    .append(String.join(", ", day.recentFoods()))
                    .append(NL);
        }
        if (!day.frequentFoods().isEmpty()) {
            builder.append("- 자주 먹는 음식: ")
                    .append(String.join(", ", day.frequentFoods()));
        }

        return builder.toString().trim();
    }

    private static long countBetween(
            List<WorkoutInsight> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout -> between(
                        workout.workoutDate(), from, to))
                .count();
    }

    private static BigDecimal volumeBetween(
            List<WorkoutInsight> workouts,
            LocalDate from,
            LocalDate to) {
        return workouts.stream()
                .filter(workout -> between(
                        workout.workoutDate(), from, to))
                .map(AiContextBuilder::workoutVolume)
                .reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal workoutVolume(WorkoutInsight workout) {
        return workout.exercises().stream()
                .flatMap(exercise -> exercise.sets().stream())
                .filter(SetInsight::completed)
                .filter(set -> set.reps() > 0)
                .map(set -> {
                    BigDecimal weight =
                            set.weightKg() == null ? ZERO : set.weightKg();
                    return weight.multiply(BigDecimal.valueOf(set.reps()));
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

    private static String macro(MacroInsight macro) {
        if (macro == null) {
            return "정보 없음";
        }
        return "칼로리 " + number(macro.calories())
                + "kcal / 탄수 " + number(macro.carbohydrateGrams())
                + "g / 단백질 " + number(macro.proteinGrams())
                + "g / 지방 " + number(macro.fatGrams()) + "g";
    }

    private static String number(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signed(BigDecimal value) {
        if (value.signum() > 0) {
            return "+" + number(value);
        }
        return number(value);
    }

    private static boolean between(
            LocalDate date,
            LocalDate from,
            LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private record ExerciseRecord(
            LocalDate date,
            ExerciseInsight exercise) {}
}
