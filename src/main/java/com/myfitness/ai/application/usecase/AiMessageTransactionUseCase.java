package com.myfitness.ai.application.usecase;

import com.myfitness.ai.application.usecase.response.AiUserMessageWriteResult;

import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.support.context.AiContextBundle;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import com.myfitness.ai.application.usecase.response.AiMessageResult;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.List;

public interface AiMessageTransactionUseCase {
    List<AiMessage> loadMessagesForOwnedConversation(Long userId, Long conversationId);

    AiUserMessageWriteResult saveUserMessage(Long userId, Long conversationId, String message);

    AiMessage saveRejectedResponse(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            String content,
            AiPolicyRun.Success policy);

    AiMessage saveProviderSuccess(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            AiModelResponse response,
            long latencyMs,
            AiPolicyRun.Success policy);

    void saveProviderFailure(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            String provider,
            String model,
            RuntimeException exception,
            long latencyMs,
            AiPolicyRun.Success policy);

    AiMessageResult classifyUserMessage(
            Long userId, Long conversationId, Long messageId, AiQueryType type);

    void savePolicyFailure(
            Long userId, Long conversationId, AiMessageResult userMessage, AiPolicyRun.Failure failure);
}
