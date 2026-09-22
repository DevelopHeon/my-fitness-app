package com.myfitness.ai.application.port.out;

import com.myfitness.ai.domain.model.AiMessage;
import java.util.List;

public interface AiMessageRepositoryPort {
    AiMessage save(AiMessage message);
    List<AiMessage> findAllByConversationId(Long conversationId);
    void deleteAllByConversationId(Long conversationId);
}
