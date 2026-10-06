package com.myfitness.ai.application.support.policy;

import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.support.AiMetrics;
import com.myfitness.ai.application.dto.request.AiClientContext;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyRequest;
import com.myfitness.ai.application.port.out.AiPolicyGateway;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiMessageRole;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class AiPolicyGuard {
    private final AiPolicyProperties properties;
    private final AiPolicyGateway gateway;
    private final AiPolicyEvaluator evaluator;
    private final AiMetrics metrics;

    public AiPolicyGuard(
            AiPolicyProperties properties, AiPolicyGateway gateway, AiPolicyEvaluator evaluator, AiMetrics metrics) {
        this.properties = properties;
        this.gateway = gateway;
        this.evaluator = evaluator;
        this.metrics = metrics;
    }

    public AiPolicyRun evaluate(
            String question, AiClientContext context, List<AiMessage> acceptedHistory) {
        long started = System.nanoTime();
        AiPolicyRun run;
        try {
            AiPolicyGateway.AiPolicyAssessment assessment =
                    gateway.assess(
                            new AiPolicyRequest(
                                    question,
                                    context == null ? null : context.normalizedScreen(),
                                    selectHistory(acceptedHistory)));
            AiPolicyDecision decision = evaluator.decide(assessment);
            run = new AiPolicyRun.Success(
                    properties.getVersion(), decision, assessment, elapsed(started));
        } catch (RuntimeException exception) {
            AiPolicyUnavailableException error =
                    exception instanceof AiPolicyUnavailableException policy
                            ? policy
                            : new AiPolicyUnavailableException(AiPolicyUnavailableException.Code.POLICY_ERROR, exception);
            run = new AiPolicyRun.Failure(properties.getVersion(), error, elapsed(started));
        }
        metrics.policy(run);
        return run;
    }

    static List<HistoryMessage> selectHistory(List<AiMessage> messages) {
        List<HistoryMessage> result = new ArrayList<>();
        int chars = 0;
        for (int index = messages.size() - 2; index >= 0 && result.size() < 4; index -= 2) {
            AiMessage user = messages.get(index);
            AiMessage assistant = messages.get(index + 1);
            if (user.getRole() != AiMessageRole.USER
                    || assistant.getRole() != AiMessageRole.ASSISTANT) continue;
            int pairChars = user.getContent().length() + assistant.getContent().length();
            if (chars + pairChars > 2000) break;
            result.addFirst(new HistoryMessage(AiMessageRole.ASSISTANT, assistant.getContent()));
            result.addFirst(new HistoryMessage(AiMessageRole.USER, user.getContent()));
            chars += pairChars;
        }
        return List.copyOf(result);
    }

    private static long elapsed(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
