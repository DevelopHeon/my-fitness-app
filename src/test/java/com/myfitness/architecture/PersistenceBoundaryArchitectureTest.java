package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Entity 비노출, JPA 배치, 기본 transaction 선언을 검증한다. 실제 transaction 동작은 통합 테스트가 담당한다. */
class PersistenceBoundaryArchitectureTest {
    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.myfitness");

    @Test
    @DisplayName("Spring Data JpaRepository 구현은 Infrastructure 계층에만 둔다")
    void springDataRepositoriesStayInInfrastructure() {
        ArchRule rule =
                classes()
                        .that()
                        .areAssignableTo(JpaRepository.class)
                        .should()
                        .resideInAPackage("..infrastructure..");

        rule.check(CLASSES);
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

        rule.check(CLASSES);
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

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Application Result의 필드는 JPA Entity 타입을 노출하지 않는다")
    void applicationResultsDoNotExposeJpaEntitiesAsFields() {
        assertThat(
                        CLASSES.stream()
                                .filter(
                                        type ->
                                                type.getPackageName()
                                                        .contains(".application.result")))
                .as("Application Result 검사 대상")
                .isNotEmpty();
        CLASSES.stream()
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

        rule.check(CLASSES);
    }

    @Test
    @DisplayName("Application In Port 구현 Service는 read-only transaction을 기본 경계로 선언한다")
    void inputPortServicesDeclareReadOnlyTransactionBoundary() {
        assertThat(
                        CLASSES.stream()
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
        CLASSES.stream()
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
}
