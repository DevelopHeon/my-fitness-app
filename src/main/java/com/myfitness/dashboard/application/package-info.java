/**
 * dashboard 모듈의 사용자 Use Case와 도메인 간 orchestration을 담당하는 Application 계층이다.
 *
 * <p>port.in은 외부가 Application을 호출하는 입력 계약이고, port.out은 Application이 외부 자원을 호출하기 위해 요구하는 출력 계약이다.</p>
 *
 * <p>Transaction boundary, command/result, application port, 조회·권한 수준 예외를 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Domain과 다른 모듈의 공개 Application Use Case에는 의존할 수 있지만 Presentation DTO나 Infrastructure 구현을 직접 참조하지 않는다.</p>
 */
package com.myfitness.dashboard.application;
