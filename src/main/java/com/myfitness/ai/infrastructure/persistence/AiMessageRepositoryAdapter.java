package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.domain.model.AiMessage;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiMessageRepositoryAdapter
        implements AiMessageRepositoryPort {
    private final SpringDataAiMessageRepository repository;

    public AiMessageRepositoryAdapter(
            SpringDataAiMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public AiMessage save(AiMessage message) {
        return repository.saveAndFlush(message);
    }

    @Override
    public List<AiMessage> findAllByConversationId(Long conversationId) {
        return repository.findAllByConversationIdOrderByCreatedAtAscIdAsc(
                conversationId);
    }

    @Override
    public void deleteAllByConversationId(Long conversationId) {
        repository.deleteAllByConversationId(conversationId);
        repository.flush();
    }
}
