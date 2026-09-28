package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

class LayerArchitectureTest {
    private static final List<String> FEATURE_MODULES =
            List.of(
                    "exercise",
                    "workout",
                    "routine",
                    "body",
                    "nutrition",
                    "dashboard",
                    "ai",
                    "user");

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
                            "com.myfitness.ai.infrastructure.typesafe..")
                    .because("외부 AI 연동은 Infrastructure에 두고 Application Out Port로 호출한다.");

    private final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.myfitness");

    @Test
    @DisplayName("아키텍처 검사는 운영 클래스를 포함하고 테스트 클래스는 제외한다")
    void importsOnlyProductionClasses() {
        assertThat(classes.contain(com.myfitness.ai.application.policy.AiPolicyGuard.class))
                .isTrue();
        assertThat(
                        classes.contain(
                                com.myfitness.ai.infrastructure.springai.SpringAiChatGateway.class))
                .isTrue();
        assertThat(classes.contain(LayerArchitectureTest.class)).isFalse();
        for (String layer : List.of("domain", "application", "presentation", "infrastructure")) {
            assertThat(
                            classes.stream()
                                    .filter(
                                            type ->
                                                    type.getPackageName()
                                                            .contains("." + layer + ".")))
                    .as("운영 %s 검사 대상", layer)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("AI의 내부 계층은 Spring AI SDK와 외부 정책 Adapter를 직접 참조하지 않는다")
    void aiInnerLayersDoNotDependOnProviderImplementations() {
        AI_PROVIDER_RULE.check(classes);
    }

    @Test
    @DisplayName("Domain 계층은 Application, Presentation, Infrastructure 계층에 의존하지 않는다")
    void domainDoesNotDependOnOuterLayers() {
        DOMAIN_RULE.check(classes);
    }

    @Test
    @DisplayName("Application 계층은 Presentation과 Infrastructure 계층에 의존하지 않는다")
    void applicationDoesNotDependOnOuterAdapters() {
        APPLICATION_RULE.check(classes);
    }

    @Test
    @DisplayName("Presentation 계층은 Infrastructure 계층에 직접 의존하지 않는다")
    void presentationDoesNotDependOnInfrastructure() {
        PRESENTATION_RULE.check(classes);
    }

    @Test
    @DisplayName("Spring Data JpaRepository 구현은 Infrastructure 계층에만 둔다")
    void springDataRepositoriesStayInInfrastructure() {
        ArchRule rule =
                classes()
                        .that()
                        .areAssignableTo(JpaRepository.class)
                        .should()
                        .resideInAPackage("..infrastructure..");

        rule.check(classes);
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

        rule.check(classes);
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

        rule.check(classes);
    }

    @Test
    @DisplayName("Presentation은 Application Service 구현체를 직접 참조하지 않는다")
    void presentationDependsOnInputPortsInsteadOfApplicationServices() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..presentation..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..application.service..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Presentation은 JPA Entity에 직접 의존하지 않는다")
    void presentationDoesNotDependOnJpaEntities() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..presentation..")
                        .should()
                        .dependOnClassesThat()
                        .areAnnotatedWith(Entity.class);

        rule.check(classes);
    }

    @Test
    @DisplayName("Application In Port는 JPA Entity에 직접 의존하지 않는다")
    void inputPortsDoNotDependOnJpaEntities() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..application.port.in..")
                        .should()
                        .dependOnClassesThat()
                        .areAnnotatedWith(Entity.class);

        rule.check(classes);
    }

    @Test
    @DisplayName("Application Result의 필드는 JPA Entity 타입을 노출하지 않는다")
    void applicationResultsDoNotExposeJpaEntitiesAsFields() {
        assertThat(
                        classes.stream()
                                .filter(
                                        type ->
                                                type.getPackageName()
                                                        .contains(".application.result")))
                .as("Application Result 검사 대상")
                .isNotEmpty();
        classes.stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".application.result"))
                .forEach(
                        javaClass ->
                                javaClass
                                        .getAllFields()
                                        .forEach(
                                                field -> {
                                                    List<String> entityTypes =
                                                            field.getAllInvolvedRawTypes().stream()
                                                                    .filter(
                                                                            type ->
                                                                                    type
                                                                                            .isAnnotatedWith(
                                                                                                    Entity
                                                                                                            .class))
                                                                    .map(type -> type.getName())
                                                                    .toList();

                                                    assertThat(entityTypes)
                                                            .as(field.getFullName())
                                                            .isEmpty();
                                                }));
    }

    @Test
    @DisplayName("JPA Entity는 Domain model 패키지에만 둔다")
    void jpaEntitiesStayInDomainModel() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .resideInAPackage("..domain.model..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Spring Service는 Application service 패키지에만 둔다")
    void springServicesStayInApplicationService() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Service.class)
                        .should()
                        .resideInAPackage("..application.service..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application In Port 구현 Service는 read-only transaction을 기본 경계로 선언한다")
    void inputPortServicesDeclareReadOnlyTransactionBoundary() {
        assertThat(
                        classes.stream()
                                .filter(type -> type.isAnnotatedWith(Service.class))
                                .filter(
                                        type ->
                                                type.getAllRawInterfaces().stream()
                                                        .anyMatch(
                                                                it ->
                                                                        it.getPackageName()
                                                                                .contains(
                                                                                        ".application.port.in"))))
                .as("In Port Service transaction 검사 대상")
                .isNotEmpty();
        classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Service.class))
                .filter(
                        javaClass ->
                                javaClass.getAllRawInterfaces().stream()
                                        .anyMatch(
                                                type ->
                                                        type.getPackageName()
                                                                .contains(".application.port.in")))
                .forEach(
                        javaClass -> {
                            assertThat(javaClass.isAnnotatedWith(Transactional.class))
                                    .as("%s transaction boundary", javaClass.getName())
                                    .isTrue();

                            Transactional transaction =
                                    javaClass.getAnnotationOfType(Transactional.class);
                            assertThat(transaction.readOnly())
                                    .as("%s default transaction", javaClass.getName())
                                    .isTrue();
                        });
    }

    @Test
    @DisplayName("REST Controller는 Presentation controller 패키지에만 둔다")
    void restControllersStayInPresentationController() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(RestController.class)
                        .should()
                        .resideInAPackage("..presentation.controller..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Spring Repository Adapter는 Infrastructure 패키지에만 둔다")
    void springRepositoryAdaptersStayInInfrastructure() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Repository.class)
                        .should()
                        .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("기능 모듈은 서로 순환 의존하지 않는다")
    void featureModulesDoNotFormDependencyCycles() {
        ArchRule rule = slices().matching("com.myfitness.(*)..").should().beFreeOfCycles();

        rule.check(classes);
    }

    @Test
    @DisplayName("기능 모듈의 Presentation은 다른 기능 모듈 Presentation을 직접 참조하지 않는다")
    void featurePresentationsDoNotDependOnOtherPresentations() {
        for (String module : FEATURE_MODULES) {
            String[] otherPresentationPackages =
                    FEATURE_MODULES.stream()
                            .filter(other -> !other.equals(module))
                            .map(other -> "com.myfitness." + other + ".presentation..")
                            .toArray(String[]::new);

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("com.myfitness." + module + ".presentation..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(otherPresentationPackages);

            rule.check(classes);
        }
    }

    @Test
    @DisplayName("기능 모듈의 Infrastructure는 다른 기능 모듈 Infrastructure를 직접 참조하지 않는다")
    void featureInfrastructuresDoNotDependOnOtherInfrastructures() {
        for (String module : FEATURE_MODULES) {
            String[] otherInfrastructurePackages =
                    FEATURE_MODULES.stream()
                            .filter(other -> !other.equals(module))
                            .map(other -> "com.myfitness." + other + ".infrastructure..")
                            .toArray(String[]::new);

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("com.myfitness." + module + ".infrastructure..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(otherInfrastructurePackages);

            rule.check(classes);
        }
    }

    @Test
    @DisplayName("Exercise 기반 모듈은 다른 기능 모듈에 의존하지 않는다")
    void exerciseModuleDoesNotDependOnFeatureModules() {
        String[] forbiddenPackages =
                FEATURE_MODULES.stream()
                        .filter(module -> !module.equals("exercise"))
                        .map(module -> "com.myfitness." + module + "..")
                        .toArray(String[]::new);

        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("com.myfitness.exercise..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(forbiddenPackages);

        rule.check(classes);
    }
}
