package com.myfitness.convention;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/** Spring 컴포넌트 어노테이션의 지정 패키지 배치를 검증한다. */
class ComponentConventionTest {
    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.myfitness");

    @Test
    @DisplayName("Application은 구조적 역할별 패키지로 구성한다")
    void applicationClassesStayInRolePackages() {
        classes()
                .that()
                .resideInAPackage("..application..")
                .should()
                .resideInAnyPackage(
                        "..application",
                        "..application.config..",
                        "..application.dto.request..",
                        "..application.dto.response..",
                        "..application.exception..",
                        "..application.port..",
                        "..application.service..",
                        "..application.support..")
                .because("입출력 DTO와 내부 협력 기능은 dto/request·response와 support에 모은다.")
                .check(CLASSES);
    }

    @Test
    @DisplayName("Application Command는 request DTO 패키지에 둔다")
    void applicationCommandsStayInRequestDtoPackages() {
        classes()
                .that()
                .resideInAPackage("..application..")
                .and()
                .haveSimpleNameEndingWith("Command")
                .should()
                .resideInAPackage("..application.dto.request..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("Application Result는 response DTO 패키지에 둔다")
    void applicationResultsStayInResponseDtoPackages() {
        classes()
                .that()
                .resideInAPackage("..application..")
                .and()
                .haveSimpleNameEndingWith("Result")
                .should()
                .resideInAPackage("..application.dto.response..")
                .check(CLASSES);
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

        rule.check(CLASSES);
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

        rule.check(CLASSES);
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

        rule.check(CLASSES);
    }
}
