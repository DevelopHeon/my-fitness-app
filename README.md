# My Fitness

개인 운동, 신체, 식단 기록과 기록 기반 AI Coach를 제공하는 개인용 피트니스 애플리케이션입니다.

## 식단 기록

음식명과 섭취 칼로리를 바로 입력합니다. 별도 음식 등록은 없습니다. 날짜와 아침·점심·저녁·간식은 현재 한국 시간으로 채우고 사용자가 수정합니다. 탄단지는 기본 접힘 상태의 선택 입력이며, 미입력 값은 0으로 계산하지 않습니다.

식단은 일별 조회·캘린더 조회와 등록·수정 화면을 분리합니다. 캘린더에는 날짜별 총 칼로리와 등록한 식사 구분을 보여주며 날짜를 선택하면 일별 목록으로 이동합니다. **사진으로 식단 입력**에서 사진을 선택하면 등록 화면에서 자동 분석하고, 여러 음식의 이름·1인분 추정 칼로리를 채웁니다. 음식별 수정·제외 후 공통 날짜·식사 구분으로 한 번에 저장합니다. 사진 분석만으로 자동 저장하지 않으며, 기존 AI 대화의 분석 결과도 목록 전체를 입력 화면으로 가져올 수 있습니다.

사진은 JPEG/PNG 한 장, 원본 5 MiB·1600만 픽셀 이하입니다. OpenAI 이미지 입력으로 판별·추정을 한 번 요청하며, 비음식·식별 불가에는 기록 버튼을 보여주지 않습니다. 앱은 원본을 보관하지 않고 검증된 분석 결과만 대화에 저장합니다. 사진 속 실제 섭취량을 측정한 값이나 공식 영양정보는 아닙니다.

## 기술 스택

- Backend: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security, Spring Modulith
- Frontend: Next.js 16.3.3, TypeScript, Tailwind CSS, PWA
- Database: PostgreSQL 17, Flyway
- Authentication: Google OAuth2 / OIDC, Spring Session JDBC
- AI: Spring AI 2.0.1, 일반 대화 JEV 정책 평가 + OpenAI/Ollama 답변 생성, 사진 분석 OpenAI 전용
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

Provider 미설정 시 AI 요청은 503을 반환합니다. 직접 식단 기록 등 나머지 기능은 사용할 수 있습니다. 일반 텍스트 대화는 JEV 평가를 반드시 거치므로 `TYPESAFE_API_KEY`도 필요합니다. JEV 평가 불가는 `AI_POLICY_UNAVAILABLE / 503`, 사진 분석 장애는 `AI_PROVIDER_UNAVAILABLE / 503`입니다.

OpenAI:

~~~bash
export AI_PROVIDER=openai
export OPENAI_API_KEY=...
export TYPESAFE_API_KEY=...
# 선택: AI_OPENAI_MODEL=gpt-4o-mini, AI_REQUEST_TIMEOUT=30s
./gradlew bootRun
~~~

로컬 Ollama:

~~~bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export TYPESAFE_API_KEY=...
./gradlew bootRun
~~~

Ollama 설정에서는 사진 분석을 지원하지 않습니다. 이미지·Structured Outputs를 지원하는 OpenAI 모델이 필요합니다. 키를 Git이나 프론트엔드에 넣지 않습니다.

## 빌드와 검증

전체 검증:

~~~bash
./gradlew test --no-daemon
./gradlew build --no-daemon
~~~

프론트엔드:

~~~bash
cd frontend
npm test
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

일반 `build`의 `check`에는 Checkstyle AST의 var 금지 검사, ArchUnit/Modulith·동작 테스트와 프론트엔드 Node 테스트가 포함됩니다. CI는 lint도 실행합니다. 테스트의 HTTP 대역은 실제 모델의 사진 인식·열량 정확도를 검증하지 않습니다.

Gradle 애플리케이션 빌드는 Next.js 정적 export를 포함하며 최종 Spring Boot JAR에서 같은 origin으로 제공합니다.

V11 migration은 음식 카탈로그와 기존 식단 항목 테이블을 제거하고 직접 입력 구조로 교체합니다. 현재 사용 데이터가 없다는 전제로 작성했습니다. 이전 이미지로만 되돌리는 복구는 보장되지 않으므로 [운영 절차](docs/infra/OPERATIONS.md#음식-사진과-직접-식단-기록-배포)를 확인합니다.

## 문서 읽는 순서

문서 전체 안내는 [docs/README.md](docs/README.md)를 기준으로 합니다.

1. [Architecture](docs/architecture/README.md) - 시스템과 모듈 구조
2. [Infrastructure](docs/infra/README.md) - AWS 운영 구성
3. [Operations](docs/infra/OPERATIONS.md) - 배포, 접속, 로그, 장애 확인
4. [Product Spec](docs/spec/PRODUCT_SPEC.md) - 제품 기능 범위
5. [Testing](docs/testing/README.md) - 테스트 기준
6. [Implementation Specs](docs/spec/README.md) - 구현 시점별 설계 기록

구현 시점의 PR 스펙은 이력 문서입니다. 현재 구조가 궁금할 때는 Architecture와 Infrastructure 문서를 우선합니다.
