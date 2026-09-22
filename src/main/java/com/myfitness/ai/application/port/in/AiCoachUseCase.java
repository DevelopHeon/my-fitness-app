package com.myfitness.ai.application.port.in;

import com.myfitness.ai.application.command.AiMessageCommand;
import com.myfitness.ai.application.result.AiConversationResult;
import com.myfitness.ai.application.result.AiMessageResult;
import com.myfitness.ai.application.result.AiSendMessageResult;
import java.util.List;

public interface AiCoachUseCase {
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

    AiSendMessageResult sendMessage(
            Long userId,
            Long conversationId,
            AiMessageCommand command);
}
