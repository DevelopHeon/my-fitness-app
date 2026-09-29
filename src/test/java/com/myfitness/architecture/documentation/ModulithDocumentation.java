package com.myfitness.architecture.documentation;

import com.myfitness.MyFitnessAppApplication;

import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** 구조 검증과 별도로 실행하는 모듈 문서 생성 도구다. */
public class ModulithDocumentation {
    public static void main(String[] args) {
        ApplicationModules modules = ApplicationModules.of(MyFitnessAppApplication.class);
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
