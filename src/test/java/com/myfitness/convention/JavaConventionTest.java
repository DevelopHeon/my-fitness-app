package com.myfitness.convention;

import static org.assertj.core.api.Assertions.assertThat;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

class JavaConventionTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "var value = 1;",
                "final var value = 1;",
                "for (var index = 0; index < 2; index++) {}",
                "for (var value : java.util.List.of(1)) {}",
                "try (var reader = new java.io.StringReader(\"value\")) {}",
                "java.util.function.Function<String,String> fn = (var value) -> value;"
            })
    @DisplayName("실제 AST 규칙이 지역 변수·반복문·resource·lambda의 var 선언을 거절한다")
    void rejectsInferredTypeDeclarations(String declaration) throws Exception {
        assertThat(violations(declaration)).isPositive();
    }

    @Test
    @DisplayName("명시적 타입·var 이름·문자열·주석·타입 생략 lambda는 정상 Java 소스로 허용한다")
    void acceptsExplicitTypesWithoutTextSearchFalsePositives() throws Exception {
        assertThat(
                        violations(
                                "String var = \"var text\"; /* var comment */"
                                    + " java.util.function.Function<String,String> fn = value ->"
                                    + " value;"))
                .isZero();
    }

    private int violations(String declaration) throws Exception {
        Path source = directory.resolve("ConventionProbe.java");
        Files.writeString(
                source,
                "class ConventionProbe { void check() throws Exception { " + declaration + " } }");
        Checker checker = new Checker();
        try {
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(
                    ConfigurationLoader.loadConfiguration(
                            "config/checkstyle/checkstyle.xml",
                            new PropertiesExpander(new Properties()),
                            ConfigurationLoader.IgnoredModulesOptions.OMIT));
            return checker.process(List.of(source.toFile()));
        } finally {
            checker.destroy();
        }
    }
}
