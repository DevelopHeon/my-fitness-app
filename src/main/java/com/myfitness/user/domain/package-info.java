/**
 * User의 외부 식별자와 프로필 갱신 규칙을 담당하는 Domain 계층이다.
 *
 * <p>Google의 변경 가능한 email이 아니라 OIDC sub를 외부 식별자로 사용한다.
 * Repository 계약은 Application Out Port에 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application, Presentation, Infrastructure에 의존하지 않는다.
 * JPA mapping annotation은 현재의 Practical Clean Architecture 기준으로 Domain Model에 허용한다.</p>
 */
package com.myfitness.user.domain;
