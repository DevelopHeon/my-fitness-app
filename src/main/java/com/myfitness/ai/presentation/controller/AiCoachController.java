package com.myfitness.ai.presentation.controller;

import com.myfitness.ai.application.port.in.FoodPhotoPort.PhotoInput;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException.Reason;
import com.myfitness.ai.application.port.in.AiCoachPort;
import com.myfitness.ai.application.port.in.FoodPhotoPort;
import com.myfitness.ai.presentation.dto.request.AiConversationRenameRequest;
import com.myfitness.ai.presentation.dto.request.AiMessageRequest;
import com.myfitness.ai.presentation.dto.response.AiConversationResponse;
import com.myfitness.ai.presentation.dto.response.AiFoodPhotoResponse;
import com.myfitness.ai.presentation.dto.response.AiMessageResponse;
import com.myfitness.ai.presentation.dto.response.AiSendMessageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/conversations")
public class AiCoachController {
    private final AiCoachPort aiCoachPort;
    private final FoodPhotoPort foodPhotoPort;

    public AiCoachController(AiCoachPort aiCoachPort,
            FoodPhotoPort foodPhotoPort) {
        this.aiCoachPort = aiCoachPort;
        this.foodPhotoPort = foodPhotoPort;
    }

    @GetMapping
    public List<AiConversationResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return aiCoachPort.listConversations(userId).stream()
                .map(AiConversationResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiConversationResponse create(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return AiConversationResponse.from(
                aiCoachPort.createConversation(userId));
    }

    @PatchMapping("/{conversationId}")
    public AiConversationResponse rename(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiConversationRenameRequest request) {
        return AiConversationResponse.from(
                aiCoachPort.renameConversation(
                        userId,
                        conversationId,
                        request.title()));
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId) {
        aiCoachPort.deleteConversation(userId, conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    public List<AiMessageResponse> messages(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId) {
        return aiCoachPort.listMessages(userId, conversationId).stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    @PostMapping(value = "/{conversationId}/food-photos", consumes = "multipart/form-data")
    public AiFoodPhotoResponse photo(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId,
            @RequestPart("image")
            List<MultipartFile> images) throws IOException {
        if (images.size() != 1) {
            throw new InvalidFoodPhotoException(Reason.INVALID);
        }
        MultipartFile image = images.getFirst();
        return AiFoodPhotoResponse.from(foodPhotoPort.analyze(
                userId, conversationId, new PhotoInput(
                        image.getBytes(), image.getContentType())));
    }

    @PostMapping("/{conversationId}/messages")
    public AiSendMessageResponse send(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiMessageRequest request) {
        return AiSendMessageResponse.from(
                aiCoachPort.sendMessage(
                        userId,
                        conversationId,
                        request.toCommand()));
    }
}
