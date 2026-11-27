package com.myfitness.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

class PortBoundaryArchitectureTest {
    static final ArchRule INPUT_PORT_IMPLEMENTATION_RULE = classes()
            .that().implement(resideInAPackage("..application.port.in.."))
            .should().resideInAPackage("..application.service..")
            .andShould().haveSimpleNameEndingWith("PortService")
            .because("공개 Input Port는 Application의 PortService에서 구현한다.");

    static final ArchRule PORT_SERVICE_RULE = noClasses()
            .that().resideInAPackage("..application.service..")
            .and().haveSimpleNameEndingWith("PortService")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..domain..", "..application.port.out..", "..application.service..", "org.springframework.transaction..")
            .because("PortService는 공개 값을 변환하고 내부 UseCase를 호출한다.");

    static final ArchRule INPUT_PORT_RULE = noClasses()
            .that().resideInAPackage("..application.port.in..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..domain..", "..application.usecase..",
                    "..application.service..", "..application.port.out..", "..presentation..")
            .because("공개 Input Port의 값은 모듈 내부 타입에 의존하지 않는다.");

    static final ArchRule INPUT_ADAPTER_RULE = noClasses()
            .that().resideInAnyPackage(
                    "..presentation..", "..infrastructure.security..", "..infrastructure.bootstrap..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..application.usecase..", "..application.service..", "..application.support..",
                    "..application.port.out..", "..domain.model..", "..domain.repository..")
            .because("Input Adapter는 공개 Input Port를 사용한다.");

    static final ArchRule DOMAIN_REPOSITORY_RULE = noClasses()
            .that().resideInAPackage("..domain.repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..",
                    "..application..", "..presentation..", "..infrastructure..")
            .because("Domain Repository는 영속 기술에 독립적인 업무 계약이다.");

    static final ArchRule BUSINESS_SERVICE_DEPENDENCY_RULE = noClasses()
            .that().areAnnotatedWith(Service.class)
            .should().dependOnClassesThat().areAnnotatedWith(Service.class)
            .because("업무 Service 간 호출은 모듈 내부 UseCase를 사용한다.");

    static final ArchRule LEGACY_DTO_PACKAGE_RULE = noClasses()
            .should().resideInAPackage("..application.dto..")
            .because("내부 DTO는 UseCase 아래에 두고 Output Port 전용 값은 Port가 소유한다.");

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.myfitness");

    @Test
    @DisplayName("PortService는 Domain이나 저장소 및 구체 Service에 의존하지 않는다")
    void portServicesUseInternalContracts() {
        INPUT_PORT_IMPLEMENTATION_RULE.check(CLASSES);
        PORT_SERVICE_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("공개 Input Port는 내부 타입을 노출하지 않는다")
    void inputPortsExposePublicValues() {
        INPUT_PORT_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Input Adapter는 내부 업무 계약을 직접 호출하지 않는다")
    void inputAdaptersUsePublicPorts() {
        INPUT_ADAPTER_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Domain Repository는 영속 기술에 독립적인 인터페이스다")
    void domainRepositoriesArePureInterfaces() {
        classes().that().resideInAPackage("..domain.repository..").should().beInterfaces().check(CLASSES);
        DOMAIN_REPOSITORY_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("Application은 다른 업무 모듈을 자기 Output Port와 Module Adapter를 경유해 호출한다")
    void applicationUsesModuleAdapters() {
        List<String> modules = CLASSES.stream()
                .map(type -> type.getPackageName())
                .filter(name -> name.startsWith("com.myfitness.") && name.contains(".application."))
                .map(name -> name.substring(0, name.indexOf(".application.")))
                .distinct()
                .toList();
        for (String module : modules) {
            String[] otherModules = modules.stream()
                    .filter(other -> !other.equals(module))
                    .map(other -> other + "..")
                    .toArray(String[]::new);
            moduleAdapterRule(module, otherModules).check(CLASSES);
        }
    }

    @Test
    @DisplayName("업무 Service는 구체 Service를 직접 참조하지 않는다")
    void businessServicesUseInternalContracts() {
        BUSINESS_SERVICE_DEPENDENCY_RULE.check(CLASSES);
    }

    @Test
    @DisplayName("이전 Application DTO 패키지는 내부 계약을 소유하지 않는다")
    void dtoContractsStayWithTheirOwner() {
        LEGACY_DTO_PACKAGE_RULE.check(CLASSES);
    }

    static ArchRule moduleAdapterRule(String module, String... otherModules) {
        return noClasses().that().resideInAPackage(module + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(otherModules)
                .because("다른 업무 모듈의 공개 진입 호출과 값 변환은 Module Adapter에 둔다.");
    }
}
