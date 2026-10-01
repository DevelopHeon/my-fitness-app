package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAiMessageRepository
        extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findAllByConversationIdOrderByCreatedAtAscIdAsc(
            Long conversationId);
    @Query("""
            select message from AiMessage message
            join AiRequestLog log on (log.userMessageId = message.id or log.assistantMessageId = message.id)
            where message.conversationId = :conversationId and log.conversationId = :conversationId
              and log.requestKind = 'TEXT' and message.messageKind = 'TEXT'
              and log.status = :status and log.assistantMessageId is not null
              and (log.policyDecision is null or log.policyDecision = 'ALLOW')
            order by log.userMessageId asc, case when message.id = log.userMessageId then 0 else 1 end asc
            """)
    List<AiMessage> findAcceptedByConversationId(@Param("conversationId") Long conversationId,
            @Param("status") AiRequestStatus status);
    void deleteAllByConversationId(Long conversationId);
}
