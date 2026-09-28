package com.myfitness.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiMessageRole;
import com.myfitness.ai.domain.model.AiQueryType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class AiHistorySelectorTest {

    @Test
    @DisplayName("단일 영역 질문 History는 같은 영역과 Composite만 남기고 다른 영역과 OutOfScope를 제외한다")
    void filtersHistoryByCurrentQueryType() {
        AiCoachProperties properties = new AiCoachProperties();
        properties.setHistoryMessageLimit(10);
        properties.setHistoryCharLimit(1000);
        AiHistorySelector selector = new AiHistorySelector(properties);

        List<HistoryMessage> history =
                selector.select(
                        List.of(
                                user(AiQueryType.NUTRITION, "영양 질문"),
                                assistant(AiQueryType.NUTRITION, "영양 답변"),
                                user(AiQueryType.COMPOSITE, "복합 질문"),
                                assistant(AiQueryType.COMPOSITE, "복합 답변"),
                                user(AiQueryType.OUT_OF_SCOPE, "날씨"),
                                assistant(AiQueryType.OUT_OF_SCOPE, "범위 밖"),
                                user(AiQueryType.WORKOUT, "운동 질문"),
                                assistant(AiQueryType.WORKOUT, "운동 답변")),
                        AiQueryType.WORKOUT);

        assertThat(history)
                .extracting(item -> item.content())
                .containsExactly("복합 질문", "복합 답변", "운동 질문", "운동 답변");
    }

    @Test
    @DisplayName("History는 최근 메시지 개수와 전체 문자 수 제한을 적용하고 시간 순서를 유지한다")
    void appliesMessageAndCharacterLimits() {
        AiCoachProperties properties = new AiCoachProperties();
        properties.setHistoryMessageLimit(2);
        properties.setHistoryCharLimit(7);
        AiHistorySelector selector = new AiHistorySelector(properties);

        List<HistoryMessage> history =
                selector.select(
                        List.of(
                                user(AiQueryType.WORKOUT, "1111"),
                                assistant(AiQueryType.WORKOUT, "2222"),
                                user(AiQueryType.WORKOUT, "3333")),
                        AiQueryType.WORKOUT);

        assertThat(history).extracting(item -> item.content()).containsExactly("222", "3333");
        assertThat(history)
                .extracting(item -> item.role())
                .containsExactly(AiMessageRole.ASSISTANT, AiMessageRole.USER);
    }

    private static AiMessage user(AiQueryType queryType, String content) {
        return AiMessage.user(1L, queryType, content, Instant.parse("2026-09-22T00:00:00Z"));
    }

    private static AiMessage assistant(AiQueryType queryType, String content) {
        return AiMessage.assistant(1L, queryType, content, Instant.parse("2026-09-22T00:00:01Z"));
    }
}
