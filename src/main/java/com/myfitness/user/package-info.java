/**
 * 사용자 식별과 인증 컨텍스트를 담당할 User 모듈의 경계다.
 *
 * <p>현재는 X-User-Id 임시 컨텍스트를 사용하며 실제 인증 도입 시에도 외부 인증 기술은 Infrastructure에 격리한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.user;
