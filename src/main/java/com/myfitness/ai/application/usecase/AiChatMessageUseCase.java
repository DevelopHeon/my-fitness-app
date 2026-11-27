package com.myfitness.ai.application.usecase;

import com.myfitness.ai.application.usecase.request.AiMessageCommand;
import com.myfitness.ai.application.usecase.response.AiSendMessageResult;

public interface AiChatMessageUseCase {
    AiSendMessageResult sendMessage(Long userId, Long conversationId, AiMessageCommand command);
}
