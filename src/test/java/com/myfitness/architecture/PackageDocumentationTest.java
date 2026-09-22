package com.myfitness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackageDocumentationTest {
    private static final Path SOURCE_ROOT =
            Path.of("src", "main", "java", "com", "myfitness");

    private static final List<String> FEATURE_MODULES = List.of(
            "exercise",
            "workout",
            "routine",
            "body",
            "nutrition",
            "dashboard",
            "ai",
            "user");

    private static final List<String> LAYERS = List.of(
            "presentation",
            "application",
            "domain",
            "infrastructure");

    private static final List<String> IMPLEMENTED_MODULES = List.of(
            "exercise",
            "workout",
            "routine",
            "body",
            "nutrition",
            "dashboard");

    @Test
    @DisplayName("모든 기능 모듈은 최상위 package-info로 책임과 경계를 문서화한다")
    void documentsEveryFeatureModuleBoundary() {
        for (String module : FEATURE_MODULES) {
            Path packageInfo = SOURCE_ROOT
                    .resolve(module)
                    .resolve("package-info.java");

            assertThat(Files.isRegularFile(packageInfo))
                    .as("%s 모듈 package-info.java", module)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("모든 기능 모듈은 4계층 package-info를 유지한다")
    void documentsEveryArchitectureLayer() {
        for (String module : FEATURE_MODULES) {
            for (String layer : LAYERS) {
                Path packageInfo = SOURCE_ROOT
                        .resolve(module)
                        .resolve(layer)
                        .resolve("package-info.java");

                assertThat(Files.isRegularFile(packageInfo))
                        .as("%s.%s package-info.java", module, layer)
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("구현된 기능 모듈은 Application Port 방향을 package-info로 문서화한다")
    void documentsInputAndOutputPortBoundaries() {
        for (String module : IMPLEMENTED_MODULES) {
            Path application = SOURCE_ROOT
                    .resolve(module)
                    .resolve("application");

            assertThat(Files.isRegularFile(
                    application.resolve("port").resolve("package-info.java")))
                    .as("%s application.port package-info.java", module)
                    .isTrue();
            assertThat(Files.isRegularFile(
                    application.resolve("port")
                            .resolve("in")
                            .resolve("package-info.java")))
                    .as("%s application.port.in package-info.java", module)
                    .isTrue();
            assertThat(Files.isRegularFile(
                    application.resolve("port")
                            .resolve("out")
                            .resolve("package-info.java")))
                    .as("%s application.port.out package-info.java", module)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Common은 실제 사용하는 Presentation과 Infrastructure 경계를 문서화한다")
    void documentsCommonTechnicalBoundaries() {
        assertThat(Files.isRegularFile(
                SOURCE_ROOT.resolve("common").resolve("package-info.java")))
                .isTrue();
        assertThat(Files.isRegularFile(
                SOURCE_ROOT.resolve("common")
                        .resolve("presentation")
                        .resolve("package-info.java")))
                .isTrue();
        assertThat(Files.isRegularFile(
                SOURCE_ROOT.resolve("common")
                        .resolve("infrastructure")
                        .resolve("package-info.java")))
                .isTrue();
    }
}
