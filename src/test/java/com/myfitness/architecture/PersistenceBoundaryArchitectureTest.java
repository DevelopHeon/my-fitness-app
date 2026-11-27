package com.myfitness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
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
import java.util.Set;
import java.util.stream.Collectors;

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
    @DisplayName("내부 UseCase와 Output Port의 DTO 필드는 JPA Entity를 노출하지 않는다")
    void applicationDtosDoNotExposeJpaEntitiesAsFields() {
        List<JavaClass> dtoTypes = CLASSES.stream()
                .filter(type -> type.getPackageName().contains(".application.usecase.request")
                        || type.getPackageName().contains(".application.usecase.response")
                        || (type.isRecord() && (type.getPackageName().contains(".application.usecase")
                                || type.getPackageName().contains(".application.port.out"))))
                .toList();

        assertThat(dtoTypes).as("내부 DTO 검사 대상").isNotEmpty();
        for (JavaClass type : dtoTypes) {
            type.getAllFields().forEach(field -> assertThat(field.getAllInvolvedRawTypes())
                    .as(field.getFullName())
                    .noneMatch(fieldType -> fieldType.isAnnotatedWith(Entity.class)));
        }
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
    @DisplayName("PortService가 호출하는 업무 Service와 저장 트랜잭션 Service는 기본 read-only 경계를 선언한다")
    void businessServicesDeclareReadOnlyTransactionBoundary() {
        Set<JavaClass> entryContracts = CLASSES.stream()
                .filter(type -> type.getSimpleName().endsWith("PortService"))
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .map(Dependency::getTargetClass)
                .filter(type -> type.isInterface() && type.getPackageName().contains(".application.usecase"))
                .collect(Collectors.toSet());

        assertThat(entryContracts).as("공개 진입이 호출하는 내부 UseCase").isNotEmpty();
        List<JavaClass> services = CLASSES.stream()
                .filter(type -> type.isAnnotatedWith(Service.class))
                .filter(type -> type.getAllRawInterfaces().stream().anyMatch(contract -> entryContracts.contains(contract)
                                || contract.getSimpleName().endsWith("TransactionUseCase"))
                        || type.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Transactional.class)))
                .toList();
        assertThat(services).as("트랜잭션 경계를 소유하는 Service").isNotEmpty();
        for (JavaClass service : services) {
            assertThat(service.isAnnotatedWith(Transactional.class))
                    .as("%s transaction boundary", service.getName())
                    .isTrue();
            assertThat(service.getAnnotationOfType(Transactional.class).readOnly())
                    .as("%s default transaction", service.getName())
                    .isTrue();
        }
    }
}
