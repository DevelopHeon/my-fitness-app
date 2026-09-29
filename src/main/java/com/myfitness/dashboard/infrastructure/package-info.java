/**
 * Dashboard Application Out Port를 다른 모듈의 공개 Insight In Port에 연결하는 Infrastructure 계층이다.
 *
 * <p>module 패키지에서 Workout/Body의 read-only API를 호출하고 Dashboard 데이터 계약으로 변환한다.
 * 현재 Dashboard 자체 Repository는 두지 않는다.</p>
 *
 * <p><strong>의존성 규칙:</strong> 다른 모듈의 Infrastructure나 Presentation을 직접 참조하지 않고 공개 Named Interface만 사용한다.</p>
 */
package com.myfitness.dashboard.infrastructure;
