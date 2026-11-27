package com.myfitness.ai.domain.repository;

import com.myfitness.ai.domain.model.AiMessage;
import java.util.List;
import java.util.Optional;

public interface AiMessageRepository {
    Optional<AiMessage> findByIdAndConversationId(Long id, Long conversationId);
    AiMessage save(AiMessage message);
    List<AiMessage> findAllByConversationId(Long conversationId);
    List<AiMessage> findAcceptedByConversationId(Long conversationId);
    void deleteAllByConversationId(Long conversationId);
}
