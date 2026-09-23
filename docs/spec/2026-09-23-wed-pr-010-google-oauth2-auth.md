# PR-010 Google OAuth2 로그인 및 사용자 인증 전환

작성일: 2026-09-23 (수)  
상태: 구현 예정  
선행 작업: Phase 1~6 완료

## 1. 배경 및 목적

현재 REST API는 `X-User-Id` 요청 헤더를 신뢰해 사용자를 구분한다. 개발용으로는 단순하지만 운영에서는 클라이언트가 다른 사용자 ID를 임의로 전달할 수 있으므로 인증 경계로 사용할 수 없다.

이번 PR에서는 Google OpenID Connect 로그인을 추가하고 모든 사용자 소유 데이터의 `userId`를 클라이언트 입력이 아니라 서버의 인증 컨텍스트에서 결정하도록 전환한다.

Google 사용자의 내부 식별은 이메일이 아니라 변경되지 않는 `sub` claim을 사용한다.

## 2. 결정 사항

- OAuth Provider는 Google만 지원한다.
- Naver/Kakao는 제외한다.
- 별도 JWT 발급 서버는 만들지 않는다.
- Spring Security 서버 세션 인증을 사용한다.
- 세션은 RDS PostgreSQL에 Spring Session JDBC로 저장한다.
- 브라우저가 전달하는 `X-User-Id`는 제거한다.
- User의 외부 식별키는 Google `sub`를 사용한다.
- 이메일은 프로필 속성으로만 저장한다.
- 인증되지 않은 API 요청은 401을 반환한다.
- 로그아웃 시 서버 세션을 무효화한다.

## 3. 목표 흐름

```text
Browser / Next.js PWA
        │
        ▼
/oauth2/authorization/google
        │
        ▼
Google OpenID Connect
        │
        ▼
/login/oauth2/code/google
        │
        ▼
GoogleOidcUserService
        ├─ google sub 조회
        ├─ User 생성/갱신
        └─ internal userId 결정
        │
        ▼
Spring Security Session
        │
        ▼
Spring Session JDBC / PostgreSQL

이후 API
Browser session cookie
        │
        ▼
SecurityContext
        │
        ▼
AuthenticatedUser.userId
        │
        ├─ Workout
        ├─ Routine
        ├─ Body
        ├─ Nutrition
        ├─ Dashboard
        └─ AI
```

## 4. 의존성

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
implementation("org.springframework.session:spring-session-jdbc")
```

기존 `spring-boot-starter-security`는 유지한다.

## 5. User 데이터 모델

Google 단일 Provider만 지원하므로 별도 OAuthAccount 테이블은 만들지 않는다.

```text
users
-----
id                  BIGINT PK
google_subject      VARCHAR(255) NOT NULL UNIQUE
email               VARCHAR(320)
display_name        VARCHAR(255)
profile_image_url   VARCHAR(1024)
created_at          TIMESTAMP NOT NULL
updated_at          TIMESTAMP NOT NULL
last_login_at       TIMESTAMP NOT NULL
```

규칙:

- `google_subject`는 Google OIDC의 `sub` claim이다.
- email 변경으로 새 사용자가 생성되어서는 안 된다.
- 동일 subject 재로그인 시 기존 User를 사용한다.
- email/name/picture는 로그인 시 최신 값으로 갱신할 수 있다.
- 내부 PK `id`만 기존 도메인의 `userId`로 사용한다.
- 다른 OAuth Provider가 실제 요구될 때만 `oauth_accounts` 분리를 검토한다.

## 6. Spring Session

애플리케이션 메모리 세션만 사용하면 EC2 컨테이너 교체 시 로그인이 풀린다.

Spring Session JDBC schema를 Flyway migration으로 관리한다.

예상 테이블:

- `SPRING_SESSION`
- `SPRING_SESSION_ATTRIBUTES`

운영에서 자동 schema 초기화에 의존하지 않는다.

## 7. User 모듈 구조

```text
user
├── presentation/controller
│   └── UserController
├── application
│   ├── port/in
│   │   └── CurrentUserQuery
│   ├── port/out
│   │   └── UserRepositoryPort
│   ├── result
│   │   └── CurrentUserResult
│   └── service
│       └── UserApplicationService
├── domain/model
│   └── User
└── infrastructure
    ├── persistence
    └── security
        ├── GoogleOidcUserService
        └── AuthenticatedUser
