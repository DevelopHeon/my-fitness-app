package com.myfitness.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.architecture.fixture.application.BadApplication;
import com.myfitness.architecture.fixture.domain.BadDomain;
import com.myfitness.architecture.fixture.domain.NormalDomain;
import com.myfitness.architecture.fixture.infrastructure.DataRepository;
import com.myfitness.architecture.fixture.presentation.BadController;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureRuleDetectionTest {
    @Test
    @DisplayName("AI SDK와 외부 Client 직접 의존 fixture는 실제 운영 검사 rule을 위반한다")
    void detectsDirectAiProviderDependency() {
        JavaClasses fixture =
                new ClassFileImporter()
                        .importClasses(
                                com.myfitness.ai.application.fixture.ForbiddenSdkConsumer.class,
                                com.myfitness.ai.application.fixture.ForbiddenClientConsumer.class);
        assertThat(
                        LayerArchitectureTest.AI_PROVIDER_RULE
                                .evaluate(fixture)
                                .getFailureReport()
                                .getDetails())
                .anySatisfy(
                        detail -> assertThat(detail).contains("ForbiddenSdkConsumer", "ChatModel"));
        assertThat(
                        LayerArchitectureTest.AI_PROVIDER_RULE
                                .evaluate(fixture)
                                .getFailureReport()
                                .getDetails())
                .anySatisfy(
                        detail -> assertThat(detail).contains("ForbiddenClientConsumer", "JevAiPolicyGateway"));
    }

    @Test
    @DisplayName("필수 아키텍처 규칙은 검사 대상이 없으면 실패한다")
    void rejectsEmptyRuleSelection() {
        assertThatThrownBy(
                        () ->
                                LayerArchitectureTest.DOMAIN_RULE.check(
                                        new ClassFileImporter()
                                                .importClasses(DataRepository.class)))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("운영 rule은 정상 Domain을 허용하고 금지 계층 의존을 탐지한다")
    void detectsForbiddenDependenciesWithProductionRules() {
        ClassFileImporter importer = new ClassFileImporter();
        assertThat(
                        LayerArchitectureTest.DOMAIN_RULE
                                .evaluate(importer.importClasses(NormalDomain.class))
                                .hasViolation())
                .isFalse();
        JavaClasses invalid =
                importer.importClasses(
                        BadDomain.class,
                        BadApplication.class,
                        BadController.class,
                        DataRepository.class);
        assertThat(
                        LayerArchitectureTest.DOMAIN_RULE
                                .evaluate(invalid)
                                .getFailureReport()
                                .getDetails())
                .anySatisfy(detail -> assertThat(detail).contains("BadDomain", "BadApplication"));
        assertThat(LayerArchitectureTest.APPLICATION_RULE.evaluate(invalid).hasViolation())
                .isTrue();
        assertThat(
                        LayerArchitectureTest.PRESENTATION_RULE
                                .evaluate(invalid)
                                .getFailureReport()
                                .getDetails())
                .anySatisfy(
                        detail -> assertThat(detail).contains("BadController", "DataRepository"));
    }
}
