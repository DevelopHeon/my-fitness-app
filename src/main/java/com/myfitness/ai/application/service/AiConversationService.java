package com.myfitness.ai.application.service;

import com.myfitness.ai.application.dto.response.AiConversationResult;
import com.myfitness.ai.application.dto.response.AiMessageResult;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.support.AiMessageResultMapper;
import com.myfitness.ai.domain.model.AiConversation;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiConversationService {
    private final AiConversationRepositoryPort conversationRepository;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final AiMessageResultMapper messageMapper;
    private final Clock clock = Clock.systemUTC();

    public AiConversationService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiMessageResultMapper messageMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.messageMapper = messageMapper;
    }

    public List<AiConversationResult> listConversations(Long userId) {
        return conversationRepository.findAllByUserId(userId).stream()
                .map(AiConversationResult::from)
                .toList();
    }

    @Transactional
    public AiConversationResult createConversation(Long userId) {
        AiConversation conversation =
                conversationRepository.save(AiConversation.create(userId, clock.instant()));
        return AiConversationResult.from(conversation);
    }

    @Transactional
    public AiConversationResult renameConversation(Long userId, Long conversationId, String title) {
        AiConversation conversation = requireOwned(userId, conversationId);
        conversation.rename(title, clock.instant());
        return AiConversationResult.from(conversationRepository.save(conversation));
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conversation = requireOwned(userId, conversationId);
        requestLogRepository.deleteAllByConversationId(conversationId);
        messageRepository.deleteAllByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    public List<AiMessageResult> listMessages(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        return messageRepository.findAllByConversationId(conversationId).stream()
                .map(messageMapper::map)
                .toList();
    }

    public AiConversation requireOwned(Long userId, Long conversationId) {
        AiConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(AiConversationNotFoundException::new);
        if (!conversation.belongsTo(userId)) {
            throw new AiConversationAccessException();
        }
        return conversation;
    }

}
