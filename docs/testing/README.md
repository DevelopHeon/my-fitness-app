# Testing Guide

이 디렉터리는 프로젝트의 테스트 전략과 요청 시 사람이 직접 검증할 수 있는 수동 테스트 절차를 기록한다.

- AI 정책 평가·검증 결과: [JEV 실호출 1,000건](./ai-policy/2026-09-30-jev-live-1000-results.md) · [정책 수정 후 저장 응답 재평가](./ai-policy/2026-09-30-policy-rule-replay.md) · [구현 검증 기록](./ai-policy/2026-09-29-jev-single-path-results.md)
- 자동 테스트 원칙: [AUTOMATED_TEST_STRATEGY.md](./AUTOMATED_TEST_STRATEGY.md)
- 수동 테스트 템플릿: [MANUAL_TEST_TEMPLATE.md](./MANUAL_TEST_TEMPLATE.md)

아키텍처/컨벤션 검사는 `./gradlew test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon`으로 함께 실행한다. 모듈 문서 생성은 `./gradlew modulithDocs --no-daemon`이며 일반 테스트에 문서 생성을 섞지 않는다. [검사별 책임](../architecture/README.md#아키텍처컨벤션-검사의-책임)을 참고한다.

자동 테스트는 비즈니스 규칙 검증을 기본으로 하며, 수동 테스트는 실제 사용자 흐름, 모바일 UX, PWA 동작, Ollama 응답처럼 자동화가 어려운 영역을 보완한다.

## 작성 시점

다음 경우 수동 테스트 문서를 추가한다.

- 사용자가 수동 테스트 방법을 요청한 경우
- 자동 테스트만으로 확인하기 어려운 UI/PWA 변경이 있는 경우
- Ollama나 외부 프로세스 연동처럼 실제 실행 환경 확인이 필요한 경우
- 배포 전 핵심 사용자 흐름을 검증해야 하는 경우

## 파일명

```text
YYYY-MM-DD-기능-manual-test.md
```

예시:

```text
2026-09-18-workout-manual-test.md
2026-09-18-pwa-install-manual-test.md
```

## 문서 구성

각 수동 테스트 문서는 아래 항목을 포함한다.

1. 테스트 목적
2. 사전 조건
3. 실행 환경
4. 테스트 데이터
5. 단계별 수행 절차
6. 기대 결과
7. 실패 시 확인 항목
8. 테스트 결과 기록

## 원칙

- 개발자가 그대로 따라 할 수 있는 명령과 화면 동작을 작성한다.
- DB를 직접 수정하는 절차보다 실제 API/UI 흐름을 우선한다.
- destructive 작업은 명확하게 표시한다.
- 비밀번호, 토큰, 실제 개인정보는 문서에 기록하지 않는다.
- AI 테스트는 문장 일치가 아니라 Tool 사용 여부, 데이터 근거, 금지 범위 준수를 확인한다.

필요한 기능의 수동 테스트를 요청받으면 이 디렉터리에 별도 문서를 추가한다.
