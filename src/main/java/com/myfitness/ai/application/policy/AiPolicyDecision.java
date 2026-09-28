package com.myfitness.ai.application.policy;

import com.myfitness.ai.domain.model.AiQueryType;

public record AiPolicyDecision(Action action, String reason, AiQueryType queryType) {
    public enum Action {
        ALLOW,
        BLOCK,
        SAFE_REDIRECT,
        CLARIFY
    }

    public AiQueryType storedQueryType() {
        return queryType == null ? AiQueryType.OUT_OF_SCOPE : queryType;
    }
}
