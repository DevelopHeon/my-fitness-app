package com.myfitness.ai.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiCoachProperties {
    private String promptVersion = "fitness-coach-v1";
    private int historyMessageLimit = 8;
    private int historyCharLimit = 4000;
    private int maxMessageLength = 1000;
    private int titleMaxLength = 60;

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public int getHistoryMessageLimit() {
        return historyMessageLimit;
    }

    public void setHistoryMessageLimit(int historyMessageLimit) {
        this.historyMessageLimit = historyMessageLimit;
    }

    public int getHistoryCharLimit() {
        return historyCharLimit;
    }

    public void setHistoryCharLimit(int historyCharLimit) {
        this.historyCharLimit = historyCharLimit;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public int getTitleMaxLength() {
        return titleMaxLength;
    }

    public void setTitleMaxLength(int titleMaxLength) {
        this.titleMaxLength = titleMaxLength;
    }
}
