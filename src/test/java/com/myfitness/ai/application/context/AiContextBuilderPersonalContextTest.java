package com.myfitness.ai.application.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery.BodyInsight;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.MacroInsight;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.NutritionDayInsight;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiContextBuilderPersonalContextTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-22T03:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("Body Context는 최신 신체 기록과 직전 기록 대비 변화를 제공한다")
    void buildsBodyContextWithLatestChange() {
        WorkoutInsightQuery workout = mock(WorkoutInsightQuery.class);
        BodyInsightQuery body = mock(BodyInsightQuery.class);
        NutritionInsightQuery nutrition = mock(NutritionInsightQuery.class);

        when(body.findRecent(1L, 2)).thenReturn(List.of(
                new BodyInsight(
                        2L,
                        Instant.parse("2026-09-21T03:00:00Z"),
                        new BigDecimal("71.0"),
                        new BigDecimal("17.5"),
                        new BigDecimal("34.5")),
                new BodyInsight(
                        1L,
                        Instant.parse("2026-09-16T03:00:00Z"),
                        new BigDecimal("72.0"),
                        new BigDecimal("18.0"),
                        new BigDecimal("34.0"))));

        AiContextBuilder builder = new AiContextBuilder(
                new AiContextSelector(),
                new AiWorkoutContextBuilder(workout, CLOCK),
                new AiBodyContextBuilder(body, CLOCK),
                new AiNutritionContextBuilder(nutrition, CLOCK));

        AiContextBundle context = builder.build(
                1L,
                AiQueryType.BODY,
                new AiClientContext("BODY", null, null),
                "최근 체중 변화를 알려줘");

        assertThat(context.text())
                .contains("[신체 기록]")
                .contains("체중: 71kg")
                .contains("체지방률: 17.5%")
                .contains("골격근량: 34.5kg")
                .contains("직전 기록 대비: 체중 -1kg")
                .contains("체지방률 -0.5%")
                .contains("골격근량 +0.5kg");
        assertThat(context.types())
                .containsExactly(AiContextType.BODY_TREND);
    }

    @Test
    @DisplayName("Body Context는 동일 측정 일시면 높은 ID를 더 최신 기록으로 판단한다")
    void prefersHigherIdWhenBodyMeasurementTimeIsEqual() {
        WorkoutInsightQuery workout = mock(WorkoutInsightQuery.class);
        BodyInsightQuery body = mock(BodyInsightQuery.class);
        NutritionInsightQuery nutrition = mock(NutritionInsightQuery.class);
        Instant measuredAt = Instant.parse("2026-09-21T03:00:00Z");

        when(body.findRecent(1L, 2)).thenReturn(List.of(
                new BodyInsight(
                        1L,
                        measuredAt,
                        new BigDecimal("64.0"),
                        new BigDecimal("15.0"),
                        new BigDecimal("31.0")),
                new BodyInsight(
                        2L,
                        measuredAt,
                        new BigDecimal("65.0"),
                        new BigDecimal("18.0"),
                        new BigDecimal("32.0"))));

        AiContextBuilder builder = new AiContextBuilder(
                new AiContextSelector(),
                new AiWorkoutContextBuilder(workout, CLOCK),
                new AiBodyContextBuilder(body, CLOCK),
                new AiNutritionContextBuilder(nutrition, CLOCK));

        AiContextBundle context = builder.build(
                1L,
                AiQueryType.BODY,
                new AiClientContext("BODY", null, null),
                "최근 신체 변화 알려줘");

        assertThat(context.text())
                .contains("체중: 65kg")
                .contains("체지방률: 18%")
                .contains("골격근량: 32kg")
                .contains("직전 기록 대비: 체중 +1kg")
                .contains("체지방률 +3%")
                .contains("골격근량 +1kg");
    }

    @Test
    @DisplayName("Nutrition Context는 화면에서 선택한 날짜의 섭취량과 남은 목표를 조회한다")
    void buildsNutritionContextForSelectedDate() {
        WorkoutInsightQuery workout = mock(WorkoutInsightQuery.class);
        BodyInsightQuery body = mock(BodyInsightQuery.class);
        NutritionInsightQuery nutrition = mock(NutritionInsightQuery.class);
        LocalDate selectedDate = LocalDate.of(2026, 9, 18);

        MacroInsight goal = new MacroInsight(
                new BigDecimal("2200"),
                new BigDecimal("250"),
                new BigDecimal("160"),
                new BigDecimal("65"));
        MacroInsight consumed = new MacroInsight(
                new BigDecimal("1600"),
                new BigDecimal("180"),
                new BigDecimal("110"),
                new BigDecimal("45"));
        MacroInsight remaining = new MacroInsight(
                new BigDecimal("600"),
                new BigDecimal("70"),
                new BigDecimal("50"),
                new BigDecimal("20"));

        when(nutrition.getDay(1L, selectedDate))
                .thenReturn(new NutritionDayInsight(
                        selectedDate,
                        goal,
                        consumed,
                        remaining,
                        List.of("현미밥", "닭가슴살"),
                        List.of("계란")));

        AiContextBuilder builder = new AiContextBuilder(
                new AiContextSelector(),
                new AiWorkoutContextBuilder(workout, CLOCK),
                new AiBodyContextBuilder(body, CLOCK),
                new AiNutritionContextBuilder(nutrition, CLOCK));

        AiContextBundle context = builder.build(
                1L,
                AiQueryType.NUTRITION,
                new AiClientContext("NUTRITION", selectedDate, null),
                "이날 식단 평가해줘");

        verify(nutrition).getDay(1L, selectedDate);
        assertThat(context.text())
                .contains("[영양 기록 2026-09-18]")
                .contains("섭취: 칼로리 1600kcal")
                .contains("남은 목표: 칼로리 600kcal")
                .contains("최근 음식: 현미밥, 닭가슴살")
                .contains("자주 먹는 음식: 계란");
        assertThat(context.types())
                .containsExactly(
                        AiContextType.NUTRITION_DAY,
                        AiContextType.NUTRITION_GOAL);
    }
}
