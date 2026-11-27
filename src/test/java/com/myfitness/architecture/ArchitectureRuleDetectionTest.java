package com.myfitness.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myfitness.architecture.fixture.application.BadApplication;
import com.myfitness.architecture.fixture.domain.BadDomain;
import com.myfitness.architecture.fixture.domain.NormalDomain;
import com.myfitness.architecture.fixture.infrastructure.DataRepository;
import com.myfitness.architecture.fixture.presentation.BadController;
import com.myfitness.architecture.fixture.application.service.ForbiddenPortService;
import com.myfitness.architecture.fixture.application.service.ForeignPortConsumer;
import com.myfitness.architecture.fixture.application.service.NormalPortService;
import com.myfitness.architecture.fixture.domain.repository.ForbiddenRepository;
import com.myfitness.architecture.fixture.domain.repository.NormalRepository;
import com.myfitness.architecture.fixture.application.port.in.FixturePort;
import com.myfitness.architecture.fixture.presentation.PortFixtureController;
import com.myfitness.architecture.fixture.application.dto.response.LegacyResult;
import com.myfitness.architecture.fixture.application.service.ServiceDependencyFixture;
import com.myfitness.architecture.fixture.application.usecase.response.CurrentResult;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import com.tngtech.archunit.lang.ArchRule;

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

    @Test
    @DisplayName("공개 진입과 Domain Repository 검사 규칙은 정상 fixture를 허용하고 위반을 탐지한다")
    void detectsPortBoundaryViolations() {
        assertBoundary(
                PortBoundaryArchitectureTest.PORT_SERVICE_RULE,
                NormalPortService.class, ForbiddenPortService.class,
                "NormalDomain", "NormalRepository", "FixtureOutputPort", "NormalPortService", "Transactional");
        assertBoundary(
                PortBoundaryArchitectureTest.INPUT_PORT_IMPLEMENTATION_RULE,
                NormalPortService.class, PortFixtureController.BypassPortImplementation.class, "PortService");
        assertBoundary(
                PortBoundaryArchitectureTest.INPUT_PORT_RULE,
                FixturePort.class, FixturePort.ForbiddenPort.class, "InternalUseCase");
        assertBoundary(
                PortBoundaryArchitectureTest.INPUT_ADAPTER_RULE,
                PortFixtureController.class, PortFixtureController.BypassController.class, "InternalUseCase");
        assertBoundary(
                PortBoundaryArchitectureTest.DOMAIN_REPOSITORY_RULE,
                NormalRepository.class, ForbiddenRepository.class, "JpaRepository");
        assertBoundary(
                PortBoundaryArchitectureTest.moduleAdapterRule(
                        "com.myfitness.architecture.fixture", "com.myfitness.body.."),
                NormalPortService.class, ForeignPortConsumer.class, "BodyInsightQuery");
    }

    @Test
    @DisplayName("직접 업무 구현 참조와 이전 DTO 위치를 실제 경계 규칙이 탐지한다")
    void detectsServiceAndDtoBoundaryViolations() {
        assertBoundary(
                PortBoundaryArchitectureTest.BUSINESS_SERVICE_DEPENDENCY_RULE,
                ServiceDependencyFixture.UsesContract.class,
                ServiceDependencyFixture.BypassesContract.class,
                "BusinessService");
        assertBoundary(
                PortBoundaryArchitectureTest.LEGACY_DTO_PACKAGE_RULE,
                CurrentResult.class, LegacyResult.class, "application.dto.response");
    }

    private void assertBoundary(ArchRule rule, Class<?> valid, Class<?> invalid, String... dependencies) {
        ClassFileImporter importer = new ClassFileImporter();
        rule.check(importer.importClasses(valid));
        AssertionError error = assertThrows(AssertionError.class, () -> rule.check(importer.importClasses(invalid)));
        assertThat(error.getMessage()).contains("Architecture Violation", invalid.getSimpleName()).contains(dependencies);
    }
}
