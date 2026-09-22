package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiMessageRole;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiHistorySelector {
    private final AiCoachProperties properties;

    public AiHistorySelector(AiCoachProperties properties) {
        this.properties = properties;
    }

    public AiQueryType latestUserQueryType(List<AiMessage> messages) {
        return messages.stream()
                .filter(message ->
                        message.getRole() == AiMessageRole.USER)
                .map(AiMessage::getQueryType)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    public List<HistoryMessage> select(
            List<AiMessage> messages,
            AiQueryType currentType) {
        int messageLimit = Math.max(
                0,
                properties.getHistoryMessageLimit());
        int charLimit = Math.max(
                0,
                properties.getHistoryCharLimit());

        List<HistoryMessage> reversed = collectReversed(
                messages,
                currentType,
                messageLimit,
                charLimit);

        List<HistoryMessage> result = new ArrayList<>();
        for (int index = reversed.size() - 1; index >= 0; index--) {
            result.add(reversed.get(index));
        }
        return List.copyOf(result);
    }

    private static List<HistoryMessage> collectReversed(
            List<AiMessage> messages,
            AiQueryType currentType,
            int messageLimit,
            int charLimit) {
        List<HistoryMessage> reversed = new ArrayList<>();
        int chars = 0;

        for (int index = messages.size() - 1;
                index >= 0 && reversed.size() < messageLimit;
                index--) {
            AiMessage message = messages.get(index);
            if (!isRelevant(message.getQueryType(), currentType)) {
                continue;
            }
            if (chars >= charLimit) {
                break;
            }

            String content = trimToRemaining(
                    message.getContent(),
                    charLimit - chars);
            reversed.add(new HistoryMessage(
                    message.getRole(),
                    content));
            chars += content.length();
        }
        return reversed;
    }

    private static String trimToRemaining(
            String content,
            int remaining) {
        if (content.length() <= remaining) {
            return content;
        }
        return content.substring(
                Math.max(0, content.length() - remaining));
    }

    private static boolean isRelevant(
            AiQueryType historyType,
            AiQueryType currentType) {
        if (historyType == null
                || historyType == AiQueryType.OUT_OF_SCOPE) {
            return false;
        }
        if (currentType == AiQueryType.COMPOSITE
                || currentType == AiQueryType.GENERAL_FITNESS) {
            return true;
        }
        return historyType == currentType
                || historyType == AiQueryType.COMPOSITE;
    }
}
