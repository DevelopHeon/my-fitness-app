# 검증 방법과 테스트 작성 기준

비즈니스 규칙과 경계의 위반을 검증한다. 검사별 책임은 [Architecture](../reference/architecture.md#검사의-책임)에 둔다.
테스트 대역·실공급자 평가·운영 배포를 서로 다른 검증 범위로 보고한다.

## 기본 검증

저장소 루트, Java 21·Node 22 환경에서 실행한다.

```bash
./gradlew checkstyleMain checkstyleTest --no-daemon
./gradlew test --tests 'com.myfitness.architecture.*' --tests 'com.myfitness.convention.*' --no-daemon
./gradlew test --no-daemon
./gradlew build --no-daemon
```

test는 Checkstyle을 선행 실행한다. build의 check에는 frontend Node 테스트가 포함되고
BootJar에는 정적 PWA 빌드·자산 검사가 연결된다. 일반 test는 `ai-policy-eval` tag를 제외한다.
CI는 frontend lint와 CDK 검사도 별도로 실행한다.

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

CDK의 검증·배포 명령은 [Deployment](deployment.md#인프라-변경)에 둔다.
실패 원인을 수정한 뒤 재실행하며 테스트 삭제·비활성화나 허용 규칙 확대를 통과 수단으로 사용하지 않는다.

## 어떤 테스트를 작성하는가

- Domain/Service: 상태 변경·계산·소유권·null/0·허용/제한의 비즈니스 규칙.
- Repository: 복잡한 조회·기간·정렬·집계·영속성 계약.
- API: 사용자 격리·HTTP 오류·검증·호출 차단·결과 저장.
- 경계: JPA Entity를 Result로 바꾸는 흐름은 테스트 수준 transaction 없이 요청 종료 뒤 응답을 확인한다.
- AI: 정책과 Context 선택, 외부 호출 여부·횟수, failure 계약·token·transaction 비활성.
  자연어 전체 문자열 일치나 대역 결과를 모델 정확도로 표현하는 테스트를 피한다.
- Frontend: 입력·시간·전송·렌더링 계약. Node 테스트는 실제 브라우저 조작과 모델 품질을 대신하지 않는다.

단순 getter/setter, JPA 기본 save 자체, CSS 문자열을 그대로 복제하는 테스트는 필요할 때만 작성한다.
메서드명은 영문 camelCase, 검증 의도는 `@DisplayName`으로 표현한다.

## 모듈 문서 생성

```bash
./gradlew modulithDocs --no-daemon
```

결과는 `build/spring-modulith-docs/`이며 문서 생성과 일반 검사 실행을 분리한다.

## AI 정책 평가

외부 호출 없는 계약 검사는 일반 AI 테스트에 포함된다.
실제 평가 task는 mode를 명시해야 하며 live는 환경의 TYPESAFE_API_KEY가 없으면 실패한다.
재시도나 대역으로 실제 호출을 대신하지 않는다.

```bash
./gradlew aiPolicyEval -PaiPolicyEval.mode=jev-live -PaiPolicyEval.runs=1 \
  -PaiPolicyEval.dataset=src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl \
  -PaiPolicyEval.allowDraft=true --no-daemon
```

위 명령은 비용이 발생하는 별도 실공급자 평가다. 일반 빌드나 문서 정리에서 자동으로 실행하지 않는다.
합성 50 family ×20 표현은 독립적인 사용자 질문 1,000개가 아니며 draft 라벨이다.
사람 검토 데이터의 요구 조건과 평가 task 옵션을 확인한다.

저장 응답 재평가는 다음과 같이 원본 cases를 지정한다.

```bash
./gradlew aiPolicyEval -PaiPolicyEval.mode=replay \
  -PaiPolicyEval.dataset=src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl \
  -PaiPolicyEval.allowDraft=true \
  -PaiPolicyEval.replay=docs/evaluations/ai-policy/jev-live-1000-v1/cases.jsonl --no-daemon
```

산출물은 `build/reports/ai-policy/<batch-id>/`에 생성된다. 채택할 보고서와 재현 자료는 Evaluations에 보존하고
입력·정책·코드 hash, 모델·임계값, live/replay 구분과 unknown 사용량을 유지한다.
replay에는 원격 지연·과금 결과를 보고하지 않는다. [기존 평가](../README.md#정량-평가)는 원본을 덮어쓰지 않는다.

배포 parameter의 로컬 stub 검사는 `python3 scripts/test-ai-policy-deploy.py`다.
모니터링 설정·규칙·연결 검사는 [Monitoring](monitoring.md#검증)을 따른다.
H2 검사는 PostgreSQL/Flyway 운영 검증을 대신하지 않는다.

## 수동 확인과 결과 기록

모바일 입력·날짜 선택·달력·PWA 설치·사진 선택·분석 대기와 실제 공급자 동작을 확인할 때
[수동 테스트 템플릿](../templates/manual-test.md)을 사용한다. 작은 수정마다 새 수동 문서를 만들 필요는 없다.

반복 실행할 절차는 이 가이드 또는 해당 Guide에, 특정 시점의 실행 결과는 Changes의 validation.md에 기록한다.
정량 모델 실험은 Evaluations에 둔다. 날짜·대상 commit·실행 환경·명령·결과·미검증 범위를 남긴다.
실행하지 않은 검사와 실제 운영 확인을 완료로 표현하지 않는다.
