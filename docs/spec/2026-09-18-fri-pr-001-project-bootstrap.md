# PR-001 프로젝트 기반 구성

- 날짜: 2026-09-18
- 요일: 금요일
- 상태: 진행 (백엔드 기반 구성 완료)

## 1. 배경 및 목적

My Fitness MVP를 구현할 수 있도록 Spring Boot + Next.js + PWA 기반의 단일 저장소/단일 배포 구조를 만든다.

## 2. 구현 범위

- Spring Boot 백엔드 프로젝트 생성
- PostgreSQL/JPA/Security/Validation 기본 의존성 구성
- Next.js + TypeScript 프론트엔드 생성
- 정적 export 기반 PWA 기본 구성
- 프론트 빌드 결과를 Spring Boot 정적 리소스에 포함할 수 있는 빌드 흐름 구성
- 로컬 개발용 PostgreSQL Docker Compose 구성
- 환경별 설정 기본값 및 예제 환경 변수 작성
- docs/spec, docs/architecture, docs/testing 문서 체계 구축

## 3. 비기능 요구사항

- 대규모 트래픽을 고려한 분산 인프라는 도입하지 않는다.
- 백엔드와 프론트엔드를 하나의 저장소에서 관리한다.
- 최종 배포 단위는 Spring Boot 애플리케이션 하나를 우선한다.
- Ollama는 추후 별도 로컬 프로세스로 연동한다.

## 4. 완료 조건

- 백엔드 테스트가 실행된다.
- 프론트엔드 빌드가 성공한다.
- 루트 빌드에서 프론트 정적 결과물을 포함한 Spring Boot 패키징 경로가 존재한다.
- 기본 README만 보고 로컬 실행 방법을 이해할 수 있다.

## 5. 제외 범위

- Workout 실제 비즈니스 로직
- Ollama 연동
- 사용자 회원가입/로그인 완성
- 배포 자동화

## 6. 구현 결과 및 후속 과제

### 완료
- Spring Boot 4.1.1 / Java 21 타깃 기반 프로젝트 생성
- PostgreSQL/JPA/Security/Validation/Actuator 구성
- 테스트용 H2 프로필 구성
- PostgreSQL Docker Compose 구성
- 도메인 기준 최상위 패키지 골격 생성
- `./gradlew test` 통과 확인

### 후속
- Next.js PWA 골격 생성
- 프론트 정적 빌드와 Spring Boot 패키징 연결
- 루트 README 개발 실행 절차 정리
