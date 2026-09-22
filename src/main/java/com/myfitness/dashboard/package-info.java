/**
 * Workout과 Body 기록을 읽어 통계와 추이를 제공하는 read-model 성격의 Dashboard 모듈이다.
 *
 * <p>Workout/Body/Exercise의 공개 Domain 및 Repository Port만 읽어 통계를 구성한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Dashboard",
    allowedDependencies = {
        "body::domain-model",
        "body::repository",
        "exercise::domain-model",
        "workout::domain-model",
        "workout::repository"
    }
)
package com.myfitness.dashboard;
