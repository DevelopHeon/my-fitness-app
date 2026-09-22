/**
 * 운동 일자, 운동 종목 snapshot, 세트 기록과 완료 상태를 관리하는 Workout 도메인이다.
 *
 * <p>운동 종목의 원본 정의는 exercise 모듈을 사용하며 기록 시점의 이름과 카테고리는 WorkoutExercise에 snapshot으로 보존한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.workout;
