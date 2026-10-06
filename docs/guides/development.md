# 로컬 개발·실행·빌드

저장소 루트에서 실행한다. Java 21, Node.js 22, Docker Compose가 필요하다.
`java -version`으로 실행 JDK를 확인하고 다른 버전이면 JAVA_HOME을 Java 21로 지정한다.
설정의 의미와 운영 parameter는 [Infrastructure](../reference/infrastructure.md#설정과-비밀값)를 따른다.

## 개발 서버

```bash
docker compose up -d postgres
./gradlew bootRun
```

다른 터미널에서 프론트엔드를 실행한다.

```bash
cd frontend
npm ci
npm run dev
```

화면은 `http://localhost:3000`, API는 `http://localhost:8080`이다.
PostgreSQL 5432나 `my-fitness-postgres` 이름이 이미 사용 중이면 기존 컨테이너·데이터를 지우지 않는다.
개발 DB를 확인하고 `DB_URL`을 지정한다. 포트만 변경할 때는 Compose의 `POSTGRES_PORT`와 DB_URL을 함께 맞춘다.
Docker `down`은 볼륨을 보존하며 `down -v`는 DB를 삭제하므로 의도한 초기화에서만 사용한다.

## AI와 로그인 설정

Google 로그인에는 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`과 로컬 callback 등록이 필요하다.
AI는 프로세스 환경으로 설정하며 `.env`를 Spring Boot가 자동으로 읽지 않는다.
키 값은 문서·Git·frontend·터미널 기록에 복사하지 않는다.

| 용도 | 프로세스에 필요한 설정 |
| --- | --- |
| OpenAI 텍스트·사진 | AI_PROVIDER=openai, OPENAI_API_KEY |
| 일반 텍스트 정책 | TYPESAFE_API_KEY 필수 |
| 로컬 Ollama 텍스트 | AI_PROVIDER=ollama, OLLAMA_BASE_URL, TYPESAFE_API_KEY |
| 별도 개발 API 주소 | 공개 NEXT_PUBLIC_API_BASE_URL, 서버 키 금지 |

Provider 미설정 시 AI는 503을 반환하지만 직접 기록 기능은 사용할 수 있다.
Ollama에서는 사진 분석을 지원하지 않는다. 사진은 이미지 입력·Structured Outputs를 지원하는 OpenAI 모델을 사용한다.
모델·timeout 기본값은 [설정 계약](../reference/infrastructure.md#앱-환경-기본값), 실패 계약은 [AI Reference](../reference/ai.md)를 따른다.

## 정적 PWA 포함 빌드

```bash
./gradlew build --no-daemon
```

Gradle은 frontend 설치·Node 테스트·정적 export·PWA 자산 검사를 연결하고
`build/libs/app.jar`에 `frontend/out`을 묶는다. 배포용 이미지에는 별도 Next.js 서버가 없다.
로컬에서 완성 JAR을 확인할 때는 로그인 성공 URL과 frontend origin을 8080으로 맞춘다.

```bash
LOGIN_SUCCESS_URL=http://localhost:8080 FRONTEND_ORIGIN=http://localhost:8080 java -jar build/libs/app.jar
```

이 실행에도 DB·OAuth·사용할 AI 설정이 필요하다. 현재 수정 전후 검사는 [Testing](testing.md),
로컬 수집 학습은 [Monitoring](monitoring.md)을 따른다.
