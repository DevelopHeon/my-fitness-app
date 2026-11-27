package com.myfitness.ai.application.usecase;

import com.myfitness.ai.application.usecase.response.AiConversationResult;
import com.myfitness.ai.application.usecase.response.AiMessageResult;
import com.myfitness.ai.domain.model.AiConversation;
import java.util.List;

public interface AiConversationUseCase {
    List<AiConversationResult> listConversations(Long userId);

    AiConversationResult createConversation(Long userId);

    AiConversationResult renameConversation(
            Long userId,
            Long conversationId,
            String title);

    void deleteConversation(Long userId, Long conversationId);

    List<AiMessageResult> listMessages(
            Long userId,
            Long conversationId);

    AiConversation requireOwned(Long userId, Long conversationId);
}
