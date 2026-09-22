/**
 * 체중, 체지방률, 골격근량 등 사용자의 신체 측정 기록을 관리하는 Body 도메인이다.
 *
 * <p>측정값 검증은 Domain에서, 조회 범위와 trend 조합은 Application에서 수행한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.body;
