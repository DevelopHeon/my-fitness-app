package com.myfitness.ai.application.exception;

public class AiConversationNotFoundException extends RuntimeException {
    public AiConversationNotFoundException() {
        super("AI Conversation을 찾을 수 없습니다.");
    }
}
