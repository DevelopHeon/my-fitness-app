package com.myfitness.ai.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.ai.application.port.in.AiCoachUseCase;
import com.myfitness.ai.presentation.dto.request.AiConversationRenameRequest;
import com.myfitness.ai.presentation.dto.request.AiMessageRequest;
import com.myfitness.ai.presentation.dto.response.AiConversationResponse;
import com.myfitness.ai.presentation.dto.response.AiMessageResponse;
import com.myfitness.ai.presentation.dto.response.AiSendMessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/conversations")
public class AiCoachController {
    private final AiCoachUseCase aiCoachUseCase;

    public AiCoachController(AiCoachUseCase aiCoachUseCase) {
        this.aiCoachUseCase = aiCoachUseCase;
    }

    @GetMapping
    public List<AiConversationResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return aiCoachUseCase.listConversations(userId).stream()
                .map(AiConversationResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiConversationResponse create(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return AiConversationResponse.from(
                aiCoachUseCase.createConversation(userId));
    }

    @PatchMapping("/{conversationId}")
    public AiConversationResponse rename(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiConversationRenameRequest request) {
        return AiConversationResponse.from(
                aiCoachUseCase.renameConversation(
                        userId,
                        conversationId,
                        request.title()));
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId) {
        aiCoachUseCase.deleteConversation(userId, conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    public List<AiMessageResponse> messages(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId) {
        return aiCoachUseCase.listMessages(userId, conversationId).stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    @PostMapping("/{conversationId}/messages")
    public AiSendMessageResponse send(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiMessageRequest request) {
        return AiSendMessageResponse.from(
                aiCoachUseCase.sendMessage(
                        userId,
                        conversationId,
                        request.toCommand()));
    }
}
