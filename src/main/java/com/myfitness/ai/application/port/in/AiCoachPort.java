package com.myfitness.ai.application.port.in;

import java.util.List;
import java.time.LocalDate;

public interface AiCoachPort {
    List<AiConversationView> listConversations(Long userId);

    AiConversationView createConversation(Long userId);

    AiConversationView renameConversation(
            Long userId,
            Long conversationId,
            String title);

    void deleteConversation(Long userId, Long conversationId);

    List<AiMessageView> listMessages(
            Long userId,
            Long conversationId);

    AiSendMessageView sendMessage(
            Long userId,
            Long conversationId,
            MessageInput command);

    record MessageInput(String message, ClientContext context) {}

    record ClientContext(String screen, LocalDate selectedDate, Long resourceId) {}
}
