package com.myfitness.ai.application.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.ExerciseInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.SetInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery.WorkoutInsight;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiContextBuilderTest {

    @Test
    @DisplayName("Workout Context는 기간 Volume과 질문 종목의 최근 중량 및 1RM을 Java에서 계산한다")
    void buildsWorkoutContextWithCalculatedMetrics() {
        WorkoutInsightQuery workout = mock(WorkoutInsightQuery.class);
        BodyInsightQuery body = mock(BodyInsightQuery.class);
        NutritionInsightQuery nutrition =
                mock(NutritionInsightQuery.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-22T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));

        when(workout.findCompletedSince(
                1L,
                LocalDate.of(2026, 8, 24))).thenReturn(List.of(
                workout(
                        LocalDate.of(2026, 9, 21),
                        "벤치프레스",
                        new BigDecimal("100"),
                        10),
                workout(
                        LocalDate.of(2026, 9, 14),
                        "벤치프레스",
                        new BigDecimal("80"),
                        10)));

        AiContextBuilder builder = new AiContextBuilder(
                workout,
                body,
                nutrition,
                new AiContextSelector(),
                clock);

        AiContextBundle context = builder.build(
                1L,
                AiQueryType.WORKOUT,
                null,
                "벤치프레스 다음 중량 추천해줘");

        assertThat(context.text())
                .contains("최근 7일 운동 횟수: 1회")
                .contains("최근 7일 Volume: 1000")
                .contains("직전 7일 Volume: 800")
                .contains("직전 7일 대비 Volume 변화: 25%")
                .contains("최근 기록 기준 최고 중량: 100kg")
                .contains("Epley 추정 1RM 최고: 133.33kg");
        assertThat(context.types())
                .containsExactly(
                        AiContextType.WORKOUT_SUMMARY,
                        AiContextType.EXERCISE_HISTORY);
    }

    private static WorkoutInsight workout(
            LocalDate date,
            String exerciseName,
            BigDecimal weight,
            int reps) {
        return new WorkoutInsight(
                date,
                date.atStartOfDay(ZoneId.of("Asia/Seoul"))
                        .toInstant(),
                List.of(new ExerciseInsight(
                        "DEFAULT",
                        1L,
                        exerciseName,
                        "CHEST",
                        List.of(new SetInsight(
                                weight,
                                reps,
                                true)))));
    }
}
