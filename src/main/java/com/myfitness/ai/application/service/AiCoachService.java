package com.myfitness.ai.application.service;

import com.myfitness.ai.application.dto.request.AiMessageCommand;
import com.myfitness.ai.application.dto.response.AiConversationResult;
import com.myfitness.ai.application.dto.response.AiMessageResult;
import com.myfitness.ai.application.dto.response.AiSendMessageResult;
import com.myfitness.ai.application.port.in.AiCoachUseCase;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiCoachService implements AiCoachUseCase {
    private final AiConversationService conversations;
    private final AiChatMessageService chatMessages;

    public AiCoachService(AiConversationService conversations, AiChatMessageService chatMessages) {
        this.conversations = conversations;
        this.chatMessages = chatMessages;
    }

    @Override
    public List<AiConversationResult> listConversations(Long userId) {
        return conversations.listConversations(userId);
    }

    @Override
    @Transactional
    public AiConversationResult createConversation(Long userId) {
        return conversations.createConversation(userId);
    }

    @Override
    @Transactional
    public AiConversationResult renameConversation(Long userId, Long conversationId, String title) {
        return conversations.renameConversation(userId, conversationId, title);
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        conversations.deleteConversation(userId, conversationId);
    }

    @Override
    public List<AiMessageResult> listMessages(Long userId, Long conversationId) {
        return conversations.listMessages(userId, conversationId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiSendMessageResult sendMessage(Long userId, Long conversationId, AiMessageCommand command) {
        return chatMessages.sendMessage(userId, conversationId, command);
    }
}
