package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.context.AiContextBundle;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.prompt.AiSystemPrompt;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.List;
import com.myfitness.ai.application.policy.AiPolicyRun;
import org.springframework.stereotype.Component;

@Component
public class AiProviderExecutor {
    private final AiChatGateway chatGateway;
    private final AiMessageTransactionService transactionService;
    private final AiCoachProperties properties;

    public AiProviderExecutor(
            AiChatGateway chatGateway,
            AiMessageTransactionService transactionService,
            AiCoachProperties properties) {
        this.chatGateway = chatGateway;
        this.transactionService = transactionService;
        this.properties = properties;
    }

    public AiMessage generate(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            List<HistoryMessage> history,
            String message,
            AiPolicyRun.Success policy) {
        long started = System.nanoTime();
        try {
            AiModelResponse response = chatGateway.chat(
                    request(context, history, message));
            return transactionService.saveProviderSuccess(
                    userId,
                    conversationId,
                    userMessage,
                    queryType,
                    context,
                    response,
                    elapsedMillis(started), policy);
        } catch (RuntimeException exception) {
            transactionService.saveProviderFailure(
                    userId,
                    conversationId,
                    userMessage,
                    queryType,
                    context,
                    chatGateway.provider(),
                    chatGateway.model(),
                    exception,
                    elapsedMillis(started), policy);
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
