package com.myfitness.ai.application.support.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.port.out.AiPolicyGateway;
import com.myfitness.ai.application.support.AiMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class AiPolicyGuardTest {
    @Test
    void preservesTypedHttpFailureAcrossPolicyResult() {
        AiPolicyUnavailableException error = AiPolicyUnavailableException.httpFailure(429);
        AiPolicyRun.Failure failure = evaluateFailure(error);
        assertThat(failure.error()).isSameAs(error);
        assertThat(failure.error().getLogCode()).isEqualTo("HTTP_429");
    }

    @Test
    void classifiesUnexpectedFailureWithoutLeakingItsMessageToMetrics() {
        IllegalStateException cause = new IllegalStateException("private provider response");
        AiPolicyRun.Failure failure = evaluateFailure(cause);
        assertThat(failure.error().getCode()).isEqualTo(AiPolicyUnavailableException.Code.POLICY_ERROR);
        assertThat(failure.error().getCause()).isSameAs(cause);
        assertThat(failure.error().getMessage()).doesNotContain("private");
    }

    private AiPolicyRun.Failure evaluateFailure(RuntimeException error) {
        AiPolicyGateway gateway = mock(AiPolicyGateway.class);
        when(gateway.assess(any())).thenThrow(error);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiPolicyGuard guard = new AiPolicyGuard(new AiPolicyProperties(), gateway,
                new AiPolicyEvaluator(new AiPolicyProperties()), new AiMetrics(registry));
        AiPolicyRun run = guard.evaluate("question", null, List.of());
        assertThat(run).isInstanceOf(AiPolicyRun.Failure.class);
        assertThat(registry.get("app.ai.policy").tag("action", "UNAVAILABLE").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().toString()).doesNotContain("private provider response", "question"));
        return (AiPolicyRun.Failure) run;
    }

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
