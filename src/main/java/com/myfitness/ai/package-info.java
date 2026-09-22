/**
 * 피트니스 기록을 기반으로 설명과 추천을 제공하는 AI Coach application module이다.
 *
 * <p>다른 기능 모듈의 Repository/Infrastructure를 직접 참조하지 않고
 * 읽기 전용 Insight Named Interface만 사용한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "AI Coach",
    allowedDependencies = {
        "workout::insight",
        "body::insight",
        "nutrition::insight"
    }
)
package com.myfitness.ai;
