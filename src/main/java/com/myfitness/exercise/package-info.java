/**
 * 기본 운동 카탈로그와 사용자 정의 운동 종목을 관리하는 독립 도메인이다.
 *
 * <p>Workout과 Routine이 공통으로 사용하는 운동 참조 타입을 공개하며 다른 기능 모듈에는 의존하지 않는다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Exercise",
    allowedDependencies = {}
)
package com.myfitness.exercise;
