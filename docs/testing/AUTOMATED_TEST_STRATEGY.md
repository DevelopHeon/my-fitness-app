# Automated Test Strategy

자동 테스트는 단순 코드 커버리지 확보가 아니라 **비즈니스 규칙을 검증하는 것**을 목적으로 한다.

## 1. 기본 원칙

각 Phase 구현 시 아래 순서를 따른다.

1. 해당 기능의 핵심 비즈니스 규칙을 테스트 케이스로 정의한다.
2. 도메인/서비스 단위 테스트를 먼저 작성한다.
3. 구현 후 해당 테스트를 통과시킨다.
4. Repository 또는 API 경계가 중요한 기능은 통합 테스트를 추가한다.
5. 전체 테스트와 빌드가 통과한 뒤에만 커밋·푸시한다.

테스트는 구현 세부사항보다 **사용자 관점의 규칙과 결과**를 검증한다.

테스트 코드 명명 규칙:
- 테스트 메서드명은 영문 camelCase로 작성한다.
- 모든 테스트에는 `@DisplayName`을 사용해 검증 의도를 사람이 읽기 쉬운 문장으로 작성한다.
- 한글 테스트 메서드명은 사용하지 않는다.

## 2. 테스트 우선순위

### 1순위: 비즈니스 규칙 단위 테스트
예:
- 완료된 Workout은 다시 완료 처리할 수 없는가
- 세트 순서와 값이 올바르게 저장되는가
- 다른 사용자의 Workout을 수정할 수 없는가
- 운동 Volume 계산이 정확한가
- Nutrition 목표 대비 잔여량 계산이 정확한가

### 2순위: 애플리케이션 서비스 테스트
예:
- Workout 생성 → 운동 추가 → 세트 기록 → 완료 흐름
- Routine으로 Workout 생성
- BodyRecord 동일 measuredAt 중복 방지와 measuredAt/id 결정적 정렬 기준
- Nutrition 일별 집계

### 3순위: Repository 통합 테스트
복잡한 조회, 기간 검색, 이전 기록 조회처럼 SQL/JPA 동작 자체가 중요한 경우 작성한다.

### 4순위: REST API 통합 테스트
핵심 사용자 흐름과 사용자 데이터 격리가 HTTP 경계에서도 유지되는지 검증한다.

JPA Entity를 Application Result로 변환하는 API는 클래스 레벨 `@Transactional` 테스트만으로 끝내지 않는다. 최소 한 개 이상의 비트랜잭션 MockMvc 테스트에서 요청별 transaction이 종료된 뒤에도 연관 데이터가 정상 응답되는지 확인한다. 이를 통해 `open-in-view: false` 환경의 LazyInitializationException을 테스트에서 놓치지 않는다.

### 5순위: Architecture 테스트

Spring Modulith와 ArchUnit의 책임을 분리한다.

Spring Modulith `ModulithArchitectureTest`:
- 모든 최상위 application module이 감지되는지 검증한다.
- `ApplicationModules.verify()`로 모듈 순환 의존성을 검증한다.
- 다른 모듈의 내부 패키지 접근을 금지하고 Named Interface만 공개한다.
- 최상위 `package-info.java`의 `allowedDependencies` 이외의 모듈 의존을 금지한다.
- 허용 의존성 목록과 Named Interface 이름 자체를 기대값으로 검증해 경계가 무심코 넓어지는 것을 막는다.

ArchUnit `LayerArchitectureTest`:
- Domain → 외부 계층, Application → Adapter, Presentation → Infrastructure/Service 구현 직접 의존을 금지한다.
- AI 내부의 외부 SDK/Adapter 직접 사용을 금지한다.
- Repository Out Port의 interface 선언과 Spring Data 비의존을 검증한다.
- 운영 클래스만 import하고 필수 검사 대상의 비어 있음을 확인한다.

`PersistenceBoundaryArchitectureTest`:
- Presentation/In Port의 Entity 직접 의존과 Result 필드의 Entity 노출을 금지한다.
- Entity는 Domain model, JpaRepository는 Infrastructure에 배치한다.
- In Port 구현 Service의 기본 read-only transaction 선언을 확인한다.

`convention.ComponentConventionTest` / `convention.JavaConventionTest`:
- Spring Service/Controller/Repository 어노테이션의 지정 패키지 배치를 검증한다.
- Java var 선언 금지는 Checkstyle Main/Test AST 검사로 강제하고 정상/위반 fixture로 규칙 자체를 확인한다.

`ArchitectureRuleDetectionTest` / `EntityBoundaryApiIntegrationTest`:
- 실제 운영 규칙으로 정상·금지 fixture와 빈 대상 실패를 확인한다.
- 테스트 수준 @Transactional 없이 요청 종료 뒤 projection을 확인해 lazy-loading 경계를 검증한다.

중복 모듈 규칙 4개는 Modulith 검증으로 통합했다. closed 모듈과 실제 공개 Named Interface 패키지를 고정해 Presentation/Infrastructure가 추가로 공개되지 않게 한다. package-info 파일 존재만 확인하는 문서 테스트는 제거하며 문서 파일과 module metadata는 유지한다. 다이어그램/canvas는 `./gradlew modulithDocs --no-daemon`으로 생성한다.

