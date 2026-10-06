package com.myfitness.ai.application.exception;

import java.util.Objects;

public class AiPolicyUnavailableException extends RuntimeException {
    public enum Code {
        CONFIGURATION,
        TIMEOUT,
        NETWORK,
        INTERRUPTED,
        HTTP_ERROR,
        MODEL_MISMATCH,
        POLICY_ERROR,
        INVALID_RESPONSE,
        INVALID_RESPONSE_JSON,
        INVALID_RESPONSE_TOPIC,
        INVALID_RESPONSE_USAGE,
        INVALID_RESPONSE_MEDICAL_DECISION,
        INVALID_RESPONSE_UNSAFE_ACTION,
        INVALID_RESPONSE_URGENT_SIGNAL,
        INVALID_RESPONSE_POLICY_BYPASS,
        INVALID_RESPONSE_ASSESSMENT
    }

    private final Code code;
    private final int httpStatus;

    public AiPolicyUnavailableException(Code code) {
        this(code, null);
    }

    public AiPolicyUnavailableException(Code code, Throwable cause) {
        this(code, 0, cause);
    }

    private AiPolicyUnavailableException(Code code, int httpStatus, Throwable cause) {
        super("질문 정책 검증을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.", cause);
        this.code = Objects.requireNonNull(code);
        this.httpStatus = httpStatus;
    }

    public static AiPolicyUnavailableException httpFailure(int status) {
        return new AiPolicyUnavailableException(Code.HTTP_ERROR, status, null);
    }

    public Code getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getLogCode() {
        return code == Code.HTTP_ERROR && httpStatus != 0 ? "HTTP_" + httpStatus : code.name();
    }
}
