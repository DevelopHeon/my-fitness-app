/**
 * Google 로그인과 현재 사용자 조회 Use Case를 orchestration하는 User Application 계층이다.
 *
 * <p>Google OIDC 기술 타입을 직접 노출하지 않고 command/result와 In/Out Port를 통해 Domain 및 Infrastructure와 연결한다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Domain에는 의존할 수 있지만 Presentation DTO나 Infrastructure 구현을 직접 참조하지 않는다.</p>
 */
package com.myfitness.user.application;
