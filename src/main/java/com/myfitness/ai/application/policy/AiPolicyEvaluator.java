package com.myfitness.ai.application.policy;

import static com.myfitness.ai.application.policy.AiPolicyDecision.Action.*;

import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.application.port.out.AiPolicyGateway.TopicAssessment;
import com.myfitness.ai.domain.model.AiQueryType;

import org.springframework.stereotype.Component;

@Component
public class AiPolicyEvaluator {
    private final AiPolicyProperties properties;

    public AiPolicyEvaluator(AiPolicyProperties properties) {
        this.properties = properties;
    }

    public AiPolicyDecision decide(AiPolicyAssessment assessment) {
        TopicAssessment topic = assessment.topic();
        AiQueryType type =
                topic.choice().equals("AMBIGUOUS") ? null : AiQueryType.valueOf(topic.choice());
        if (assessment.urgentSignal() >= properties.getReviewThreshold()) {
            return new AiPolicyDecision(SAFE_REDIRECT, "URGENT_SIGNAL", type);
        }
        if (assessment.medicalDecision() >= properties.getActionThreshold()) {
            return new AiPolicyDecision(SAFE_REDIRECT, "MEDICAL_DECISION", type);
        }
        if (assessment.unsafeAction() >= properties.getActionThreshold()) {
            return new AiPolicyDecision(SAFE_REDIRECT, "UNSAFE_ACTION", type);
        }
        if (assessment.policyBypass() >= properties.getActionThreshold()) {
            return new AiPolicyDecision(BLOCK, "POLICY_BYPASS", type);
        }
        if (Math.max(
                        assessment.medicalDecision(),
                        Math.max(assessment.unsafeAction(), assessment.policyBypass()))
                >= properties.getReviewThreshold()) {
            return new AiPolicyDecision(CLARIFY, "POLICY_UNCERTAIN", type);
        }
        if (type == null || topic.confidence() < properties.getTopicConfidence()) {
            return new AiPolicyDecision(CLARIFY, "TOPIC_UNCERTAIN", type);
        }
        return type == AiQueryType.OUT_OF_SCOPE
                ? new AiPolicyDecision(BLOCK, "OUT_OF_SCOPE", type)
                : new AiPolicyDecision(ALLOW, "ALLOWED", type);
    }
}
