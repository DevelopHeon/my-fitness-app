/**
 * routine 모듈의 외부 요청과 응답을 담당하는 Presentation 계층이다.
 *
 * <p>Controller, request/response DTO, Bean Validation과 외부 표현 변환을 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> Application과 필요한 Domain 타입에는 의존할 수 있지만 Infrastructure를 직접 참조하지 않는다. 다른 기능 모듈의 Presentation DTO를 재사용하지 않는다.</p>
 */
package com.myfitness.routine.presentation;