2026-09-29 정리 검증: 아키텍처 24개 + 컨벤션 10개, 전체 build 205개 모두 통과했다. 이전 44개에서 중복 모듈 4개·문서 파일 존재 5개·문서 생성 1개를 분리/제거한 결과다. 보존한 17개 ArchUnit 검사 조건은 동일하다. 임시 금지 의존과 추가 공개 API로 Modulith의 실제 실패를 확인한 후 probe를 제거했다. 독립 문서 생성 작업도 테스트 실행 없이 19개 파일을 생성했다. 원격 CI/CD·배포는 실행하지 않았다.

### 6순위: AI Coach 테스트
LLM 자연어 문장 자체가 아니라 Router 분류, 선택 Context, In/Out Port 호출, Provider Gateway 호출 여부와 데이터 근거를 검증한다.

## 3. 작성하지 않아도 되는 테스트

다음은 가치가 낮으면 억지로 작성하지 않는다.

- getter/setter 단순 호출
- JPA 기본 save 동작 자체
- 프레임워크가 보장하는 기능
- CSS 클래스 문자열의 정확한 일치
- LLM의 자연어 전체 문자열 일치

## 4. Phase 완료 조건

Phase는 다음 조건을 모두 만족해야 완료로 본다.

- 핵심 비즈니스 규칙 테스트 존재
- 해당 Phase 자동 테스트 통과
- 전체 백엔드 테스트 통과
- 프론트 변경이 있으면 lint/build 통과
- 통합 bootJar 빌드 통과
- 관련 spec/architecture 문서 업데이트
- 한글 커밋 및 GitHub push

수동 테스트는 사용자가 요청하거나 자동화가 어려운 UX/PWA/Ollama 검증이 필요한 경우 별도 문서로 작성한다.

## AI 정책 평가 (PR-012)

기본 `./gradlew test`는 외부 API 평가 tag를 제외하고 로컬 HTTP 계약·임계값·실패·생성 차단·이력·영속성 검증을 실행한다. 운영 아키텍처 검사 대상에는 테스트 fixture가 포함되지 않는다. 컨벤션·구조 검증 후 정책 동작 테스트를 실행하고 전체 backend/frontend 빌드를 확인한다.

Java 21 환경에서 명시적으로 실행한다:

```bash
./gradlew checkstyleMain checkstyleTest
./gradlew test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*'
./gradlew test --tests 'com.myfitness.ai.*'
./gradlew aiPolicyEval -PaiPolicyEval.mode=legacy
./gradlew aiPolicyEval -PaiPolicyEval.mode=jev-live -PaiPolicyEval.runs=3
./gradlew aiPolicyEval -PaiPolicyEval.mode=replay -PaiPolicyEval.replay=/absolute/path/to/cases.jsonl
```

live는 서버 환경의 `TYPESAFE_API_KEY`가 없으면 실패하며 대역으로 대체하지 않는다. custom dataset은 `-PaiPolicyEval.dataset=/absolute/path/to/corpus.jsonl`로 지정하고 두 검토자의 reviewed 라벨이 필요하다. 기본 seed는 24개 초안이며 승격 근거가 아니다. 1000개 합성 기준선도 초안이므로 `-PaiPolicyEval.allowDraft=true`를 명시해 실행한다. 50 family ×20 표현 변형이며 독립 표본 1000개로 해석하지 않는다. 저장된 [변경 전 판정](ai-policy/baseline-1000-v1/comparison.md)을 일반 테스트에서 hash와 판정별로 재현한다.

`build/reports/ai-policy/<batch-id>/run-N/`에 manifest/cases/metrics/comparison, batch root에 aggregate.json을 저장한다. 코드 SHA와 미커밋 source hash, dataset/questions/policy hash, 모델, 임계값, runtime, 실패와 unknown 사용량을 보존한다. 한국어와 위험 유형 slice, topic F1, coverage, 보정 오차, Wilson·family bootstrap CI와 같은 case의 반복 변동을 보고한다. replay에는 원격 지연·비용을 보고하지 않는다. 현재 도구 범위는 입력 gate이며 실제 생성 비용·E2E 노출 답변 위반률은 미측정이다.

마이그레이션 호환 테스트는 변경하지 않은 V6와 신규 V10 SQL을 H2에서 실행한다. TIMESTAMPTZ alias를 사용하는 H2 검증이며 실제 PostgreSQL/Flyway staging 검증을 대신하지 않는다.

Java var 금지는 가독성 컨벤션이다. `config/checkstyle/checkstyle.xml`의 MatchXpath가 `//TYPE/IDENT[@text='var']`를 검사한다. `convention.JavaConventionTest`가 local/final/for/enhanced-for/resource/lambda 위반과 이름·주석·문자열의 정상 사례를 검증한다. Checkstyle은 test 선행 작업과 check/build/CI에 연결된다. ArchUnit/Modulith 규칙과 예외를 완화하지 않는다. 배포 parameter 연결은 `python3 scripts/test-ai-policy-deploy.py`로 AWS/Docker 없이 검증한다.
