/**
 * user 모듈의 사용자 Use Case와 도메인 간 orchestration을 담당하는 Application 계층이다.
 *
 * <p>Transaction boundary, command/result, application port, 조회·권한 수준 예외를 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Domain과 다른 모듈의 공개 Application Use Case에는 의존할 수 있지만 Presentation DTO나 Infrastructure 구현을 직접 참조하지 않는다.</p>
 */
package com.myfitness.user.application;
