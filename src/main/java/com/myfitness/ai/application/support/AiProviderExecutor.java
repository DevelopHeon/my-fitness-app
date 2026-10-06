package com.myfitness.ai.application.support;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.support.context.AiContextBundle;
import com.myfitness.ai.application.support.prompt.AiSystemPrompt;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiProviderExecutor {
    private final AiChatGateway chatGateway;
    private final AiCoachProperties properties;
    private final AiMetrics metrics;

    public AiProviderExecutor(
            AiChatGateway chatGateway,
            AiCoachProperties properties,
            AiMetrics metrics) {
        this.chatGateway = chatGateway;
        this.properties = properties;
        this.metrics = metrics;
    }

    public AiModelResponse generate(
            AiContextBundle context,
            List<HistoryMessage> history,
            String message) {
        long started = System.nanoTime();
        AiModelResponse response;
        try {
            response = chatGateway.chat(request(context, history, message));
        } catch (RuntimeException exception) {
            String error = exception instanceof AiProviderUnavailableException failure
                    ? failure.getErrorCode() : "OTHER";
            metrics.provider("chat", provider(), "FAILURE", error, elapsed(started));
            throw exception;
        }
        metrics.provider("chat", response.provider(), "SUCCESS", "NONE", elapsed(started));
        metrics.tokens("chat", response.provider(), response.inputTokens(), response.outputTokens());
        return response;
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    public String provider() {
        return chatGateway.provider();
    }

    public String model() {
        return chatGateway.model();
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
}
