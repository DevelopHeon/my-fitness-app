/**
 * User 영속화와 Google OIDC/Spring Security 연동을 담당하는 Infrastructure 계층이다.
 *
 * <p>Domain Repository의 Persistence Adapter와 인증 사용자 변환을 담당하는 Security Adapter를 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application/Domain 계약을 구현하며 Presentation DTO를 직접 참조하지 않는다.</p>
 */
package com.myfitness.user.infrastructure;
