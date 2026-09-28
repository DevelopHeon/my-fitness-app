package com.myfitness.architecture;

import com.myfitness.MyFitnessAppApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithDocumentationTest {

    private final ApplicationModules modules =
            ApplicationModules.of(MyFitnessAppApplication.class);

    @Test
    @DisplayName("현재 Spring Modulith 모듈 구조 문서를 생성한다")
    void writesModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
