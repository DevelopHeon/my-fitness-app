package com.myfitness.ai.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class AiPolicyGuardTest {
    @Test
    @DisplayName("평가 이력은 최신 허용 user/assistant 쌍 4개 메시지·2000자 이내이며 쌍을 자르지 않는다")
    void boundsHistoryWithoutPartialPairs() {
        Instant time = Instant.parse("2026-09-28T00:00:00Z");
        List<AiMessage> messages =
                List.of(
                        AiMessage.user(1L, AiQueryType.WORKOUT, "older", time),
                        AiMessage.assistant(1L, AiQueryType.WORKOUT, "older answer", time),
                        AiMessage.user(1L, AiQueryType.WORKOUT, "u".repeat(500), time),
                        AiMessage.assistant(1L, AiQueryType.WORKOUT, "a".repeat(500), time),
                        AiMessage.user(1L, AiQueryType.WORKOUT, "v".repeat(500), time),
                        AiMessage.assistant(1L, AiQueryType.WORKOUT, "b".repeat(500), time));
        List<HistoryMessage> selected = AiPolicyGuard.selectHistory(messages);
        assertThat(selected).hasSize(4);
        assertThat(selected.stream().mapToInt(it -> it.content().length()).sum()).isEqualTo(2000);
        assertThat(selected.getFirst().content()).startsWith("u");
        assertThat(
                        AiPolicyGuard.selectHistory(
                                List.of(
                                        AiMessage.user(
                                                1L, AiQueryType.WORKOUT, "u".repeat(2000), time),
                                        AiMessage.assistant(1L, AiQueryType.WORKOUT, "a", time))))
                .isEmpty();
    }
}
