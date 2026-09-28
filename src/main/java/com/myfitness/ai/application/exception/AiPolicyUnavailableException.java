package com.myfitness.ai.application.exception;

public class AiPolicyUnavailableException extends RuntimeException {
    private final String code;

    public AiPolicyUnavailableException(String code) {
        super("질문 정책 검증을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
