package com.myfitness.ai.application.router;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiQueryRouter {
    private static final Set<String> WORKOUT = Set.of(
            "운동", "헬스", "웨이트", "루틴", "세트", "반복", "횟수",
            "중량", "벤치", "스쿼트", "데드", "프레스", "로우", "컬",
            "풀다운", "풀업", "근육", "볼륨", "volume", "1rm", "pr");
    private static final Set<String> NUTRITION = Set.of(
            "식단", "영양", "칼로리", "단백질", "탄수", "지방", "음식",
            "먹", "식사", "다이어트", "벌크", "감량", "증량", "매크로",
            "protein", "calorie");
    private static final Set<String> BODY = Set.of(
            "체중", "몸무게", "체지방", "골격근", "근육량", "인바디",
            "신체", "몸", "weight", "bodyfat");
    private static final Set<String> GENERAL = Set.of(
            "회복", "휴식", "수면", "스트레칭", "유산소", "근력",
            "fitness", "피트니스", "건강");
    private static final Set<String> CLEAR_OUT_OF_SCOPE = Set.of(
            "자바", "스프링", "코딩", "주식", "비트코인", "코인",
            "날씨", "정치", "선거", "영화", "게임", "여행", "번역");

    public AiQueryType route(
            String question,
            AiClientContext context,
            AiQueryType previousType) {
        if (question == null || question.isBlank()) {
            return AiQueryType.OUT_OF_SCOPE;
        }

        String text = question.toLowerCase(Locale.ROOT);
        boolean workout = containsAny(text, WORKOUT);
        boolean nutrition = containsAny(text, NUTRITION);
        boolean body = containsAny(text, BODY);

        int specificMatches =
                (workout ? 1 : 0) + (nutrition ? 1 : 0) + (body ? 1 : 0);
        if (specificMatches >= 2) {
            return AiQueryType.COMPOSITE;
        }
        if (workout) {
            return AiQueryType.WORKOUT;
        }
        if (nutrition) {
            return AiQueryType.NUTRITION;
        }
        if (body) {
            return AiQueryType.BODY;
        }
        if (containsAny(text, GENERAL)) {
            return AiQueryType.GENERAL_FITNESS;
        }
        if (containsAny(text, CLEAR_OUT_OF_SCOPE)) {
            return AiQueryType.OUT_OF_SCOPE;
        }

        AiQueryType screenType = screenType(context);
        if (screenType != null) {
            return screenType;
        }

        if (previousType != null
                && previousType != AiQueryType.OUT_OF_SCOPE) {
            return previousType;
        }

        return AiQueryType.OUT_OF_SCOPE;
    }

    private static boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static AiQueryType screenType(AiClientContext context) {
        if (context == null || context.normalizedScreen() == null) {
            return null;
        }
        return switch (context.normalizedScreen()) {
            case "WORKOUT", "ROUTINE" -> AiQueryType.WORKOUT;
            case "NUTRITION" -> AiQueryType.NUTRITION;
            case "BODY" -> AiQueryType.BODY;
            case "DASHBOARD" -> AiQueryType.COMPOSITE;
            default -> null;
        };
    }
}
