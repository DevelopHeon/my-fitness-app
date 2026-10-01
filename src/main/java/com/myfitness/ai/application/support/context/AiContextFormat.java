package com.myfitness.ai.application.support.context;

import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.MacroInsight;
import java.math.BigDecimal;

final class AiContextFormat {
    private AiContextFormat() {}

    static String macro(MacroInsight macro) {
        if (macro == null) {
            return "정보 없음";
        }
        return "칼로리 " + number(macro.calories())
                + "kcal / 탄수 " + number(macro.carbohydrateGrams())
                + "g / 단백질 " + number(macro.proteinGrams())
                + "g / 지방 " + number(macro.fatGrams()) + "g";
    }

    static String number(BigDecimal value) {
        if (value == null) {
            return "미입력으로 계산 불가";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    static String signed(BigDecimal value) {
        if (value.signum() > 0) {
            return "+" + number(value);
        }
        return number(value);
    }
}
