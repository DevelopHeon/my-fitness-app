# Frontend

Next.js·TypeScript·Tailwind CSS 기반 PWA다. 운영에서는 정적 export를 Spring Boot JAR에 포함하므로
별도 Next.js 서버를 실행하지 않는다. 전체 관계는 [Architecture](../docs/architecture.md#c2-container)를 따른다.

## 실행

Node.js 22를 사용한다. 아래 명령은 저장소 루트에서 시작한다.

```bash
cd frontend
npm ci
npm run dev
```

개발 화면은 `http://localhost:3000`, 기본 API는 `http://localhost:8080`이다.
서버·DB·Google 로그인 준비는 [Backend](../docs/backend/README.md)를 따른다.
다른 API 주소를 사용할 때만 공개 `NEXT_PUBLIC_API_BASE_URL`을 지정한다. 서버 키를 NEXT_PUBLIC 변수에 넣지 않는다.

## 소스와 사용자 규칙

- `src/app`: 페이지·라우팅과 화면 흐름.
- `src/components`: 재사용 화면 구성과 입력 기능.
- `src/lib`: API·공통 클라이언트 기능.
- `public`: PWA·정적 자산.
- `tests`: Node 동작 검사, `scripts/verify-pwa.mjs`: export 자산 확인.

날짜·식사 구분·null/0·사진 분석 초안·일괄 저장은 [Product](../docs/product.md)를 따른다.
음식 사진 선택은 식단 메뉴에서 제공한다. AI 결과는 입력 초안이며 사용자의 저장 전 식단을 등록하지 않는다.

## 검증과 빌드

아래 명령은 `frontend/`에서 실행한다.

```bash
npm test
npm run lint
npm run build
```

build는 정적 export와 PWA 자산 검사를 수행한다. 결과 `out/`은 생성물이며 Git 문서나 소스로 복사하지 않는다.
전체 앱 검증·JAR 생성은 저장소 루트의 `./gradlew build --no-daemon`을 사용한다.
자동 검사와 실제 모바일·PWA 확인의 차이는 [Testing](../docs/testing.md)을 따른다.
