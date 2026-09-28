/**
 * AI Coach처럼 영양 상태를 읽기 전용으로 소비하는 모듈이 사용하는 공개 In Port다.
 *
 * <p>Nutrition Entity나 Repository를 노출하지 않고 일별 영양 요약 projection만 제공한다.</p>
 */
@org.springframework.modulith.NamedInterface("insight")
package com.myfitness.nutrition.application.port.in.insight;
