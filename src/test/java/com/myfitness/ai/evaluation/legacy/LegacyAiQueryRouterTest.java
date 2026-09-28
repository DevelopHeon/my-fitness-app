package com.myfitness.ai.evaluation.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.domain.model.AiQueryType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegacyAiQueryRouterTest {
    private final LegacyAiQueryRouter router = new LegacyAiQueryRouter();

    @Test
    @DisplayName("벤치 중량 질문은 Workout 질문으로 분류한다")
    void routesWorkoutQuestion() {
        assertThat(router.route(
                "다음 벤치프레스 중량은 어떻게 잡을까?",
                null,
                null))
                .isEqualTo(AiQueryType.WORKOUT);
    }

    @Test
    @DisplayName("단백질 질문은 Nutrition 질문으로 분류한다")
    void routesNutritionQuestion() {
        assertThat(router.route(
                "오늘 단백질 얼마나 남았어?",
                null,
                null))
                .isEqualTo(AiQueryType.NUTRITION);
    }

    @Test
    @DisplayName("체중과 운동을 함께 묻는 질문은 Composite로 분류한다")
    void routesCompositeQuestion() {
        assertThat(router.route(
                "체중은 줄었는데 운동 퍼포먼스는 어때?",
                null,
                null))
                .isEqualTo(AiQueryType.COMPOSITE);
    }

    @Test
    @DisplayName("명백한 개발 질문은 현재 화면보다 우선해 Out Of Scope로 차단한다")
    void rejectsClearOutOfScopeQuestion() {
        AiClientContext context = new AiClientContext(
                "WORKOUT",
                LocalDate.of(2026, 9, 22),
                null);

        assertThat(router.route(
                "자바 스프링 트랜잭션 설명해줘",
                context,
                AiQueryType.WORKOUT))
                .isEqualTo(AiQueryType.OUT_OF_SCOPE);
    }

    @Test
    @DisplayName("짧은 후속 질문은 직전 Fitness 질문 유형을 이어간다")
    void usesPreviousQueryTypeForFollowUp() {
        assertThat(router.route(
                "조금 더 올려도 될까?",
                null,
                AiQueryType.WORKOUT))
                .isEqualTo(AiQueryType.WORKOUT);
    }
}
