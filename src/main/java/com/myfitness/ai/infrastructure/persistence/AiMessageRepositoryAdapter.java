package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.domain.repository.AiMessageRepository;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiMessageRepositoryAdapter
        implements AiMessageRepository {
    private final SpringDataAiMessageRepository repository;

    public AiMessageRepositoryAdapter(
            SpringDataAiMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AiMessage> findByIdAndConversationId(Long id, Long conversationId) {
        return repository.findByIdAndConversationId(id, conversationId);
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
    public List<AiMessage> findAcceptedByConversationId(Long conversationId) {
        return repository.findAcceptedByConversationId(conversationId, AiRequestStatus.SUCCESS);
    }

    @Override
    public void deleteAllByConversationId(Long conversationId) {
        repository.deleteAllByConversationId(conversationId);
        repository.flush();
    }
}
