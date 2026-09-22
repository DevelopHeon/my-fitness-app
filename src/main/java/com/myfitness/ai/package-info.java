/**
 * 피트니스 기록을 기반으로 설명과 추천을 제공할 AI 기능의 경계다.
 *
 * <p>AI provider, Spring AI, 외부 모델 SDK는 Infrastructure에 격리하고 Application은 Port를 통해서만 호출한다.</p>
 *
 * <p>모듈 내부는 presentation → application → domain 방향을 기본으로 하며,
 * infrastructure는 application/domain의 port를 구현하는 adapter 역할을 한다.</p>
 */
package com.myfitness.ai;
