/**
 * 다른 application module에 공개하는 domain-model 인터페이스다.
 *
 * <p>외부 모듈은 workout::domain-model 의존을 명시한 경우에만 이 패키지에 접근할 수 있다.</p>
 */
@org.springframework.modulith.NamedInterface("domain-model")
package com.myfitness.workout.domain.model;
