/**
 * 음식, 식사 기록, 영양 snapshot, 일일 영양 목표를 관리하는 Nutrition 도메인이다.
 *
 * <p>음식 원본이 변경되어도 과거 MealFood 기록의 영양 정보가 유지되도록 snapshot을 보존한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.nutrition;
