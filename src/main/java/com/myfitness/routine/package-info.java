/**
 * 반복 가능한 운동 템플릿과 루틴 기반 Workout 시작 흐름을 관리하는 Routine 도메인이다.
 *
 * <p>Routine Domain은 템플릿 규칙만 가지며 Exercise 조회와 Workout 생성의 조합은 Application 계층에서 수행한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.routine;
