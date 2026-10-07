# ADR 0001 · 아키텍처와 소스 규칙의 자동 검사

- 상태: Accepted
- 정리일: 2026-10-07. 2026-09-29 정리 당시 기록과 현재 코드를 근거로 작성한 회고 기록이다.

## 배경과 대안

AI가 생성한 코드의 의존 방향·패키지·DTO·책임 배치를 반복해서 눈으로 확인하고 리팩토링하는 비용이 있었다.
Markdown은 기준을 설명하지만 긴 대화에서 누락되거나 같은 구조 위반이 다시 발생할 수 있다.
사람 검수와 문서만 유지하는 방법, 모든 조건을 ArchUnit에 넣는 방법을 비교했다.
소스의 var 선언은 bytecode 의존 검사로 충분히 검증할 수 없어 검사별 책임을 구분했다.

## 결정

모듈 공개 범위·허용 의존성·순환은 Spring Modulith, 내부 계층·영속성 경계는 ArchUnit,
Java var 선언 금지는 AST 기반 Checkstyle로 강제한다. 규칙 자체의 금지 fixture 탐지 검사도 유지한다.
규칙별 소유 검사와 현재 패키지 계약은 [Architecture](../architecture.md#검사의-책임)에 둔다.

## 결과와 비용

반복되는 구조 검수를 빌드 실패로 드러낼 수 있다. 문서만으로 규칙 준수를 보장하지 않는다.
검사도 유지보수가 필요하며 실제 정책 변경에서는 이유와 함께 수정해야 한다.
허용 의존성을 좁히거나 DTO를 무조건 격리하는 정책을 추가한 것은 아니다. 기존 완화된 DDD·Port-Adapter 경계를 유지한다.
사업 규칙·런타임 transaction·모델 품질·UI는 각각 필요한 동작 검사와 실측으로 확인한다.

## 근거

- [당시 패키지·검사 정리와 금지 의존 탐지 검증](../archive/specs/2026-09-29-architecture-package-cleanup-validation.md)
- [책임 분리 후속 작업](../archive/specs/2026-10-01-ai-responsibility-refactoring-spec.md)
- [현재 검사 실행과 검증 범위](../testing.md)
