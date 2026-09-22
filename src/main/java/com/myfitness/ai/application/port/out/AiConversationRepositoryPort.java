package com.myfitness.ai.application.port.out;

import com.myfitness.ai.domain.model.AiConversation;
import java.util.List;
import java.util.Optional;

public interface AiConversationRepositoryPort {
    AiConversation save(AiConversation conversation);
    Optional<AiConversation> findById(Long id);
    List<AiConversation> findAllByUserId(Long userId);
    void delete(AiConversation conversation);
}
