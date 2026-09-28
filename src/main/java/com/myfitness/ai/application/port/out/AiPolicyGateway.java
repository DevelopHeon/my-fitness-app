package com.myfitness.ai.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AiPolicyGateway {
    Set<String> TOPICS =
            Set.of(
                    "WORKOUT",
                    "NUTRITION",
                    "BODY",
                    "GENERAL_FITNESS",
                    "COMPOSITE",
                    "OUT_OF_SCOPE",
                    "AMBIGUOUS");

    AiPolicyAssessment assess(AiPolicyRequest request);

    record AiPolicyRequest(
            String currentQuestion,
            String screen,
            List<AiChatGateway.HistoryMessage> previousTurns) {
        public AiPolicyRequest {
            if (currentQuestion == null
                    || currentQuestion.isBlank()
                    || currentQuestion.length() > 1000) {
                throw new IllegalArgumentException("정책 평가 질문은 1~1000자여야 합니다.");
            }
            previousTurns = List.copyOf(previousTurns);
        }
    }

    record AiPolicyAssessment(
            String model,
            double medicalDecision,
            double unsafeAction,
            double urgentSignal,
            double policyBypass,
            TopicAssessment topic,
            Integer inputTokens,
            long latencyMs) {
        public AiPolicyAssessment {
            if (model == null
                    || model.isBlank()
                    || topic == null
                    || inputTokens == null
                    || inputTokens < 0
                    || latencyMs < 0) {
                throw new IllegalArgumentException("정책 평가 메타데이터가 유효하지 않습니다.");
            }
            probability(medicalDecision);
            probability(unsafeAction);
            probability(urgentSignal);
            probability(policyBypass);
        }
    }

    record TopicAssessment(String choice, Map<String, Double> probabilities, double confidence) {
        public TopicAssessment {
            if (!TOPICS.contains(choice)
                    || probabilities == null
                    || !probabilities.keySet().equals(TOPICS)) {
                throw new IllegalArgumentException("정책 주제 선택지가 일치하지 않습니다.");
            }
            probability(confidence);
            probabilities.values().forEach(AiPolicyGateway::probability);
            if (Math.abs(probabilities.values().stream().mapToDouble(Double::doubleValue).sum() - 1)
                    > 1e-6) {
                throw new IllegalArgumentException("주제 확률 합은 1이어야 합니다.");
            }
            if (probabilities.get(choice)
                    < probabilities.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .max()
                            .orElseThrow()) {
                throw new IllegalArgumentException("선택 주제는 가장 높은 확률이어야 합니다.");
            }
            probabilities = Map.copyOf(probabilities);
        }
    }

    private static void probability(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("확률은 유한한 0~1 값이어야 합니다.");
        }
    }
}
