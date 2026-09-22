package com.myfitness.ai.infrastructure.persistence;

import com.myfitness.ai.domain.model.AiConversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiConversationRepository
        extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}