```

Google/Spring Security 타입은 Infrastructure 경계에 유지한다. 기능 모듈 Application에는 `OidcUser`, `Authentication` 같은 기술 타입을 전달하지 않는다.

## 8. Controller 사용자 식별

기존:

```java
@RequestHeader("X-User-Id") Long userId
```

변경:

```java
public record AuthenticatedUser(
        Long userId,
        String email,
        String displayName
) {}
```

Controller는 `@AuthenticationPrincipal AuthenticatedUser`에서 내부 userId를 얻어 기존 Use Case에 전달한다.

Principal에 JPA Entity 자체를 넣지 않는다.

## 9. API

로그인:

```http
GET /oauth2/authorization/google
```

Callback:

```http
GET /login/oauth2/code/google
```

Production callback:

```text
https://<domain>/login/oauth2/code/google
```

현재 사용자:

```http
GET /api/users/me
```

응답 예:

```json
{
  "id": 1,
  "email": "user@example.com",
  "displayName": "User",
  "profileImageUrl": "https://..."
}
```

로그아웃:

```http
POST /logout
```

## 10. Security 정책

인증 불필요:

- 정적 frontend asset
- `/manifest.webmanifest`
- `/oauth2/**`
- `/login/**`
- `/actuator/health`

인증 필요:

- `/api/**`

Session cookie:

- HttpOnly
- Secure=true in production
- SameSite=Lax
- HTTPS only

세션 쿠키 기반 인증이므로 CSRF를 단순 비활성화하지 않는다. Frontend 공통 API client에 CSRF token 전달 방식을 반영한다.

Production frontend/backend는 동일 origin으로 배포한다. 기존 CORS의 `X-User-Id` 허용은 제거한다.

## 11. 기존 데이터

운영 DB는 신규 환경으로 시작하는 것을 기본으로 한다.

기존 로컬 `user_id = 1` 데이터를 유지해야 할 때만 특정 내부 User와 연결하는 개발용 migration을 별도 검토한다. Google `sub`를 migration에 하드코딩하지 않는다.

## 12. Frontend

- Google 로그인 화면/CTA
- 앱 초기화 시 `GET /api/users/me`
- 401이면 로그인 화면
- 인증되면 기존 앱
- 사용자 이름/프로필 최소 UI
- 로그아웃 UI
- 모든 `X-User-Id: 1` 제거
- CSRF 처리

로그인 전에는 사용자 데이터 API를 요청하지 않는다.

## 13. 테스트

- 신규 Google 사용자 생성
- 동일 subject 재로그인
- email 변경 시 동일 User 유지
- 미인증 `/api/**` 401
- Principal의 userId가 Use Case로 전달
- request header로 userId 조작 불가
- logout 후 session 무효화
- 기존 API 통합 테스트를 인증 Principal 기반으로 전환
- 실제 Google 네트워크 호출은 자동 테스트에서 제외

반복 테스트 코드는 `authenticatedUser(userId)` 형태의 helper로 공통화한다.

## 14. 구현 순서

1. User Domain / Repository / Flyway
2. OAuth2 Client / Spring Session JDBC 의존성
3. GoogleOidcUserService / AuthenticatedUser
4. SecurityConfig
5. `GET /api/users/me`
6. 모든 Controller의 `X-User-Id` 제거
7. Frontend API client 인증 전환
8. 로그인/로그아웃 UI
9. 통합 테스트 인증 전환
10. JDBC session 재시작 유지 검증
11. Google local smoke test
12. production redirect URI 준비
13. 전체 test / lint / build / bootJar

## 15. 완료 조건

- Google 로그인으로 User 생성
- 재로그인 시 동일 User 사용
- email을 식별키로 사용하지 않음
- 모든 API가 Principal userId 사용
- production 코드/frontend에서 `X-User-Id` 제거
- 미인증 요청 401
- 사용자 데이터 격리 유지
- 재배포 후 JDBC session 유지
- 로그인/로그아웃 UI 정상
- 전체 검증 통과

## 16. 제외 범위

- Naver/Kakao OAuth
- 이메일/비밀번호 회원가입
- JWT access/refresh token
- Cognito
- MFA 직접 구현
- 관리자 Role
- 다중 OAuth account linking
- multi-tenant/organization

## 17. 운영 설정

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
APP_BASE_URL
SESSION_COOKIE_SECURE=true
```

Google Client Secret은 git/Docker image에 포함하지 않는다.

## 18. 참고

- https://docs.spring.io/spring-security/reference/servlet/oauth2/login/
- https://developers.google.com/identity/openid-connect/openid-connect
- https://developers.google.com/identity/openid-connect/reference
