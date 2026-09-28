/**
 * Workout과 Body 기록을 읽어 통계와 추이를 제공하는 read-model 성격의 Dashboard 모듈이다.
 *
 * <p>Workout/Body의 공개 Insight In Port만 사용하며 다른 모듈의 Repository나 Domain Entity를 직접 참조하지 않는다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Dashboard",
    allowedDependencies = {
        "body::insight",
        "workout::insight"
    }
)
package com.myfitness.dashboard;
