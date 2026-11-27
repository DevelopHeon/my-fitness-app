package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.port.in.AiFoodPhotoView;

public record AiFoodPhotoResponse(AiConversationResponse conversation,
        AiMessageResponse userMessage, AiMessageResponse assistantMessage) {
    public static AiFoodPhotoResponse from(AiFoodPhotoView result) {
        return new AiFoodPhotoResponse(AiConversationResponse.from(result.conversation()),
                AiMessageResponse.from(result.userMessage()), AiMessageResponse.from(result.assistantMessage()));
    }
}
