/**
 * Exercise 모듈로 들어오는 Use Case Port다.
 *
 * <p>Presentation Adapter가 호출하는 입력 계약을 두며,
 * 다른 모듈에 공개할 계약은 별도 Named Interface 하위 패키지로 분리한다.</p>
 *
 * <p>In Port는 JPA Entity를 입력/출력 계약으로 노출하지 않는다.
 * 외부에 필요한 값은 value object 또는 Application Result projection으로 전달한다.</p>
 */
package com.myfitness.exercise.application.port.in;
