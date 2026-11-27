package com.myfitness.ai.domain.repository;

import com.myfitness.ai.domain.model.AiRequestLog;

public interface AiRequestLogRepository {
    AiRequestLog save(AiRequestLog log);
    void deleteAllByConversationId(Long conversationId);
}
