package com.myfitness.ai.application.context;

import static com.myfitness.ai.application.context.AiContextFormat.macro;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.NutritionDayInsight;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class AiNutritionContextBuilder {
    private static final String NL = System.lineSeparator();

    private final NutritionInsightQuery nutritionInsightQuery;
    private final Clock clock;

    @Autowired
    AiNutritionContextBuilder(
            NutritionInsightQuery nutritionInsightQuery) {
        this(
                nutritionInsightQuery,
                Clock.systemDefaultZone());
    }

    AiNutritionContextBuilder(
            NutritionInsightQuery nutritionInsightQuery,
            Clock clock) {
        this.nutritionInsightQuery = nutritionInsightQuery;
        this.clock = clock;
    }

    AiContextSection build(
            Long userId,
            AiClientContext clientContext) {
        LocalDate date = selectedDate(clientContext);
        NutritionDayInsight day =
                nutritionInsightQuery.getDay(userId, date);

        List<AiContextType> types = new ArrayList<>();
        types.add(AiContextType.NUTRITION_DAY);

        StringBuilder text = new StringBuilder()
                .append("[영양 기록 ")
                .append(day.date())
                .append("]")
                .append(NL)
                .append("- 섭취: ")
                .append(macro(day.consumed()))
                .append(NL);

        appendGoal(text, day, types);
        appendFoods(text, day);

        return new AiContextSection(
                text.toString().trim(),
                List.copyOf(types));
    }

    private LocalDate selectedDate(
            AiClientContext clientContext) {
        if (clientContext != null
                && clientContext.selectedDate() != null) {
            return clientContext.selectedDate();
        }
        return LocalDate.now(clock);
    }

    private static void appendGoal(
            StringBuilder text,
            NutritionDayInsight day,
            List<AiContextType> types) {
        if (day.goal() == null) {
            text.append("- 영양 목표 미등록").append(NL);
            return;
        }

        types.add(AiContextType.NUTRITION_GOAL);
        text.append("- 목표: ")
                .append(macro(day.goal()))
                .append(NL)
                .append("- 남은 목표: ")
                .append(macro(day.remaining()))
                .append(NL);
    }

    private static void appendFoods(
            StringBuilder text,
            NutritionDayInsight day) {
        if (!day.recentFoods().isEmpty()) {
            text.append("- 최근 음식: ")
                    .append(String.join(", ", day.recentFoods()))
                    .append(NL);
        }
        if (!day.frequentFoods().isEmpty()) {
            text.append("- 자주 먹는 음식: ")
                    .append(String.join(", ", day.frequentFoods()));
        }
    }
}
