/**
 * 여러 기능 모듈에서 공통으로 필요한 HTTP 오류 변환과 기술 설정을 제공하는 Common 모듈이다.
 *
 * <p>기능별 예외를 HTTP 응답으로 변환하기 위해 공개된 exception Named Interface에만 의존한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Common",
    allowedDependencies = {
        "ai::application-exception",
        "ai::domain-exception",
        "body::application-exception",
        "body::domain-exception",
        "exercise::application-exception",
        "exercise::domain-exception",
        "nutrition::application-exception",
        "nutrition::domain-exception",
        "routine::application-exception",
        "routine::domain-exception",
        "workout::application-exception",
        "workout::domain-exception"
    }
)
package com.myfitness.common;
