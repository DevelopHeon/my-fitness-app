/**
 * Body 모듈의 체중, 체지방률, 골격근량 기록과 도메인 규칙을 담당한다.
 *
 * <p>Domain Model과 Domain Exception을 두며 Repository 계약은 Application Out Port에 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application, Presentation, Infrastructure에 의존하지 않는다.
 * JPA mapping annotation은 현재의 Practical Clean Architecture 기준으로 Domain Model에 허용한다.</p>
 */
package com.myfitness.body.domain;
