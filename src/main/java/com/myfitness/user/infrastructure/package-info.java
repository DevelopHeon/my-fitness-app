/**
 * user 모듈의 영속화와 외부 기술 연동을 담당하는 Infrastructure 계층이다.
 *
 * <p>Spring Data JPA, Repository Adapter, query adapter, bootstrap, 외부 API/provider 구현을 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application Port와 Domain Repository Port를 구현하며 Presentation을 직접 참조하지 않는다.</p>
 */
package com.myfitness.user.infrastructure;
