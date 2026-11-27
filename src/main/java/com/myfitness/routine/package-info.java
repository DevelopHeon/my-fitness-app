/**
 * 반복 가능한 운동 템플릿과 루틴 기반 Workout 시작 흐름을 관리하는 Routine 도메인이다.
 *
 * <p>Module Adapter를 통해 운동 카탈로그를 조회하고 저장된 루틴 snapshot으로 운동을 시작한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Routine",
    allowedDependencies = {
        "exercise::catalog",
        "workout::routine-api"
    }
)
package com.myfitness.routine;
