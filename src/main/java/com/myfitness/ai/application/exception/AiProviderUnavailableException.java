package com.myfitness.ai.application.exception;

import java.util.Objects;

public class AiProviderUnavailableException extends RuntimeException {
    public enum Code {
        NONE,
        CONFIGURATION_ERROR,
        TIMEOUT,
        TRANSPORT_ERROR,
        HTTP_ERROR,
        INVALID_RESPONSE,
        MODEL_REFUSAL,
        INCOMPLETE_RESPONSE,
        AI_PROVIDER_UNAVAILABLE,
        OTHER
    }

    private final Code errorCode;

    public AiProviderUnavailableException(String message, Throwable cause) {
        this(message, cause, Code.AI_PROVIDER_UNAVAILABLE);
    }

    public AiProviderUnavailableException(String message, Throwable cause, Code errorCode) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode);
    }

    public Code getErrorCode() {
        return errorCode;
    }
}
