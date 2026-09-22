/**
 * 체중, 체지방률, 골격근량 등 사용자의 신체 측정 기록을 관리하는 Body 도메인이다.
 *
 * <p>독립적인 신체 기록 도메인으로 다른 기능 모듈에 의존하지 않는다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Body",
    allowedDependencies = {}
)
package com.myfitness.body;
