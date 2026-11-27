package com.myfitness.ai.application.port.in;


public record AiFoodPhotoView(
        AiConversationView conversation,
        AiMessageView userMessage,
        AiMessageView assistantMessage
) {}
