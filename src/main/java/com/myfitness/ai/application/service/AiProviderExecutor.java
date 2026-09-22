package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.context.AiContextBundle;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.prompt.AiSystemPrompt;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiProviderExecutor {
    private final AiChatGateway chatGateway;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final AiCoachProperties properties;
    private final Clock clock;

    @Autowired
    public AiProviderExecutor(
            AiChatGateway chatGateway,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiCoachProperties properties) {
        this(
                chatGateway,
                messageRepository,
                requestLogRepository,
                properties,
                Clock.systemUTC());
    }

    AiProviderExecutor(
            AiChatGateway chatGateway,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiCoachProperties properties,
            Clock clock) {
        this.chatGateway = chatGateway;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public AiMessage generate(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            List<HistoryMessage> history,
            String message) {
        long started = System.nanoTime();
        try {
            AiModelResponse response = chatGateway.chat(
                    request(context, history, message));
            AiMessage assistantMessage = saveAssistant(
                    conversationId,
                    queryType,
                    response);
            saveSuccessLog(
                    userId,
                    conversationId,
                    userMessage,
                    assistantMessage,
                    queryType,
                    context,
                    response,
                    elapsedMillis(started));
            return assistantMessage;
        } catch (RuntimeException exception) {
            saveFailureLog(
                    userId,
                    conversationId,
                    userMessage,
                    queryType,
                    context,
                    exception,
                    elapsedMillis(started));
            throw providerException(exception);
        }
    }

    private AiModelRequest request(
            AiContextBundle context,
            List<HistoryMessage> history,
            String message) {
        return new AiModelRequest(
                AiSystemPrompt.create(properties.getPromptVersion()),
                context.text(),
                history,
                message);
    }

    private AiMessage saveAssistant(
            Long conversationId,
            AiQueryType queryType,
            AiModelResponse response) {
        return messageRepository.save(
                AiMessage.assistant(
                        conversationId,
                        queryType,
                        response.content(),
                        clock.instant()));
    }

    private void saveSuccessLog(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiMessage assistantMessage,
            AiQueryType queryType,
            AiContextBundle context,
            AiModelResponse response,
            long latencyMs) {
        requestLogRepository.save(AiRequestLog.success(
                userId,
                conversationId,
                userMessage.getId(),
                assistantMessage.getId(),
                queryType,
                response.provider(),
                response.model(),
                properties.getPromptVersion(),
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens(),
                latencyMs,
                context.typeNames(),
                clock.instant()));
    }

    private void saveFailureLog(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            RuntimeException exception,
            long latencyMs) {
        requestLogRepository.save(AiRequestLog.failed(
                userId,
                conversationId,
                userMessage.getId(),
                queryType,
                chatGateway.provider(),
                chatGateway.model(),
                properties.getPromptVersion(),
                latencyMs,
                exception.getClass().getSimpleName(),
                context.typeNames(),
                clock.instant()));
    }

    private static AiProviderUnavailableException providerException(
            RuntimeException exception) {
        if (exception instanceof AiProviderUnavailableException provider) {
            return provider;
        }
        return new AiProviderUnavailableException(
                "AI 응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.",
                exception);
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
