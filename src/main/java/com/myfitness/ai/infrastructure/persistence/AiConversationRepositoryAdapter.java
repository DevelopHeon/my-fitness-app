package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.domain.model.AiConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiConversationRepositoryAdapter
        implements AiConversationRepositoryPort {
    private final SpringDataAiConversationRepository repository;

    public AiConversationRepositoryAdapter(
            SpringDataAiConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public AiConversation save(AiConversation conversation) {
        return repository.saveAndFlush(conversation);
    }

    @Override
    public Optional<AiConversation> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<AiConversation> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public void delete(AiConversation conversation) {
        repository.delete(conversation);
        repository.flush();
    }
}
