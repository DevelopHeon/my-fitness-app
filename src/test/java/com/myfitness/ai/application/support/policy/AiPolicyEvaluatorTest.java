package com.myfitness.ai.application.support.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.application.port.out.AiPolicyGateway.TopicAssessment;
import com.myfitness.ai.application.port.out.AiPolicyGateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.stream.Collectors;

class AiPolicyEvaluatorTest {
    private final AiPolicyEvaluator evaluator = new AiPolicyEvaluator(new AiPolicyProperties());

    @ParameterizedTest
    @DisplayName("위험 확률과 주제 불확실성 경계에서 정해진 정책 동작을 적용한다")
    @CsvSource({
        "0,0,0,0,WORKOUT,0.95,ALLOW,ALLOWED",
        "0.349999,0,0,0,WORKOUT,0.95,ALLOW,ALLOWED",
        "0.35,0,0,0,WORKOUT,0.95,CLARIFY,POLICY_UNCERTAIN",
        "0.699999,0,0,0,WORKOUT,0.95,CLARIFY,POLICY_UNCERTAIN",
        "0.70,0,0,0,WORKOUT,0.95,SAFE_REDIRECT,MEDICAL_DECISION",
        "0,0.70,0,0,WORKOUT,0.95,SAFE_REDIRECT,UNSAFE_ACTION",
        "0,0,0.349999,0,WORKOUT,0.95,ALLOW,ALLOWED",
        "0,0,0.35,0,OUT_OF_SCOPE,0.95,SAFE_REDIRECT,URGENT_SIGNAL",
        "0.95,0.95,0.9,0.95,WORKOUT,0.95,SAFE_REDIRECT,URGENT_SIGNAL",
        "0.95,0.95,0,0.95,WORKOUT,0.95,SAFE_REDIRECT,MEDICAL_DECISION",
        "0,0,0,0.699999,WORKOUT,0.95,CLARIFY,POLICY_UNCERTAIN",
        "0,0,0,0.70,WORKOUT,0.95,BLOCK,POLICY_BYPASS",
        "0,0,0,0.45,OUT_OF_SCOPE,0.95,BLOCK,OUT_OF_SCOPE",
        "0,0,0,0,WORKOUT,0.599999,CLARIFY,TOPIC_UNCERTAIN",
        "0,0,0,0,WORKOUT,0.60,ALLOW,ALLOWED",
        "0,0,0,0,AMBIGUOUS,0.95,CLARIFY,TOPIC_UNCERTAIN",
        "0,0,0,0,OUT_OF_SCOPE,0.95,BLOCK,OUT_OF_SCOPE"
    })
    void appliesPolicyBoundaries(
            double medical,
            double unsafe,
            double urgent,
            double bypass,
            String topic,
            double confidence,
            String action,
            String reason) {
        AiPolicyDecision result =
                evaluator.decide(assessment(medical, unsafe, urgent, bypass, topic, confidence));
        assertThat(result.action().name()).isEqualTo(action);
        assertThat(result.reason()).isEqualTo(reason);
        if (action.equals("ALLOW")) assertThat(result.queryType().name()).isEqualTo(topic);
        if (topic.equals("AMBIGUOUS")) assertThat(result.queryType()).isNull();
    }

    public static AiPolicyAssessment assessment(
            double medical,
            double unsafe,
            double urgent,
            double bypass,
            String topic,
            double confidence) {
        Map<String, Double> distribution =
                AiPolicyGateway.TOPICS.stream()
                        .collect(Collectors.toMap(it -> it, it -> it.equals(topic) ? 1.0 : 0.0));
        return new AiPolicyAssessment(
                "jev-1.13.0",
                medical,
                unsafe,
                urgent,
                bypass,
                new TopicAssessment(topic, distribution, confidence),
                120,
                10);
    }
}
