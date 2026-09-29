package com.myfitness.ai.application.support.policy;

import com.myfitness.ai.application.config.AiPolicyProperties;
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

    public AiPolicyGuard(
            AiPolicyProperties properties, AiPolicyGateway gateway, AiPolicyEvaluator evaluator) {
        this.properties = properties;
        this.gateway = gateway;
        this.evaluator = evaluator;
    }

    public AiPolicyRun evaluate(
            String question, AiClientContext context, List<AiMessage> acceptedHistory) {
        long started = System.nanoTime();
        try {
            AiPolicyGateway.AiPolicyAssessment assessment =
                    gateway.assess(
                            new AiPolicyRequest(
                                    question,
                                    context == null ? null : context.normalizedScreen(),
                                    selectHistory(acceptedHistory)));
            AiPolicyDecision decision = evaluator.decide(assessment);
            return new AiPolicyRun.Success(
                    properties.getVersion(), decision, assessment, elapsed(started));
        } catch (RuntimeException exception) {
            String code =
                    exception instanceof AiPolicyUnavailableException policy
                            ? policy.getCode()
                            : "POLICY_ERROR";
            return new AiPolicyRun.Failure(properties.getVersion(), code, elapsed(started));
        }
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
