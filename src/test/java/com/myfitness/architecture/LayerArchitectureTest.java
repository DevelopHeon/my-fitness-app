package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class LayerArchitectureTest {
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
}
