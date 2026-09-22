package com.myfitness.ai.application.exception;

public class AiConversationAccessException extends RuntimeException {
    public AiConversationAccessException() {
        super("다른 사용자의 AI Conversation에는 접근할 수 없습니다.");
    }
}
