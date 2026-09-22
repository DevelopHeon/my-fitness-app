/**
 * 반복 가능한 운동 템플릿과 루틴 기반 Workout 시작 흐름을 관리하는 Routine 도메인이다.
 *
 * <p>Exercise 조회와 Workout 생성 Use Case를 조합하되 각 모듈이 공개한 Named Interface만 사용한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Routine",
    allowedDependencies = {
        "exercise::catalog",
        "exercise::domain-model",
        "workout::routine-api"
    }
)
package com.myfitness.routine;
