# 2026-09-29 아키텍처·패키지 정리 검증

기존 테스트 전략 문서에 섞여 있던 당시 실행 결과를 보존했다. 현재 규칙은 [Architecture](../../../reference/architecture.md), 실행 방법은 [Testing](../../../guides/testing.md)을 따른다. 아래 수치는 현재 빌드 결과나 배포 상태를 뜻하지 않는다.

2026-09-29 정리 검증: 아키텍처 24개 + 컨벤션 10개, 전체 build 205개 모두 통과했다. 이전 44개에서 중복 모듈 4개·문서 파일 존재 5개·문서 생성 1개를 분리/제거한 결과다. 보존한 17개 ArchUnit 검사 조건은 동일하다. 임시 금지 의존과 추가 공개 API로 Modulith의 실제 실패를 확인한 후 probe를 제거했다. 독립 문서 생성 작업도 테스트 실행 없이 19개 파일을 생성했다. 원격 CI/CD·배포는 실행하지 않았다.



2026-09-29 후속 Application 패키지 정리 검증(Java 21):

- `./gradlew checkstyleMain checkstyleTest test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon`: 아키텍처 25개·컨벤션 13개 통과. AST var 검사도 포함한다.
- 당시 원본 Router hash와 seed·1,000개 저장 판정 재현 검사는 통과했다. 해당 test-only 코드는 2026-09-30 실측 뒤 제거했고 변경 전 산출물은 보존했다.
- `./gradlew build modulithDocs --no-daemon`: 전체 209개 테스트 통과, module 문서 19개 생성. AI 답변 생성 실패의 503 계약·추적 로그와 외부 호출 중 transaction 비활성도 검증한다.
- 임시 운영 소스 5개로 Support → Service, Presentation → Support, 구 DTO 위치, Command/Result 위치, Response DTO Entity 노출을 넣어 6개 검사 실패를 확인한 뒤 제거했다. 허용 모듈 의존성과 Named Interface는 변경하지 않았다.
- 당시 실제 JEV 호출, 원격 CI/CD와 배포는 실행하지 않았다. 후속 실호출 결과는 [2026-09-30 보고서](../../../evaluations/ai-policy/jev-live-1000-v1/report.md)에 기록했다.

Infrastructure 후속 정리도 Java 21에서 검증했다. `./gradlew checkstyleMain checkstyleTest test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --tests 'com.myfitness.ai.infrastructure.*' --no-daemon`으로 59개 검사를 통과했고, `./gradlew build modulithDocs --no-daemon`으로 전체 209개와 문서 생성을 확인했다. AI SDK뿐 아니라 새 client 패키지의 JEV 구현 직접 참조도 금지 fixture로 탐지한다. JAR에는 새 client/module 경로만 포함되며 기존 springai/typesafe/query 경로는 없다. Client의 요청·응답 구현과 Named Interface·모듈 허용 의존성은 유지했다.
