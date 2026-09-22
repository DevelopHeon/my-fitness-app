package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class LayerArchitectureTest {
    private static final List<String> IMPLEMENTED_MODULES = List.of(
            "exercise",
            "workout",
            "routine",
            "body",
            "nutrition",
            "dashboard");

    private final JavaClasses classes =
            new ClassFileImporter().importPackages("com.myfitness");

    @Test
    @DisplayName("Domain 계층은 Application, Presentation, Infrastructure 계층에 의존하지 않는다")
    void domainDoesNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..presentation..",
                        "..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application 계층은 Presentation과 Infrastructure 계층에 의존하지 않는다")
    void applicationDoesNotDependOnOuterAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..presentation..",
                        "..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Presentation 계층은 Infrastructure 계층에 직접 의존하지 않는다")
    void presentationDoesNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..presentation..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Spring Data JpaRepository 구현은 Infrastructure 계층에만 둔다")
    void springDataRepositoriesStayInInfrastructure() {
        ArchRule rule = classes()
                .that().areAssignableTo(JpaRepository.class)
                .should().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain Repository Port는 Spring Data에 의존하지 않는다")
    void domainRepositoryPortsDoNotDependOnSpringData() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.repository..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.data..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain Repository Port는 인터페이스로 선언한다")
    void domainRepositoryPortsAreInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.repository..")
                .should().beInterfaces();

        rule.check(classes);
    }

    @Test
    @DisplayName("JPA Entity는 Domain model 패키지에만 둔다")
    void jpaEntitiesStayInDomainModel() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..domain.model..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Spring Service는 Application service 패키지에만 둔다")
    void springServicesStayInApplicationService() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Service.class)
                .should().resideInAPackage("..application.service..");

        rule.check(classes);
    }

    @Test
    @DisplayName("REST Controller는 Presentation controller 패키지에만 둔다")
    void restControllersStayInPresentationController() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..presentation.controller..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Spring Repository Adapter는 Infrastructure 패키지에만 둔다")
    void springRepositoryAdaptersStayInInfrastructure() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Repository.class)
                .should().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("기능 모듈은 서로 순환 의존하지 않는다")
    void featureModulesDoNotFormDependencyCycles() {
        ArchRule rule = slices()
                .matching("com.myfitness.(*)..")
                .should().beFreeOfCycles();

        rule.check(classes);
    }

    @Test
    @DisplayName("기능 모듈의 Presentation은 다른 기능 모듈 Presentation을 직접 참조하지 않는다")
    void featurePresentationsDoNotDependOnOtherPresentations() {
        for (String module : IMPLEMENTED_MODULES) {
            String[] otherPresentationPackages = IMPLEMENTED_MODULES.stream()
                    .filter(other -> !other.equals(module))
                    .map(other -> "com.myfitness." + other + ".presentation..")
                    .toArray(String[]::new);

            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "com.myfitness." + module + ".presentation..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherPresentationPackages);

            rule.check(classes);
        }
    }

    @Test
    @DisplayName("기능 모듈의 Infrastructure는 다른 기능 모듈 Infrastructure를 직접 참조하지 않는다")
    void featureInfrastructuresDoNotDependOnOtherInfrastructures() {
        for (String module : IMPLEMENTED_MODULES) {
            String[] otherInfrastructurePackages = IMPLEMENTED_MODULES.stream()
                    .filter(other -> !other.equals(module))
                    .map(other -> "com.myfitness." + other + ".infrastructure..")
                    .toArray(String[]::new);

            ArchRule rule = noClasses()
                    .that().resideInAPackage(
                            "com.myfitness." + module + ".infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherInfrastructurePackages);

            rule.check(classes);
        }
    }

    @Test
    @DisplayName("Exercise 기반 모듈은 다른 기능 모듈에 의존하지 않는다")
    void exerciseModuleDoesNotDependOnFeatureModules() {
        String[] forbiddenPackages = IMPLEMENTED_MODULES.stream()
                .filter(module -> !module.equals("exercise"))
                .map(module -> "com.myfitness." + module + "..")
                .toArray(String[]::new);

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.myfitness.exercise..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(forbiddenPackages);

        rule.check(classes);
    }
}
