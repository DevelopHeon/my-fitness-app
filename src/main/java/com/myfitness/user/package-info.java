/**
 * 사용자 식별과 인증 컨텍스트를 담당하는 User 모듈의 경계다.
 *
 * <p>Google OIDC 기반 사용자와 서버 인증 컨텍스트를 관리한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * 다른 기능 모듈에 의존하지 않는다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "User",
    allowedDependencies = {}
)
package com.myfitness.user;
