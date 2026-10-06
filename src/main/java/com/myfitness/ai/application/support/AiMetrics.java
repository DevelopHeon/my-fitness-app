package com.myfitness.ai.application.support;

import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** AI 결과를 관측 지표로 기록한다. 업무 판정·저장·외부 호출을 수행하지 않는다. */
@Component
public class AiMetrics {
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
            error = policyError(failure.error());
        }
        Timer.builder("app.ai.policy")
                .tags("action", action, "topic", topic, "error", error)
                .serviceLevelObjectives(seconds(0.05, 0.1, 0.25, 0.5, 1, 1.5, 2, 5))
                .register(registry)
                .record(run.latencyMs(), TimeUnit.MILLISECONDS);
    }

    public void provider(
            String kind, String provider, String outcome,
            AiProviderUnavailableException.Code error, long elapsedMillis) {
        Timer.builder("app.ai.provider")
                .tags("kind", kind, "provider", providerName(provider), "outcome", outcome,
                        "error", error.name())
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

    private static String policyError(AiPolicyUnavailableException error) {
        if (error.getCode() != AiPolicyUnavailableException.Code.HTTP_ERROR) {
            return error.getCode().name();
        }
        return switch (error.getHttpStatus() / 100) {
            case 4 -> "HTTP_4XX";
            case 5 -> "HTTP_5XX";
            default -> "HTTP_OTHER";
        };
    }

    private static Duration[] seconds(double... values) {
        Duration[] durations = new Duration[values.length];
        for (int index = 0; index < values.length; index++) {
            durations[index] = Duration.ofMillis((long) (values[index] * 1000));
        }
        return durations;
    }
}
