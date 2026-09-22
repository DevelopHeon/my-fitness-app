package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.domain.model.AiRequestLog;
import org.springframework.stereotype.Repository;

@Repository
public class AiRequestLogRepositoryAdapter
        implements AiRequestLogRepositoryPort {
    private final SpringDataAiRequestLogRepository repository;

    public AiRequestLogRepositoryAdapter(
            SpringDataAiRequestLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AiRequestLog save(AiRequestLog log) {
        return repository.saveAndFlush(log);
    }

    @Override
    public void deleteAllByConversationId(Long conversationId) {
        repository.deleteAllByConversationId(conversationId);
        repository.flush();
    }
}
