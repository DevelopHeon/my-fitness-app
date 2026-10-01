package com.myfitness.ai.application.dto.response;

import com.myfitness.ai.domain.model.AiConversation;

public record AiFoodPhotoResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage
) {
    public static AiFoodPhotoResult from(
            AiConversation conversation,
            AiMessageResult userMessage,
            AiMessageResult assistantMessage) {
        return new AiFoodPhotoResult(
                AiConversationResult.from(conversation),
                userMessage,
                assistantMessage);
    }
}
