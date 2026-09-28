package com.myfitness.ai.application.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.application.context.AiContextSelector.AiContextArea;
import com.myfitness.ai.domain.model.AiQueryType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

class AiContextSelectorTest {
    private final AiContextSelector selector = new AiContextSelector();

    @Test
    @DisplayName("체중과 운동을 함께 묻는 Composite 질문은 Body와 Workout Context만 선택한다")
    void selectsOnlyBodyAndWorkoutForExplicitCompositeQuestion() {
        Set<AiContextArea> selected =
                selector.select(
                        AiQueryType.COMPOSITE,
                        new AiClientContext("DASHBOARD", null, null),
                        "최근 체중과 운동 퍼포먼스 변화를 같이 봐줘");

        assertThat(selected).containsExactlyInAnyOrder(AiContextArea.BODY, AiContextArea.WORKOUT);
    }

    @Test
    @DisplayName("영양과 운동을 함께 묻는 Composite 질문은 Nutrition과 Workout Context만 선택한다")
    void selectsOnlyNutritionAndWorkoutForExplicitCompositeQuestion() {
        Set<AiContextArea> selected =
                selector.select(
                        AiQueryType.COMPOSITE,
                        new AiClientContext("NUTRITION", LocalDate.of(2026, 9, 22), null),
                        "오늘 식단과 운동량을 같이 평가해줘");

        assertThat(selected)
                .containsExactlyInAnyOrder(AiContextArea.NUTRITION, AiContextArea.WORKOUT);
    }

    @Test
    @DisplayName("영역이 드러나지 않는 Dashboard Composite 질문은 세 기록 Context를 함께 선택한다")
    void selectsAllPersonalContextsForGenericDashboardCompositeQuestion() {
        Set<AiContextArea> selected =
                selector.select(
                        AiQueryType.COMPOSITE,
                        new AiClientContext("DASHBOARD", null, null),
                        "내 기록에서 개선할 점을 알려줘");

        assertThat(selected)
                .containsExactlyInAnyOrder(
                        AiContextArea.WORKOUT, AiContextArea.BODY, AiContextArea.NUTRITION);
    }
}
