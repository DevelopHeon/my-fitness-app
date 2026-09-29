package com.myfitness.ai.application.support.policy;

import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;

import java.util.Objects;

public sealed interface AiPolicyRun {
    String version();

    long latencyMs();

    record Success(
            String version,
            AiPolicyDecision decision,
            AiPolicyAssessment assessment,
            long latencyMs)
            implements AiPolicyRun {
        public Success {
            Objects.requireNonNull(version);
            Objects.requireNonNull(decision);
            Objects.requireNonNull(assessment);
            if (version.isBlank() || latencyMs < 0) {
                throw new IllegalArgumentException("정책 버전과 지연 시간이 유효하지 않습니다.");
            }
        }
    }

    record Failure(String version, String errorCode, long latencyMs) implements AiPolicyRun {
        public Failure {
            Objects.requireNonNull(version);
            Objects.requireNonNull(errorCode);
            if (version.isBlank() || errorCode.isBlank() || latencyMs < 0) {
                throw new IllegalArgumentException("정책 실패의 버전·오류·지연 시간이 유효하지 않습니다.");
            }
        }
    }
}
