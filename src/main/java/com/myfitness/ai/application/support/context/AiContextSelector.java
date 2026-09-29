package com.myfitness.ai.application.support.context;

import com.myfitness.ai.application.dto.request.AiClientContext;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiContextSelector {
    private static final Set<String> WORKOUT_KEYWORDS = Set.of(
            "운동", "웨이트", "루틴", "세트", "중량", "벤치", "스쿼트",
            "데드", "프레스", "로우", "컬", "풀다운", "풀업", "볼륨",
            "volume", "1rm", "pr", "퍼포먼스");
    private static final Set<String> BODY_KEYWORDS = Set.of(
            "체중", "몸무게", "체지방", "골격근", "근육량", "인바디",
            "신체", "weight", "bodyfat");
    private static final Set<String> NUTRITION_KEYWORDS = Set.of(
            "식단", "영양", "칼로리", "단백질", "탄수", "지방", "음식",
            "식사", "먹", "다이어트", "벌크", "감량", "증량", "매크로",
            "protein", "calorie");

    public Set<AiContextArea> select(
            AiQueryType queryType,
            AiClientContext clientContext,
            String question) {
        if (queryType == null
                || queryType == AiQueryType.OUT_OF_SCOPE
                || queryType == AiQueryType.GENERAL_FITNESS) {
            return Set.of();
        }

        return switch (queryType) {
            case WORKOUT -> Set.of(AiContextArea.WORKOUT);
            case BODY -> Set.of(AiContextArea.BODY);
            case NUTRITION -> Set.of(AiContextArea.NUTRITION);
            case COMPOSITE -> selectComposite(clientContext, question);
            case GENERAL_FITNESS, OUT_OF_SCOPE -> Set.of();
        };
    }

    private static Set<AiContextArea> selectComposite(
            AiClientContext clientContext,
            String question) {
        EnumSet<AiContextArea> areas = EnumSet.noneOf(AiContextArea.class);
        String text = question == null
                ? ""
                : question.toLowerCase(Locale.ROOT);

        if (containsAny(text, WORKOUT_KEYWORDS)) {
            areas.add(AiContextArea.WORKOUT);
        }
        if (containsAny(text, BODY_KEYWORDS)) {
            areas.add(AiContextArea.BODY);
        }
        if (containsAny(text, NUTRITION_KEYWORDS)) {
            areas.add(AiContextArea.NUTRITION);
        }

        if (areas.size() >= 2) {
            return Set.copyOf(areas);
        }

        AiContextArea screenArea = screenArea(clientContext);
        if (screenArea != null) {
            areas.add(screenArea);
        }

        if (areas.size() >= 2) {
            return Set.copyOf(areas);
        }

        return Set.of(
                AiContextArea.WORKOUT,
                AiContextArea.BODY,
                AiContextArea.NUTRITION);
    }

    private static boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static AiContextArea screenArea(AiClientContext context) {
        if (context == null || context.normalizedScreen() == null) {
            return null;
        }
        return switch (context.normalizedScreen()) {
            case "WORKOUT", "ROUTINE" -> AiContextArea.WORKOUT;
            case "BODY" -> AiContextArea.BODY;
            case "NUTRITION" -> AiContextArea.NUTRITION;
            default -> null;
        };
    }

    public enum AiContextArea {
        WORKOUT,
        BODY,
        NUTRITION
    }
}
