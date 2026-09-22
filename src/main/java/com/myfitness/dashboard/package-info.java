/**
 * Workout과 Body 기록을 읽어 통계와 추이를 제공하는 read-model 성격의 Dashboard 모듈이다.
 *
 * <p>다른 모듈의 persistence 구현을 직접 참조하지 않고 DashboardQueryPort와 Domain Repository Port를 통해 데이터를 조회한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.dashboard;
