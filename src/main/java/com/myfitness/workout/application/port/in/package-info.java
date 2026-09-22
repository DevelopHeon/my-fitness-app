/**
 * Workout Presentation Adapter가 호출하는 입력 Port다.
 *
 * <p>Controller는 Application Service 구현체가 아니라 이 패키지의 Use Case 계약을 의존한다.
 * 다른 모듈용 공개 API는 routine, insight 같은 별도 Named Interface 패키지로 분리한다.</p>
 *
 * <p>In Port는 JPA Entity를 입력/출력 계약으로 노출하지 않는다.
 * 조회/변경 결과는 Application Result projection으로 변환한 뒤 반환한다.</p>
 */
package com.myfitness.workout.application.port.in;
