package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/** 모듈 내부의 의존성 방향과 Port 계약을 검증한다. 모듈 간 공개 경계는 Modulith가 담당한다. */
class LayerArchitectureTest {
    static final ArchRule DOMAIN_RULE =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..presentation..", "..infrastructure..")
                    .because("Domain은 업무 모델을 유지하며 외부 계층 호출은 Application으로 옮긴다.");
    static final ArchRule APPLICATION_RULE =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..presentation..", "..infrastructure..")
                    .because("Application은 외부 구현 대신 Out Port에 의존해야 한다.");
    static final ArchRule PRESENTATION_RULE =
            noClasses()
                    .that()
                    .resideInAPackage("..presentation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Controller는 Repository 대신 Application In Port를 호출한다.");

    static final ArchRule AI_PROVIDER_RULE =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.myfitness.ai.domain..",
                            "com.myfitness.ai.application..",
                            "com.myfitness.ai.presentation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.ai..",
                            "com.myfitness.ai.infrastructure.client..")
                    .because("외부 AI 연동은 Infrastructure에 두고 Application Out Port로 호출한다.");

    static final ArchRule SUPPORT_RULE =
            noClasses()
                    .that()
                    .resideInAPackage("..application.support..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..application.service..")
                    .because("Support는 내부 협력 기능이며 유스케이스와 transaction 조율은 Service가 담당한다.");

    static final ArchRule PRESENTATION_IMPLEMENTATION_RULE =
            noClasses()
                    .that()
                    .resideInAPackage("..presentation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application.service..", "..application.support..")
                    .because("Presentation은 내부 구현을 우회 호출하지 않고 In Port를 사용한다.");

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.myfitness");

    @Test
    @DisplayName("아키텍처 검사는 운영 클래스를 포함하고 테스트 클래스는 제외한다")
    void importsOnlyProductionClasses() {
        assertThat(CLASSES.contain(com.myfitness.ai.application.support.policy.AiPolicyGuard.class))
                .isTrue();
        assertThat(
                        CLASSES.contain(
                                com.myfitness.ai.infrastructure.client.SpringAiChatGateway.class))
                .isTrue();
        assertThat(CLASSES.contain(LayerArchitectureTest.class)).isFalse();
        for (String layer : List.of("domain", "application", "presentation", "infrastructure")) {
            assertThat(
                            CLASSES.stream()
                                    .filter(
                                            type ->
                                                    type.getPackageName()
                                                            .contains("." + layer + ".")))
                    .as("운영 %s 검사 대상", layer)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("AI의 내부 계층은 Spring AI SDK와 외부 AI Client를 직접 참조하지 않는다")
    void aiInnerLayersDoNotDependOnProviderImplementations() {
        AI_PROVIDER_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Domain 계층은 Application, Presentation, Infrastructure 계층에 의존하지 않는다")
    void domainDoesNotDependOnOuterLayers() {
        DOMAIN_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Application 계층은 Presentation과 Infrastructure 계층에 의존하지 않는다")
    void applicationDoesNotDependOnOuterAdapters() {
        APPLICATION_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Presentation 계층은 Infrastructure 계층에 직접 의존하지 않는다")
    void presentationDoesNotDependOnInfrastructure() {
        PRESENTATION_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Application Out Port는 Spring Data에 의존하지 않는다")
    void applicationOutputPortsDoNotDependOnSpringData() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..application.port.out..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("org.springframework.data..");

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Repository Out Port는 인터페이스로 선언한다")
    void repositoryOutputPortsAreInterfaces() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..application.port.out..")
                        .and()
                        .haveSimpleNameEndingWith("RepositoryPort")
                        .should()
                        .beInterfaces();

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Presentation은 Application Service와 Support 구현체를 직접 참조하지 않는다")
    void presentationDoesNotDependOnApplicationImplementations() {
        PRESENTATION_IMPLEMENTATION_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Application Support는 Service에 역방향 의존하지 않는다")
    void applicationSupportDoesNotDependOnServices() {
        SUPPORT_RULE.check(CLASSES);
    }
}
