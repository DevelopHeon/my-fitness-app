package com.myfitness.ai.application.support;

import com.myfitness.ai.application.config.AiCoachProperties;
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

    public AiProviderExecutor(
            AiChatGateway chatGateway,
            AiCoachProperties properties) {
        this.chatGateway = chatGateway;
        this.properties = properties;
    }

    public AiModelResponse generate(
            AiContextBundle context,
            List<HistoryMessage> history,
            String message) {
        return chatGateway.chat(request(context, history, message));
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
