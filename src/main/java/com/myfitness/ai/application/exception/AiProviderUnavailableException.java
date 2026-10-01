package com.myfitness.ai.application.exception;

public class AiProviderUnavailableException extends RuntimeException {
    private final String errorCode;

    public AiProviderUnavailableException(String message) {
        this(message, null, "AI_PROVIDER_UNAVAILABLE");
    }

    public AiProviderUnavailableException(String message, Throwable cause) {
        this(message, cause, "AI_PROVIDER_UNAVAILABLE");
    }

    public AiProviderUnavailableException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
