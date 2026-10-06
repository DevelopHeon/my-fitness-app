package com.myfitness.ai.application.support;

import com.myfitness.ai.application.support.policy.AiPolicyRun;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** AI 결과를 관측 지표로 기록한다. 업무 판정·저장·외부 호출을 수행하지 않는다. */
@Component
public class AiMetrics {
    private static final Set<String> POLICY_ERRORS = Set.of(
            "NONE", "CONFIGURATION", "TIMEOUT", "NETWORK", "INTERRUPTED", "MODEL_MISMATCH", "POLICY_ERROR",
            "INVALID_RESPONSE", "INVALID_RESPONSE_JSON", "INVALID_RESPONSE_TOPIC", "INVALID_RESPONSE_USAGE",
            "INVALID_RESPONSE_MEDICAL_DECISION", "INVALID_RESPONSE_UNSAFE_ACTION", "INVALID_RESPONSE_URGENT_SIGNAL",
            "INVALID_RESPONSE_POLICY_BYPASS", "INVALID_RESPONSE_ASSESSMENT");
    private static final Set<String> PROVIDER_ERRORS = Set.of(
            "NONE", "CONFIGURATION_ERROR", "TIMEOUT", "TRANSPORT_ERROR", "HTTP_ERROR", "INVALID_RESPONSE",
            "MODEL_REFUSAL", "INCOMPLETE_RESPONSE", "AI_PROVIDER_UNAVAILABLE");
    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void policy(AiPolicyRun run) {
        String action;
        String topic;
        String error;
        if (run instanceof AiPolicyRun.Success success) {
            action = success.decision().action().name();
            topic = success.assessment().topic().choice();
            error = "NONE";
            tokens("policy", "jev", success.assessment().inputTokens(), null);
        } else {
            AiPolicyRun.Failure failure = (AiPolicyRun.Failure) run;
            action = "UNAVAILABLE";
            topic = "UNKNOWN";
            error = policyError(failure.errorCode());
        }
        Timer.builder("app.ai.policy")
                .tags("action", action, "topic", topic, "error", error)
                .serviceLevelObjectives(seconds(0.05, 0.1, 0.25, 0.5, 1, 1.5, 2, 5))
                .register(registry)
                .record(run.latencyMs(), TimeUnit.MILLISECONDS);
    }

    public void provider(String kind, String provider, String outcome, String error, long elapsedMillis) {
        Timer.builder("app.ai.provider")
                .tags("kind", kind, "provider", providerName(provider), "outcome", outcome,
                        "error", PROVIDER_ERRORS.contains(error) ? error : "OTHER")
                .serviceLevelObjectives(seconds(0.5, 1, 2, 5, 10, 20, 30, 60))
                .register(registry)
                .record(elapsedMillis, TimeUnit.MILLISECONDS);
    }

    public void tokens(String kind, String provider, Integer input, Integer output) {
        token(kind, provider, "input", input);
        token(kind, provider, "output", output);
    }

    private void token(String kind, String provider, String direction, Integer value) {
        if (value != null && value >= 0) {
            registry.counter("app.ai.tokens", "kind", kind, "provider", providerName(provider),
                    "direction", direction).increment(value);
        }
    }

    private static String providerName(String provider) {
        return switch (provider) {
            case "openai", "ollama", "none", "jev" -> provider;
            default -> "other";
        };
    }

    private static String policyError(String error) {
        if (error.matches("HTTP_4[0-9]{2}")) {
            return "HTTP_4XX";
        }
        if (error.matches("HTTP_5[0-9]{2}")) {
            return "HTTP_5XX";
        }
        if (error.startsWith("HTTP_")) {
            return "HTTP_OTHER";
        }
        return POLICY_ERRORS.contains(error) ? error : "OTHER";
    }

    private static Duration[] seconds(double... values) {
        Duration[] durations = new Duration[values.length];
        for (int index = 0; index < values.length; index++) {
            durations[index] = Duration.ofMillis((long) (values[index] * 1000));
        }
        return durations;
    }
}
