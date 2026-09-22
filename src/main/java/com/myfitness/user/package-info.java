/**
 * 사용자 식별과 인증 컨텍스트를 담당할 User 모듈의 경계다.
 *
 * <p>현재 기능 구현 전이므로 외부 기능 모듈 의존을 허용하지 않는다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "User",
    allowedDependencies = {}
)
package com.myfitness.user;
