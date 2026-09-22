/**
 * 음식, 식사 기록, 영양 snapshot, 일일 영양 목표를 관리하는 Nutrition 도메인이다.
 *
 * <p>독립적인 영양 기록 도메인으로 다른 기능 모듈에 의존하지 않는다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Nutrition",
    allowedDependencies = {}
)
package com.myfitness.nutrition;
