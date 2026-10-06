package com.myfitness.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.port.out.AiPolicyGateway;
import com.myfitness.ai.application.support.policy.AiPolicyDecision;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AiMetricsTest {
    @Test
    void exportsProviderHistogramAndObservedTokensWithDashboardMetricNames() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            AiMetrics metrics = new AiMetrics(registry);
            metrics.policy(new AiPolicyRun.Failure("version", "TIMEOUT", 25));
            metrics.provider("chat", "openai", "SUCCESS", "NONE", 1000);
            metrics.tokens("chat", "openai", 12, 3);
            String scrape = registry.scrape();
            assertThat(scrape).contains("app_ai_policy_seconds_count{", "action=\"UNAVAILABLE\"",
                    "app_ai_policy_seconds_bucket{", "app_ai_provider_seconds_bucket{", "le=\"1.0\"",
                    "app_ai_provider_seconds_count{", "app_ai_tokens_total{", "direction=\"input\"");
        } finally {
            registry.close();
        }
    }

    @Test
    void countsEveryPolicyActionAndFailureWithoutRecordingQuestionContent() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(registry);
        AiPolicyGateway.TopicAssessment topic = new AiPolicyGateway.TopicAssessment(
                "WORKOUT", Map.of("WORKOUT", 1.0, "NUTRITION", 0.0, "BODY", 0.0,
                        "GENERAL_FITNESS", 0.0, "COMPOSITE", 0.0, "OUT_OF_SCOPE", 0.0, "AMBIGUOUS", 0.0), 1.0);
        AiPolicyGateway.AiPolicyAssessment assessment = new AiPolicyGateway.AiPolicyAssessment(
                "model", 0, 0, 0, 0, topic, 12, 10);
        for (AiPolicyDecision.Action action : AiPolicyDecision.Action.values()) {
            metrics.policy(new AiPolicyRun.Success("version", new AiPolicyDecision(action, "reason", null),
                    assessment, 25));
            assertThat(registry.get("app.ai.policy").tag("action", action.name()).timer().count()).isEqualTo(1);
        }
        metrics.policy(new AiPolicyRun.Failure("version", "HTTP_429", 100));
        assertThat(registry.get("app.ai.policy").tag("action", "UNAVAILABLE").tag("error", "HTTP_4XX")
                .timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(100);
        assertThat(registry.get("app.ai.tokens").tag("kind", "policy").counter().count()).isEqualTo(48);
        assertThat(registry.getMeters()).allSatisfy(meter -> {
            assertThat(meter.getId().getTags()).allSatisfy(tag ->
                    assertThat(tag.getKey()).isIn("action", "topic", "error", "kind", "provider", "direction", "le"));
        });
    }

    @Test
    void distinguishesPhotoResultsAndNormalizesUnknownErrorsAndProviders() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(registry);
        for (String outcome : new String[] {"FOOD", "NOT_FOOD", "UNCERTAIN"}) {
            metrics.provider("photo", "openai", outcome, "NONE", 20);
            assertThat(registry.get("app.ai.provider").tag("outcome", outcome).timer().count()).isEqualTo(1);
        }
        metrics.provider("chat", "unexpected-model-provider", "FAILURE", "secret error text", 15);
        assertThat(registry.get("app.ai.provider").tag("provider", "other").tag("error", "OTHER")
                .timer().count()).isEqualTo(1);
        metrics.tokens("chat", "openai", null, null);
        assertThat(registry.find("app.ai.tokens").meters()).isEmpty();
        metrics.tokens("chat", "openai", 0, 3);
        assertThat(registry.get("app.ai.tokens").tag("direction", "output").counter().count()).isEqualTo(3);
    }
}
