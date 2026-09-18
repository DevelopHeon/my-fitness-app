# my-fitness-app

개인 운동, 신체, 식단 기록과 로컬 AI 코치를 위한 개인용 피트니스 애플리케이션입니다.

## 기술 스택

- Backend: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security
- Frontend: Next.js 16.3.3, TypeScript, Tailwind CSS, PWA
- Database: PostgreSQL 17
- AI: Spring AI + Ollama (Phase 6 예정)
- Build: Gradle + npm

## 프로젝트 구조

```text
.
├── src/                # Spring Boot
├── frontend/           # Next.js PWA
├── docs/
│   ├── spec/
│   ├── architecture/
│   └── testing/
├── docker-compose.yml
└── build.gradle.kts
```

## 로컬 실행

PostgreSQL 실행:

```bash
docker compose up -d postgres
```

백엔드 실행:

```bash
./gradlew bootRun
```

프론트엔드 개발 서버:

```bash
cd frontend
npm install
npm run dev
```

## 통합 빌드

```bash
./gradlew bootJar
```

통합 빌드 시 Next.js 정적 export 결과가 Spring Boot JAR의 정적 리소스에 포함됩니다.

## 문서

- 제품 스펙: `docs/spec/PRODUCT_SPEC.md`
- 구현 PR 스펙: `docs/spec/`
- 아키텍처: `docs/architecture/README.md`
- 수동 테스트: `docs/testing/`

## 개발 원칙

기능 개발은 Phase 단위로 진행합니다.

1. 해당 Phase의 핵심 비즈니스 규칙을 테스트로 정의
2. 자동 테스트를 작성하면서 기능 구현
3. 관련 spec / architecture 문서 업데이트
4. 전체 테스트, 프론트 lint/build, 통합 bootJar 확인
5. 한글 커밋
6. GitHub push
7. 작업 중단

## 현재 진행 상태

- [x] Phase 1 Workout 기록
- [ ] Phase 2 Routine
- [ ] Phase 3 BodyRecord
- [ ] Phase 4 Dashboard
- [ ] Phase 5 Nutrition
- [ ] Phase 6 Local AI Coach

Phase 1 개발 환경에서는 인증 구현 전까지 `X-User-Id: 1`을 임시 사용자 컨텍스트로 사용합니다.

다음 작업은 Phase 2 Routine 구현입니다.
