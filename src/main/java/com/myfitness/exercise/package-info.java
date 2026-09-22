/**
 * 기본 운동 카탈로그와 사용자 정의 운동 종목을 관리하는 독립 도메인이다.
 *
 * <p>Workout과 Routine이 공통으로 사용하는 ExerciseReference, ExerciseType, ExerciseCategory를 제공하며 다른 기능 모듈에 의존하지 않는 기반 모듈로 유지한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.exercise;
