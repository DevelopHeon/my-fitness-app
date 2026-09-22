package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.domain.model.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiRequestLogRepository
        extends JpaRepository<AiRequestLog, Long> {
    void deleteAllByConversationId(Long conversationId);
}
