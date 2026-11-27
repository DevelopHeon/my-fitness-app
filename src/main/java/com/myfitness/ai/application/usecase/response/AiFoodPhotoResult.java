package com.myfitness.ai.application.usecase.response;

public record AiFoodPhotoResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage
) {}
