package com.myfitness.ai.application.service;

import com.myfitness.ai.application.usecase.request.AiClientContext;
import com.myfitness.ai.application.usecase.request.AiMessageCommand;
import com.myfitness.ai.application.usecase.response.AiConversationResult;
import com.myfitness.ai.application.usecase.response.AiFoodPhotoResult;
import com.myfitness.ai.application.usecase.response.AiMessageResult;
import com.myfitness.ai.application.usecase.response.AiSendMessageResult;
import com.myfitness.ai.application.port.in.AiCoachPort;
import com.myfitness.ai.application.port.in.AiConversationView;
import com.myfitness.ai.application.port.in.AiFoodPhotoView;
import com.myfitness.ai.application.port.in.AiMessageView;
import com.myfitness.ai.application.port.in.AiSendMessageView;
import com.myfitness.ai.application.port.in.FoodPhotoPort;
import com.myfitness.ai.application.usecase.AiConversationUseCase;
import com.myfitness.ai.application.usecase.AiChatMessageUseCase;
import com.myfitness.ai.application.usecase.FoodPhotoUseCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiCoachPortService implements AiCoachPort, FoodPhotoPort {
    private final AiConversationUseCase conversations;
    private final AiChatMessageUseCase messages;
    private final FoodPhotoUseCase foodPhotos;

    public AiCoachPortService(
            AiConversationUseCase conversations, AiChatMessageUseCase messages, FoodPhotoUseCase foodPhotos) {
        this.conversations = conversations;
        this.messages = messages;
        this.foodPhotos = foodPhotos;
    }

    @Override
    public List<AiConversationView> listConversations(Long userId) {
        return conversations.listConversations(userId).stream().map(AiCoachPortService::toConversation).toList();
    }

    @Override
    public AiConversationView createConversation(Long userId) {
        return toConversation(conversations.createConversation(userId));
    }

    @Override
    public AiConversationView renameConversation(
            Long userId,
            Long conversationId,
            String title) {
        return toConversation(conversations.renameConversation(userId, conversationId, title));
    }

    @Override
    public void deleteConversation(Long userId, Long conversationId) {
        conversations.deleteConversation(userId, conversationId);
    }

    @Override
    public List<AiMessageView> listMessages(
            Long userId,
            Long conversationId) {
        return conversations.listMessages(userId, conversationId).stream().map(AiCoachPortService::toMessage).toList();
    }

    @Override
    public AiSendMessageView sendMessage(
            Long userId,
            Long conversationId,
            MessageInput command) {
        return toSend(messages.sendMessage(userId, conversationId, toCommand(command)));
    }

    @Override
    public AiFoodPhotoView analyze(Long userId, Long conversationId, PhotoInput input) {
        AiFoodPhotoResult result = foodPhotos.analyze(
                userId, conversationId, input.bytes(), input.contentType());
        return new AiFoodPhotoView(
                toConversation(result.conversation()), toMessage(result.userMessage()), toMessage(result.assistantMessage()));
    }

    private static AiMessageCommand toCommand(MessageInput input) {
        if (input == null) {
            return null;
        }
        ClientContext context = input.context();
        return new AiMessageCommand(input.message(), context == null ? null : new AiClientContext(
                context.screen(), context.selectedDate(), context.resourceId()));
    }

    private static AiConversationView toConversation(AiConversationResult value) {
        return new AiConversationView(value.id(), value.title(), value.createdAt(), value.updatedAt());
    }

    private static AiMessageView toMessage(AiMessageResult value) {
        AiMessageResult.PhotoResult photo = value.foodPhotoResult();
        AiMessageView.PhotoView photoView = photo == null ? null : new AiMessageView.PhotoView(
                photo.status(), photo.items().stream().map(item -> new AiMessageView.PhotoItem(
                        item.foodName(), item.servingDescription(), item.caloriesPerServing())).toList());
        return new AiMessageView(
                value.id(), value.role(), value.queryType(), value.content(), value.createdAt(), photoView);
    }

    private static AiSendMessageView toSend(AiSendMessageResult value) {
        return new AiSendMessageView(
                toConversation(value.conversation()), toMessage(value.userMessage()),
                toMessage(value.assistantMessage()), value.providerCalled(), value.policyDecision());
    }
}
