# ADR 0005 · 공개 진입과 내부 업무·영속 계약 분리

- 상태 — Accepted
- 결정일 — 2026-11-27

## 배경과 대안

기존 Application Service가 공개 진입과 업무 조율을 맡았다. 운동 카탈로그는 다른 모듈에 자체 Domain 값을
공개했고, Repository Port는 상태 변경과 검색 계약을 같은 역할로 표현했다. 기존 구조에서 의존 관계만
정리하는 대안과 공개 값·내부 업무·Aggregate 영속 계약을 역할별로 분리하는 대안을 검토했다.

## 결정

Input Port는 공개 값으로 진입을 제공한다. PortService가 값을 변환해 내부 UseCase에 위임하고 업무 Service가
트랜잭션 범위를 소유한다. Aggregate를 보존하는 계약은 Domain Repository에 두며 검색·집계와 모듈 협력은
Application Output Port에서 정의한다. 모듈 경계를 넘는 Adapter가 상대 공개 값을 자신의 모델로 변환한다.

## 결과와 비용

공개 타입이 내부 모델에서 독립적으로 바뀔 수 있다. 그만큼 DTO와 값 변환, 경계에 대한 검증 코드가 추가된다.
운동과 루틴은 각자의 snapshot으로 기존 DB 문자열을 읽고 기록 당시의 종목 정보를 보존한다.

[ADR 0001](0001-architecture-guards.md)의 자동 검사 결정을 확장한다. Modulith는 공개 범위와 모듈 의존을 검사하고
ArchUnit은 PortService, Input Adapter, Domain Repository 경계를 검사한다.

## 근거

- [현재 구조와 검사의 책임](../architecture.md)
- [전환 Spec과 인수 기준](../archive/specs/2026-11-27-backend-port-usecase-repository-spec.md)
