/**
 * 운동 일자, 운동 종목 snapshot, 세트 기록과 완료 상태를 관리하는 Workout 도메인이다.
 *
 * <p>카탈로그 Module Adapter가 상대 공개 값을 변환하며 운동 기록에 이름과 카테고리를 보존한다.</p>
 *
 * <p>Spring Modulith의 closed application module로 선언하며,
 * allowedDependencies와 Named Interface를 통해 모듈 간 접근 범위를 검증한다.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Workout",
    allowedDependencies = {
        "exercise::catalog"
    }
)
package com.myfitness.workout;
