# 백엔드 PortService·내부 UseCase·Domain Repository 전환

- 상태 — 구현과 로컬 검증 완료
- 현재 구조와 결정 — [Architecture](../../architecture.md), [ADR 0005](../../adr/0005-backend-port-usecase-repository.md)
- My Fitness 기준 — `main`의 `fc94994b709d97f2dadd55deb3cda208df05fd4c`
- Wiki 기준 — `0928ba5d7d6d487127f86b40884b55ded375f7ed`의 `wiki/standards/ddd-hexagonal.md`, `wiki/concepts/module-boundaries-and-contracts.md`, `wiki/projects/my-fitness.md`
- 사례 근거 — [Limited Ticketing의 모듈 경계](https://github.com/DevelopHeon/limited-ticketing/blob/bd958582a9decf2f9e4989c638588a2cd816fc99/docs/architecture/module-boundaries.md), [Domain Repository ADR](https://github.com/DevelopHeon/limited-ticketing/blob/bd958582a9decf2f9e4989c638588a2cd816fc99/docs/adr/0009-domain-repository-ownership.md)

## 목표와 범위

현재 업무 Service가 함께 맡는 공개 진입, 업무 조율, 저장소 호출을 역할별로 배치한다. `user`, `exercise`, `workout`, `routine`, `body`, `nutrition`, `dashboard`, `ai`의 업무 진입에 적용한다. `common`은 공통 기술 설정·HTTP 오류 변환을 소유한다.

공통 DDD·헥사고날 기준만 적용하는 안은 현재 패키지와 공개 진입을 대부분 유지할 수 있다. 이번 작업은 사용자가 선택한 Limited Ticketing 사례의 역할 구분을 적용한다. 공개 DTO 변환과 내부 UseCase 위임 코드가 추가된다. 신체 기록에서 경계를 검증한 뒤 같은 방식으로 순차 전환한다. Controller와 외부 구현은 저장소의 `presentation`, `infrastructure` 패키지 관례를 따른다.

## 목표 구조와 계약

| 역할 | 책임과 위치 |
| --- | --- |
| Input Adapter | `presentation`의 HTTP, `infrastructure/security`의 OIDC, `infrastructure/bootstrap`의 기본 운동 초기화가 자기 모듈의 Input Port를 호출한다. |
| Input Port와 공개 DTO | `application/port/in`의 진입 계약이다. HTTP DTO, Application 내부 DTO, Entity와 Domain 내부 타입에 의존하지 않는다. 기존 공개 Java 계약은 아래 호환성 범위에서 전환한다. |
| PortService | `application/service`에서 Input Port를 구현한다. 공개·내부 DTO 변환과 내부 UseCase의 실행 순서를 맡는다. Domain, Domain Repository, Output Port, 구체 업무 Service를 직접 참조하지 않는다. |
| 내부 UseCase와 업무 Service | `application/usecase`에 내부 계약과 필요한 입력·결과를 둔다. `application/service`의 업무 Service가 이를 구현하고 업무·transaction을 조율한다. PortService와 업무 Service 사이에서 오가는 타입은 내부 입력·결과의 값으로 표현한다. |
| Domain Repository | `domain/repository`가 자기 업무 모델의 저장·조회·정합성 계약을 소유한다. 순수 Java interface로 두고 Persistence Adapter가 직접 구현한다. 기존 Repository Port의 메서드는 이름으로 일괄 이동하지 않고 목적을 확인한다. |
| Application Output Port | `application/port/out`은 검색·집계용 조회, 다른 모듈 호출, 정책·모델·사진 분석 같은 외부 기능을 표현한다. 필요한 결과 타입을 호출 모듈이 소유하고 Output Adapter가 변환한다. |

기존 `application/port/in`의 `*UseCase` 이름은 공개 진입 역할을 드러내는 `*Port` 계열 이름으로 맞추고 `*UseCase`는 내부 업무 계약에 사용한다. 업무 Service 사이에는 필요한 내부 UseCase로 연결한다. 동일한 계약의 중계 Adapter나 별도 Facade는 두지 않는다. 입력과 결과의 의미가 같은 내부 호출에는 값 타입을 재사용한다. HTTP 입력·응답 DTO는 `presentation`이 계속 소유한다. 기존 JPA Entity 매핑과 package-private Spring Data Repository 관례를 유지한다.

예를 들어 `BodyRecord` 저장·수정·삭제와 측정 시각 중복 확인은 Domain Repository가 소유한다. 운동의 이전 완료 기록 조회(`WorkoutExerciseRepositoryPort`)처럼 화면·통계에 필요한 검색은 Application Output Port와 projection을 사용한다. 같은 Persistence Adapter가 두 역할을 구현할 수 있다.

### 모듈 협력과 데이터 소유권

- 다른 업무 모듈의 호출은 `자기 Output Port → Module Adapter → 상대 Input Port`로 연결한다. `workout`의 운동 조회와 `routine`의 운동 조회·운동 시작처럼 Application에서 직접 상대 Port를 호출하는 곳을 Adapter로 옮긴다. `dashboard`와 `ai`의 기존 조회 Adapter 패턴을 재사용한다.
- `exercise::domain-model` 공개를 제거한다. 카탈로그 Input Port는 운동 종류·ID·이름·카테고리·소유권 판단에 필요한 값을 공개 계약으로 전달한다. `workout`과 `routine`은 Adapter에서 자기 값으로 변환하고 이름·카테고리 snapshot을 자기 모델에 저장한다. 루틴이 운동을 시작할 때도 공개 DTO로 snapshot을 전달한다. 종류·카테고리의 저장 문자열과 HTTP 값, 카탈로그 접근 제한과 과거 기록 정합성을 유지한다.
- Module Adapter는 상대 모듈의 DB나 Domain Repository에 접근하지 않는다. 각 모듈이 자기 테이블 쓰기를 맡는다. 루틴 기반 운동은 `workout`이 저장하고 저장 단위와 실패 전파를 보존한다. AI 사진 분석·대화 저장과 `nutrition`의 식단 등록은 기존 사용 흐름과 각각의 쓰기 책임을 유지한다.
- Spring Modulith Named Interface는 필요한 공개 입력·결과만 노출한다. `common`의 기존 기술적 예외 매핑은 유지하고 같은 HTTP 상태 및 오류 코드를 검증한다.
- `DefaultExerciseInitializer`는 앱 시작 트리거를 담당하고 모듈 외부에 공개하지 않는 Input Port를 호출한다. 초기 카탈로그 값과 이름 기준 중복 방지, 쓰기 transaction을 유지한다.

### 트랜잭션과 호환성

- 기존 transaction 범위를 업무 Service 또는 이미 역할이 분리된 transaction Service에 배치한다. `readOnly`, 전파 설정, 쓰기 원자성, 예외 시 rollback, JPA lazy 접근을 확인한다. Entity는 열린 transaction 안에서 필요한 값으로 변환한다. 여러 내부 UseCase를 조합할 때에는 기존 원자성을 소유하는 업무 UseCase를 사용한다.
- AI 정책·모델·사진 분석 호출의 transaction 비활성 경계와 호출 앞뒤의 짧은 저장 transaction을 보존한다. 분리 후 실제 Spring Bean 호출을 통해 기존 전파가 적용되는지 테스트로 확인한다.
- 전환은 단일 Spring Boot 애플리케이션 내부에서 진행한다. 공개 Java Port·DTO·Named Interface 변경은 제공자와 소비자를 같은 변경 단위에서 전환한다. HTTP 경로·메서드·JSON·오류·인증, DB 스키마·Flyway migration, PWA와 배포 구성을 유지한다. 전환 뒤에도 기존 저장값을 읽을 수 있어야 한다. Gradle 멀티 프로젝트와 비동기 처리는 이번 범위 밖이다.

## 구현 순서

1. **신체 기록** — `body`의 HTTP·조회 진입, PortService, 내부 UseCase, Domain Repository와 Adapter를 전환한다. 중복 측정 시각, 사용자 소유권, 기간 조회와 응답 projection을 기존 API 테스트에서 확인한다. 전환 대상에서 새 구조가 확인되도록 검사하고, 아직 전환되지 않은 다른 모듈에는 기존 검사를 적용한다.
2. **운동·루틴·운동 카탈로그** — 공개 DTO와 Module Adapter를 함께 전환한다. workout snapshot, 사용자별 커스텀 운동 접근, 루틴 기반 운동 저장·실패 처리와 과거 응답을 확인한다. `exercise::domain-model` 참조를 제거한 뒤 Modulith 검사를 실행한다.
3. **나머지 업무 모듈** — `user`, `nutrition`, `dashboard`, `ai`를 전환한다. OIDC 진입, 식단 일괄 rollback과 null/0 집계, 통계 조회, AI 정책 차단 및 저장 경계를 확인한다. 최종 구조 검사에서 단계별 대상 제한을 제거해 모든 업무 모듈을 검사한다.
4. **문서와 검증** — `docs/architecture.md`의 현재 구조와 `docs/testing.md`의 검사 설명을 실제 구현에 맞춘다. 새 ADR에 공개 진입·영속 계약의 선택과 비용을 기록하고 ADR 0001의 자동 검사 결정과 연결한다. 검증 후 유효 내용을 현재 문서에 반영하고 이 Spec을 저장소 관례에 따라 archive로 이동한다.

## 인수 기준과 검증

- Spring Modulith가 허용되지 않은 모듈 의존, 내부 계약의 외부 사용, 순환과 `exercise::domain-model` 노출을 검출한다. ArchUnit이 PortService의 Domain·저장소·Output Port·구체 Service 의존, Input Adapter의 내부 호출 우회와 Domain Repository의 Spring Data·JPA·Application 의존을 검출한다. 기존 JPA Entity 비노출·Layer·AI SDK·Checkstyle 규칙도 유지한다. 새 금지 경로는 위반 fixture를 규칙에 넣어 실패를 재현하고 정상 fixture 및 운영 클래스의 통과를 확인한다.
- 기존 동작 테스트를 유지한다. 특히 `EntityBoundaryApiIntegrationTest`·`RoutineLazyLoadingApiIntegrationTest`로 요청 종료 후 응답 projection, `WorkoutApiIntegrationTest`·`RoutineApiIntegrationTest`로 모듈 협력과 기록, `BodyRecordApiIntegrationTest`로 사용자 격리·중복, `MealBatchTransactionIntegrationTest`로 rollback, `UserSecurityIntegrationTest`로 인증, AI API·정책·저장 테스트로 호출 차단과 저장 경계를 확인한다. 기존 테스트가 확인하지 않는 트랜잭션 경계가 드러나면 해당 회귀 테스트를 추가한다.
- [Testing](../../testing.md#기본-검증)의 환경과 명령을 사용한다. H2 및 테스트 대역으로 검증한 결과를 운영 PostgreSQL·Google·JEV·OpenAI·Ollama 확인과 구분한다. 실제 공급자 평가와 운영 배포는 이번 인수 조건에 포함하지 않는다.

```bash
./gradlew checkstyleMain checkstyleTest --no-daemon
./gradlew test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon
./gradlew test --no-daemon
./gradlew build --no-daemon
./gradlew modulithDocs --no-daemon
git diff --check
```

## 실행 결과

Java 21.0.11과 Node 22.23.4에서 기준 동작을 먼저 확인한 뒤 아래 단계로 전환했다.
각 단계에서 예상한 구조 위반을 재현했고, 정상·위반 fixture와 수정한 운영 클래스가 기대한 결과를 냈다.

| 단계 | 구현 전 실패 | 전환 후 확인 |
| --- | --- | --- |
| 신체 기록 | 공개 DTO와 Input Adapter 경계 위반, PortService와 Domain Repository 검사 대상 부재로 4건 실패 | 신체 기록·아키텍처·컨벤션 53개 통과. 소유권·중복·기간 조회와 요청 종료 후 projection 확인 |
| 운동·루틴·카탈로그 | 공개 DTO와 Input Adapter 경계, 기존 Named Interface와 허용 의존 차이로 4건 실패 | 카탈로그·운동·루틴 검사 통과. 카탈로그 수정 후 snapshot, 직전 기록 조회 실패 시 rollback, 초기화 2회 뒤 카탈로그와 ID 보존 확인 |
| 인증·영양·통계·AI | 공개 DTO와 Input Adapter 경계, AI Application의 타 모듈 직접 의존으로 3건 실패 | 대상 검사 통과. 잘못된 OIDC 입력·사용자 격리, 식단 일괄 rollback, AI 정책·모델·사진 호출과 저장 경계 확인 |

아래 단계별 테스트 명령은 전환 후 모두 종료 코드 `0`이었다.

```bash
./gradlew test --tests 'com.myfitness.body.*' --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon
./gradlew test --tests 'com.myfitness.exercise.*' --tests 'com.myfitness.workout.*' --tests 'com.myfitness.routine.*' --tests 'com.myfitness.architecture.*' --no-daemon
./gradlew test --tests 'com.myfitness.user.*' --tests 'com.myfitness.nutrition.*' --tests 'com.myfitness.dashboard.*' --tests 'com.myfitness.ai.*' --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon
```

초기 구현 검증의 `checkstyleMain checkstyleTest`, `test`, `build`, `modulithDocs`와 `git diff --check`는 모두 종료 코드 `0`이었다.
백엔드 테스트 247개와 frontend Node 테스트 26개를 통과했다. BootJar에 PWA 자산이 포함됐고 Modulith 문서가 생성됐다.
검증에는 H2와 테스트 대역을 사용했다. 운영 PostgreSQL, Google·JEV·OpenAI·Ollama 실호출,
운영 배포는 검증 범위 밖이다.

## 리뷰 후 검증

운동·루틴·영양 업무와 AI 대화·저장 조율은 내부 UseCase로 연결했다. 기존 트랜잭션을 소유하는
업무·저장 Service의 기본 read-only와 쓰기 경계를 유지했다. AI 사용자 메시지 저장 결과는 값으로
전달하고, 분류 시 기존 쓰기 트랜잭션 안에서 대화 ID와 메시지 ID로 대상을 조회한다.

내부 DTO를 `application/usecase` 아래로 옮기고 사진 업로드 값은 `FoodPhotoGateway`에서 소유한다.
`PortBoundaryArchitectureTest`에서 수정 전 구체 Service 의존과 이전 DTO 패키지 검사 2건이 예상대로
실패했다. 수정 후 두 규칙은 금지 fixture를 탐지했고 정상 fixture와 운영 코드는 통과했다.

| 명령 | 종료 코드 | 결과 |
| --- | --- | --- |
| `./gradlew test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon` | 0 | 아키텍처·컨벤션과 fixture 통과 |
| `./gradlew test --no-daemon` | 0 | Java 21 백엔드 테스트 250개 통과 |
| `./gradlew modulithDocs --no-daemon` | 0 | 모듈 문서 생성 |
| `./gradlew build --no-daemon` 재시도 | 1 | Next.js 16.4.3이 정적 페이지를 생성한 뒤 자체 `SecurityFatalError`로 `:frontendBuild` 종료 |
| `./gradlew frontendTest --no-daemon` | 0 | Node 테스트 26개 통과 |
| `git diff --check` | 0 | 변경 공백 검사 통과 |

패키지 재설치 직후 첫 빌드도 같은 Next.js 보안 알림으로 종료 코드 1이었다.

### 최종 통합 확인

Java 21.0.11과 Node 22.23.4에서 Checkstyle, 전체 백엔드·프론트엔드 테스트와 `modulithDocs`를 다시 실행했다.
백엔드 250개 중 아키텍처·컨벤션 검사 47개를 포함해 모두 통과했고 프론트엔드 테스트 26개도 통과했다.
읽기 전용 재리뷰에서 두 지적의 해소를 확인했다.

같은 작업 트리에서 `./gradlew build --no-daemon --console=plain`은 종료 코드 `0`이었다.
`:frontendBuild`와 `:bootJar`가 실행됐으며 `build/libs/app.jar`에서 PWA의 HTML·manifest·JavaScript와
전환된 백엔드 클래스를 확인했다. 기준 SHA의 Next.js 16.4.3으로 빌드했으며 보안 알림 대상 버전이다.
