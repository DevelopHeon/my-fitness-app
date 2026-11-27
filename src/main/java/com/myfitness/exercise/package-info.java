/**
 * 기본 운동 카탈로그와 사용자 정의 운동 종목을 관리하는 독립 도메인이다.
 *
 * <p>카탈로그 Input Port를 공개하고 운동 분류와 소유권을 값으로 전달한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Exercise",
    allowedDependencies = {}
)
package com.myfitness.exercise;
