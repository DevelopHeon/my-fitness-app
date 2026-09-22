package com.myfitness.ai.application.context;

import static com.myfitness.ai.application.context.AiContextFormat.number;
import static com.myfitness.ai.application.context.AiContextFormat.signed;

import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery.BodyInsight;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class AiBodyContextBuilder {
    private static final int RECENT_LIMIT = 2;
    private static final String NL = System.lineSeparator();

    private final BodyInsightQuery bodyInsightQuery;
    private final Clock clock;

    @Autowired
    AiBodyContextBuilder(BodyInsightQuery bodyInsightQuery) {
        this(bodyInsightQuery, Clock.systemDefaultZone());
    }

    AiBodyContextBuilder(
            BodyInsightQuery bodyInsightQuery,
            Clock clock) {
        this.bodyInsightQuery = bodyInsightQuery;
        this.clock = clock;
    }

    AiContextSection build(Long userId) {
        List<BodyInsight> records = bodyInsightQuery
                .findRecent(userId, RECENT_LIMIT)
                .stream()
                .sorted(Comparator
                        .comparing(BodyInsight::measuredAt)
                        .thenComparing(BodyInsight::id)
                        .reversed())
                .toList();

        if (records.isEmpty()) {
            return AiContextSection.of(
                    "[신체 기록]" + NL + "- 신체 기록 없음",
                    AiContextType.BODY_TREND);
        }

        BodyInsight latest = records.getFirst();
        BodyInsight previous =
                records.size() > 1 ? records.get(1) : null;

        StringBuilder text = new StringBuilder()
                .append("[신체 기록]").append(NL)
                .append("- 최신 측정일: ")
                .append(LocalDate.ofInstant(
                        latest.measuredAt(),
                        clock.getZone()))
                .append(NL)
                .append("- 체중: ")
                .append(number(latest.weightKg()))
                .append("kg")
                .append(NL)
                .append("- 체지방률: ")
                .append(number(latest.bodyFatPercentage()))
                .append("%")
                .append(NL)
                .append("- 골격근량: ")
                .append(number(latest.skeletalMuscleKg()))
                .append("kg")
                .append(NL);

        if (previous != null) {
            appendChange(text, latest, previous);
        }

        return AiContextSection.of(
                text.toString().trim(),
                AiContextType.BODY_TREND);
    }

    private static void appendChange(
            StringBuilder text,
            BodyInsight latest,
            BodyInsight previous) {
        text.append("- 직전 기록 대비: 체중 ")
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
}
