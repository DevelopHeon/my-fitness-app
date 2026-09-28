/**
 * Dashboard Application Out Port를 다른 모듈의 공개 Insight In Port에 연결하는 Infrastructure 계층이다.
 *
 * <p>현재 Dashboard 자체 Repository는 두지 않고 Workout/Body의 read-only API를 조합하는 query adapter를 둔다.</p>
 *
 * <p><strong>의존성 규칙:</strong> 다른 모듈의 Infrastructure나 Presentation을 직접 참조하지 않고 공개 Named Interface만 사용한다.</p>
 */
package com.myfitness.dashboard.infrastructure;
