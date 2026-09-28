/**
 * Workout 모듈의 운동 일자, 종목 snapshot, 세트와 완료 상태 규칙을 담당하는 Domain 계층이다.
 *
 * <p>Domain Model과 Domain Exception을 두며 Repository 계약은 Application Out Port에 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application, Presentation, Infrastructure에 의존하지 않는다.
 * JPA mapping annotation은 현재의 Practical Clean Architecture 기준으로 Domain Model에 허용한다.</p>
 */
package com.myfitness.workout.domain;
