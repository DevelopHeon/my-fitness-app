/**
 * ai 모듈의 영속화와 외부 기술 연동을 담당하는 Infrastructure 계층이다.
 *
 * <p>persistence에는 Spring Data JPA와 Repository Adapter를,
 * client에는 답변 생성과 정책 평가를 위한 외부 AI 연동 구현을 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application Out Port를 구현하거나 다른 모듈의 공개 In Port로 연결하는 Adapter를 두며 Presentation을 직접 참조하지 않는다.</p>
 */
package com.myfitness.ai.infrastructure;
