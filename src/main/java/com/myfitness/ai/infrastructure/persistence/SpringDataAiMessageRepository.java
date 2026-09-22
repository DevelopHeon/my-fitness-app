package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.domain.model.AiMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiMessageRepository
        extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findAllByConversationIdOrderByCreatedAtAscIdAsc(
            Long conversationId);
    void deleteAllByConversationId(Long conversationId);
}
