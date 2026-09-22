package com.myfitness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.MyFitnessAppApplication;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.NamedInterface;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES =
            Map.of(
                    "exercise", Set.of(),
                    "workout", Set.of(
                            "exercise::catalog",
                            "exercise::domain-model"),
                    "routine", Set.of(
                            "exercise::catalog",
                            "exercise::domain-model",
                            "workout::routine-api"),
                    "body", Set.of(),
                    "nutrition", Set.of(),
                    "dashboard", Set.of(
                            "body::insight",
                            "workout::insight"),
                    "ai", Set.of(
                            "workout::insight",
                            "body::insight",
                            "nutrition::insight"),
                    "user", Set.of(),
                    "common", Set.of(
                            "ai::application-exception",
                            "ai::domain-exception",
                            "body::application-exception",
                            "body::domain-exception",
                            "exercise::application-exception",
                            "exercise::domain-exception",
                            "nutrition::application-exception",
                            "nutrition::domain-exception",
                            "routine::application-exception",
                            "routine::domain-exception",
                            "workout::application-exception",
                            "workout::domain-exception"));

    private static final Map<String, String> NAMED_INTERFACES =
            Map.ofEntries(
                    Map.entry(
                            "ai.application.exception",
                            "application-exception"),
                    Map.entry(
                            "ai.domain.exception",
                            "domain-exception"),
                    Map.entry(
                            "exercise.application.port.in.catalog",
                            "catalog"),
                    Map.entry("exercise.domain.model", "domain-model"),
                    Map.entry(
                            "workout.application.port.in.routine",
                            "routine-api"),
                    Map.entry(
                            "workout.application.port.in.insight",
                            "insight"),
                    Map.entry(
                            "body.application.port.in.insight",
                            "insight"),
                    Map.entry(
                            "nutrition.application.port.in.insight",
                            "insight"),
                    Map.entry(
                            "exercise.application.exception",
                            "application-exception"),
                    Map.entry(
                            "exercise.domain.exception",
                            "domain-exception"),
                    Map.entry(
                            "workout.application.exception",
                            "application-exception"),
                    Map.entry(
                            "workout.domain.exception",
                            "domain-exception"),
                    Map.entry(
                            "routine.application.exception",
                            "application-exception"),
                    Map.entry(
                            "routine.domain.exception",
                            "domain-exception"),
                    Map.entry(
                            "body.application.exception",
                            "application-exception"),
                    Map.entry(
                            "body.domain.exception",
                            "domain-exception"),
                    Map.entry(
                            "nutrition.application.exception",
                            "application-exception"),
                    Map.entry(
                            "nutrition.domain.exception",
                            "domain-exception"));

    private final ApplicationModules modules =
            ApplicationModules.of(MyFitnessAppApplication.class);

    @Test
    @DisplayName("Spring Modulith 모듈 공개 인터페이스와 허용 의존성을 검증한다")
    void verifiesApplicationModuleStructure() {
        modules.verify();
    }

    @Test
    @DisplayName("Spring Modulith는 합의한 최상위 application module을 모두 감지한다")
    void detectsExpectedApplicationModules() {
        Set<String> detected = modules.stream()
                .map(module -> module.getIdentifier().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(detected)
                .containsExactlyInAnyOrderElementsOf(
                        ALLOWED_DEPENDENCIES.keySet());
    }

    @Test
    @DisplayName("각 모듈은 합의한 allowedDependencies만 선언한다")
    void declaresExpectedAllowedDependencies() throws Exception {
        for (Map.Entry<String, Set<String>> entry
                : ALLOWED_DEPENDENCIES.entrySet()) {
            Package modulePackage =
                    loadPackage("com.myfitness." + entry.getKey());
            ApplicationModule metadata =
                    modulePackage.getAnnotation(ApplicationModule.class);

            assertThat(metadata)
                    .as("%s @ApplicationModule", entry.getKey())
                    .isNotNull();
            assertThat(metadata.allowedDependencies())
                    .as("%s allowedDependencies", entry.getKey())
                    .containsExactlyInAnyOrderElementsOf(entry.getValue());
        }
    }

    @Test
    @DisplayName("모듈 간 공개 패키지는 합의한 Named Interface 이름을 유지한다")
    void declaresExpectedNamedInterfaces() throws Exception {
        for (Map.Entry<String, String> entry
                : NAMED_INTERFACES.entrySet()) {
            Package apiPackage =
                    loadPackage("com.myfitness." + entry.getKey());
            NamedInterface metadata =
                    apiPackage.getAnnotation(NamedInterface.class);

            assertThat(metadata)
                    .as("%s @NamedInterface", entry.getKey())
                    .isNotNull();
            assertThat(metadata.value())
                    .as("%s Named Interface name", entry.getKey())
                    .containsExactly(entry.getValue());
        }
    }

    private static Package loadPackage(String packageName)
            throws ClassNotFoundException {
        return Class.forName(packageName + ".package-info").getPackage();
    }
}
