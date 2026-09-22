package com.myfitness.ai.application.port.out;

import com.myfitness.ai.domain.model.AiRequestLog;

public interface AiRequestLogRepositoryPort {
    AiRequestLog save(AiRequestLog log);
    void deleteAllByConversationId(Long conversationId);
}
