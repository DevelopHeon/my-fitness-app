# My Fitness

개인 운동, 신체, 식단 기록과 기록 기반 AI Coach를 제공하는 개인용 피트니스 애플리케이션입니다.

## 기술 스택

- Backend: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security, Spring Modulith
- Frontend: Next.js 16.3.3, TypeScript, Tailwind CSS, PWA
- Database: PostgreSQL 17, Flyway
- Authentication: Google OAuth2 / OIDC, Spring Session JDBC
- AI: Spring AI 2.0.1, OpenAI 또는 Ollama ChatModel
- Architecture: Modular Monolith, 4-layer, In/Out Port
- Infrastructure: AWS CDK, EC2, RDS, ECR, SSM, GitHub Actions OIDC

## 프로젝트 구조

~~~text
.
├── src/                    Spring Boot backend
├── frontend/               Next.js PWA
├── infra/                  AWS CDK source
├── scripts/                EC2 deploy / Caddy scripts
├── docs/
│   ├── architecture/       현재 소프트웨어 구조
│   ├── infra/              현재 AWS 구성과 운영 방법
│   ├── spec/               제품 스펙과 구현 이력
│   └── testing/            테스트 전략과 수동 테스트
└── README.md
~~~

## 로컬 실행

PostgreSQL을 실행합니다.

~~~bash
docker compose up -d postgres
~~~

백엔드를 실행합니다.

~~~bash
./gradlew bootRun
~~~

프론트엔드 개발 서버를 별도로 실행할 때는 다음을 사용합니다.

~~~bash
cd frontend
npm install
npm run dev
~~~

Google 로그인은 로컬 OAuth Client 설정이 필요합니다.

~~~bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
~~~

AI Coach는 Provider를 설정하지 않으면 비활성화되고 나머지 기능은 정상 동작합니다.

OpenAI:

~~~bash
export AI_PROVIDER=openai
export OPENAI_API_KEY=...
./gradlew bootRun
~~~

로컬 Ollama:

~~~bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
./gradlew bootRun
~~~

## 빌드와 검증

전체 검증:

~~~bash
./gradlew test --no-daemon
./gradlew build --no-daemon
~~~

프론트엔드:

~~~bash
cd frontend
npm run lint
npm run build
~~~

CDK:

~~~bash
cd infra
npm ci
npm run build
npm test -- --runInBand
npx cdk synth
npx cdk diff
~~~

Gradle 애플리케이션 빌드는 Next.js 정적 export를 포함하며 최종 Spring Boot JAR에서 같은 origin으로 제공합니다.

## 문서 읽는 순서

문서 전체 안내는 [docs/README.md](docs/README.md)를 기준으로 합니다.

1. [Architecture](docs/architecture/README.md) - 시스템과 모듈 구조
2. [Infrastructure](docs/infra/README.md) - AWS 운영 구성
3. [Operations](docs/infra/OPERATIONS.md) - 배포, 접속, 로그, 장애 확인
4. [Product Spec](docs/spec/PRODUCT_SPEC.md) - 제품 기능 범위
5. [Testing](docs/testing/README.md) - 테스트 기준
6. [Implementation Specs](docs/spec/README.md) - 구현 시점별 설계 기록

구현 시점의 PR 스펙은 이력 문서입니다. 현재 구조가 궁금할 때는 Architecture와 Infrastructure 문서를 우선합니다.
