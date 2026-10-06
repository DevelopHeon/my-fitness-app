# My Fitness

운동·신체·식단 기록과 기록 기반 AI Coach를 제공하는 개인용 피트니스 앱이다.
Spring Boot와 Next.js PWA를 사용하고 정적 frontend를 같은 Spring Boot JAR에 포함한다.

- Backend: Java 21, Spring Boot, JPA, Spring Security, Spring Modulith, PostgreSQL, Flyway.
- Frontend: Next.js, TypeScript, Tailwind CSS, PWA.
- AI: 일반 대화는 JEV 정책 평가 뒤 OpenAI/Ollama 답변 생성, 음식 사진은 OpenAI 분석.
- Infrastructure: AWS CDK, EC2, RDS, ECR, SSM, GitHub Actions OIDC.

## 시작하기

[문서 인덱스](docs/README.md)에서 현재 기준과 과거 기록을 구분한다.

- [로컬 실행·빌드](docs/guides/development.md)
- [제품 기능](docs/reference/product.md) · [구조와 규칙](docs/reference/architecture.md)
- [테스트](docs/guides/testing.md) · [배포와 schema 복구](docs/guides/deployment.md)
- [접속·장애 대응](docs/guides/troubleshooting.md) · [로컬 Prometheus·Grafana](docs/guides/monitoring.md)

## 소스 위치

| 경로 | 역할 |
| --- | --- |
| src/ | Spring Boot backend |
| frontend/ | Next.js 정적 PWA |
| infra/ | AWS CDK |
| monitoring/ | 로컬 수집·대시보드 설정 |
| scripts/ | 배포·로컬 설정·검증 도구 |
| docs/ | 현재 기준·절차·변경 이력·평가 근거 |
