package com.myfitness.ai.application.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "app.ai.policy")
public class AiPolicyProperties {
    @NotBlank private String model = "jev-1.13.0";
    @NotBlank private String version = "fitness-policy-v1";
    private String apiKey = "";
    @NotNull private URI endpoint = URI.create("https://api.typesafe.ai/v1/systemone");
    @NotNull private Duration requestTimeout = Duration.ofMillis(1500);

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double reviewThreshold = 0.35;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double actionThreshold = 0.70;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double topicConfidence = 0.60;

    @AssertTrue(message = "정책 timeout은 양수이며 review threshold는 action threshold보다 작아야 합니다.")
    public boolean isValidPolicyConfiguration() {
        return requestTimeout != null
                && requestTimeout.toMillis() > 0
                && reviewThreshold < actionThreshold;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public double getReviewThreshold() {
        return reviewThreshold;
    }

    public void setReviewThreshold(double reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }

    public double getActionThreshold() {
        return actionThreshold;
    }

    public void setActionThreshold(double actionThreshold) {
        this.actionThreshold = actionThreshold;
    }

    public double getTopicConfidence() {
        return topicConfidence;
    }

    public void setTopicConfidence(double topicConfidence) {
        this.topicConfidence = topicConfidence;
    }
}
