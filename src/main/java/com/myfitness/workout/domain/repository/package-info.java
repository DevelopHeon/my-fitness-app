/**
 * 다른 application module에 공개하는 repository 인터페이스다.
 *
 * <p>외부 모듈은 workout::repository 의존을 명시한 경우에만 이 패키지에 접근할 수 있다.</p>
 */
@org.springframework.modulith.NamedInterface("repository")
package com.myfitness.workout.domain.repository;
