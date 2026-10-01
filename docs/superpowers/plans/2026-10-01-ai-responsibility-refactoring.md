# AI·식단 책임 분리 계획

**Goal:** API와 사용자 동작을 유지하면서 조율, 저장, 이미지 처리, 응답 변환, 입력 폼의 변경 이유를 분리한다.

**Architecture:** 기존 In/Out Port와 완화된 DDD 경계를 유지한다. 구체적인 협력 클래스만 추가하고 새로운 전략·범용 인터페이스는 만들지 않는다. main에서 진행한다.

**Spec:** `docs/spec/2026-10-01-thu-pr-013-food-photo-meal-recording.md`

1. AI 대화 관리와 텍스트 질문 처리를 별도 Service로 분리한다. 소유권 조회는 대화 Service에 모으고 메시지 DTO의 JSON 해석은 Support mapper로 옮긴다.
   - 검증: 기존 대화 CRUD·정책 거절·장애·메시지 이력 API 테스트.
2. 사진 저장은 전용 transaction Service로, 이미지 정규화와 모델 응답 해석은 Infrastructure 협력 클래스로 분리한다.
   - 검증: 잘못된 이미지의 무저장, 비음식·식별 불가, SDK 계약, 실패 코드, DB transaction 없는 외부 호출 테스트.
3. 식단 입력·목표 입력은 각각 상태를 소유하는 폼으로, AI 사진 선택·미리보기는 전용 컴포넌트로 분리한다. 화면은 API 호출과 화면 전환을 조율한다.
   - 검증: 기존 프론트 테스트, lint, TypeScript/정적 PWA 빌드.
4. 아키텍처 문서에 책임 분리·가독성 규칙과 실제 책임 지도를 기록한다.
   - 검증: Checkstyle(var 금지), ArchUnit, Modulith, 전체 build. 독립 리뷰에서 transaction 경계·미리보기 정리·입력 초깃값·오류 계약을 확인한다.

기능 추가나 정책 변경은 없다. 기존 테스트를 기준으로 리팩토링 전후를 확인하며 검사 규칙을 완화하지 않는다. 실제 모델 품질 평가는 이번 범위에 포함하지 않는다.

## 실행 결과

- 1~4 구현 완료. 기존 In/Out Port와 외부 호출의 NOT_SUPPORTED 경계를 유지했다.
- 변경 전: AI·Nutrition 동작 테스트와 frontendTest 통과.
- 변경 후: `./gradlew build --no-daemon` 통과. Java 212건(실패·오류·생략 0), 프론트 5건 통과. Checkstyle, ArchUnit·Modulith, TypeScript, 정적 PWA 빌드 포함.
- `cd frontend && npm run lint`, `git diff --check` 통과.
- 독립 읽기 전용 리뷰: 수정이 필요한 항목 없음. 사진 선택 수명, null/0 입력, 폼 초기화, 정책 및 transaction 경계를 확인했다.
- 실제 모델 품질·브라우저 실사용은 평가하지 않았다. main의 작업 내용으로 남겨 두었다.
