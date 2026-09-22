/**
 * Routine 모듈이 Workout을 시작할 때 사용하는 공개 In Port다.
 *
 * <p>Workout Domain Entity나 내부 Service를 외부 모듈에 노출하지 않고
 * Routine 시작에 필요한 projection만 제공한다.</p>
 */
@org.springframework.modulith.NamedInterface("routine-api")
package com.myfitness.workout.application.port.in.routine;
