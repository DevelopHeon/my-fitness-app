package com.myfitness.ai.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.config.AiPolicyProperties;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

class AiPolicyPropertiesTest {
    @Test
    @DisplayName("기본 JEV 설정과 정책 임계값은 유효하다")
    void acceptsDefaultPolicyConfiguration() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new AiPolicyProperties())).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "timeout",
                "review-range",
                "action-range",
                "ordering",
                "topic-range",
                "model",
                "version"
            })
    @DisplayName("잘못된 deadline·임계값·모드·버전 설정은 시작 검증에서 거절된다")
    void rejectsInvalidPolicyConfiguration(String mutation) {
        AiPolicyProperties properties = new AiPolicyProperties();
        switch (mutation) {
            case "timeout" -> properties.setRequestTimeout(Duration.ZERO);
            case "review-range" -> properties.setReviewThreshold(-0.1);
            case "action-range" -> properties.setActionThreshold(1.1);
            case "ordering" -> properties.setReviewThreshold(properties.getActionThreshold());
            case "topic-range" -> properties.setTopicConfidence(1.1);
            case "model" -> properties.setModel("");
            case "version" -> properties.setVersion("");
        }
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
        }
    }
}
