package com.myfitness.ai.infrastructure.client;

import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.domain.model.AiMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SpringAiChatGateway implements AiChatGateway {
    private static final String NL = System.lineSeparator();

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final Environment environment;

    public SpringAiChatGateway(
            ObjectProvider<ChatModel> chatModelProvider,
            Environment environment) {
        this.chatModelProvider = chatModelProvider;
        this.environment = environment;
    }

    @Override
    public AiModelResponse chat(AiModelRequest request) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new AiProviderUnavailableException(
                    "AI Provider가 설정되지 않았습니다. AI_PROVIDER를 설정해 주세요.");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(request.systemPrompt()));

        if (request.context() != null && !request.context().isBlank()) {
            messages.add(new SystemMessage(
                    "아래 내용은 서버가 사용자 기록에서 계산한 Context입니다."
                            + NL
                            + request.context()));
        }

        for (HistoryMessage history : request.history()) {
            if (history.role() == AiMessageRole.USER) {
                messages.add(new UserMessage(history.content()));
            } else {
                messages.add(new AssistantMessage(history.content()));
            }
        }

        messages.add(new UserMessage(request.userMessage()));

        ChatResponse response = chatModel.call(new Prompt(messages));
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null
                || response.getResult().getOutput().getText() == null
                || response.getResult().getOutput().getText().isBlank()) {
            throw new AiProviderUnavailableException(
                    "AI Provider가 빈 응답을 반환했습니다.");
        }

        Usage usage = response.getMetadata() == null
                ? null
                : response.getMetadata().getUsage();
        if (usage instanceof EmptyUsage) {
            usage = null;
        }

        return new AiModelResponse(
                response.getResult().getOutput().getText().trim(),
                provider(),
                model(),
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens());
    }

    @Override
    public String provider() {
        return environment.getProperty(
                "spring.ai.model.chat",
                "none");
    }

    @Override
    public String model() {
        return switch (provider()) {
            case "openai" -> environment.getProperty(
                    "spring.ai.openai.chat.model",
                    "gpt-4o-mini");
            case "ollama" -> environment.getProperty(
                    "spring.ai.ollama.chat.model",
                    "unknown");
            default -> "none";
        };
    }
}
