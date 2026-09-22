/**
 * 운동 일자, 운동 종목 snapshot, 세트 기록과 완료 상태를 관리하는 Workout 도메인이다.
 *
 * <p>Exercise의 공개 Application/Domain API만 사용하고 기록 시점의 이름과 카테고리를 WorkoutExercise에 snapshot으로 보존한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Workout",
    allowedDependencies = {
        "exercise::catalog",
        "exercise::domain-model"
    }
)
package com.myfitness.workout;
