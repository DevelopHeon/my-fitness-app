/**
 * 피트니스 기록을 기반으로 설명과 추천을 제공할 AI 기능의 경계다.
 *
 * <p>Phase 6 구현 전에는 외부 기능 모듈 의존을 허용하지 않으며 구현 시 필요한 공개 API만 명시적으로 추가한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "AI Coach",
    allowedDependencies = {}
)
package com.myfitness.ai;
