/**
 * Dashboard와 AI 같은 읽기 전용 소비자가 Workout 통계를 구성할 때 사용하는 공개 In Port다.
 *
 * <p>Persistence나 Workout Entity 전체를 노출하지 않고 계산에 필요한 projection만 반환한다.</p>
 */
@org.springframework.modulith.NamedInterface("insight")
package com.myfitness.workout.application.port.in.insight;
