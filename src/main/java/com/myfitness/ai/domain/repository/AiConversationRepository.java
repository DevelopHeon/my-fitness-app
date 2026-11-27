package com.myfitness.ai.domain.repository;

import com.myfitness.ai.domain.model.AiConversation;
import java.util.List;
import java.util.Optional;

public interface AiConversationRepository {
    AiConversation save(AiConversation conversation);
    Optional<AiConversation> findById(Long id);
    List<AiConversation> findAllByUserId(Long userId);
    void delete(AiConversation conversation);
}
