# My Fitness Frontend

Next.js 16.3.3·React 19·TypeScript·Tailwind CSS로 만든 PWA입니다. 개발 시 Next 개발 서버를 사용하고, 배포 시 정적 export를 Spring Boot JAR의 `static`에 포함해 같은 origin에서 제공합니다.

## 개발과 검증

Node 22와 Java 21을 기준으로 CI를 실행합니다.

```bash
npm ci
npm run dev
npm test
npm run lint
npm run build
```

`npm test`는 기존 Node test runner와 설치된 TypeScript로 입력·시간·전송 계약과 결과 화면의 렌더링을 검사합니다. 실제 브라우저 클릭이나 모델 품질 평가를 대신하지 않습니다. `postbuild`는 정적 export의 PWA 자산을 검사합니다. 저장소 루트의 `./gradlew build`에는 프론트엔드 테스트와 빌드도 포함됩니다.

개발 주소는 `http://localhost:3000`, 기본 API는 `http://localhost:8080`입니다. 별도 API 주소가 필요하면 `NEXT_PUBLIC_API_BASE_URL`을 설정합니다. 이 공개 설정에 서버 API 키를 넣지 않습니다. Google 로그인·서버 세션·CSRF는 백엔드 계약을 따릅니다.

## 식단과 사진 흐름

- 식단은 음식명·최종 섭취 칼로리를 직접 입력합니다. 음식 카탈로그와 회분 곱셈은 없습니다.
- 입력 폼을 열 때 한국 시간으로 날짜·식사 구분을 채웁니다. 사용자가 수정하며 편집 중 기본값으로 덮어쓰지 않습니다.
- 탄단지는 `<details>`의 기본 닫힘 영역에서 선택 입력합니다. 빈 값과 0을 구분하며 미입력 영양소의 합계·잔여값을 확정 숫자로 표시하지 않습니다.
- 일별·캘린더 조회와 등록·수정 화면을 분리합니다. 월간 날짜 셀에는 총 kcal와 등록된 식사 구분을 표시하고, 날짜를 선택하면 일별 조회로 이동합니다. 기록 없음과 기록된 0 kcal를 구분합니다.
- 식단 메뉴의 **사진으로 식단 입력**에서 JPEG/PNG 사진을 선택하면 등록 화면에서 자동 분석합니다. 업로드는 식단 화면에서만 제공하고 AI Coach의 업로드 영역은 제거했습니다. 원본 5 MiB·1600만 픽셀 제한을 확인하고 긴 변 최대 1600px의 JPEG로 변환합니다.
- MealPhotoInput은 파일 처리·기존 AI 사진 API 호출·분석 상태를 맡으며 비음식/식별 불가/오류에는 기존 입력값을 바꾸지 않습니다. 음식 결과는 MealRecordForm의 여러 행에 채우고 수정·제외할 수 있습니다. 기존 AI 대화의 사진 결과 버튼도 전체 음식 draft를 전달합니다.
- 사진 선택은 버튼으로 제공하며 분석 중에는 회전 아이콘과 “AI가 분석 중이에요” 모달을 표시합니다. 결과나 오류가 도착하면 모달을 닫습니다. 좁은 화면에서는 날짜·식사 구분과 저장 영역을 세로로 배치하고, 조회·달력의 내용이 화면 너비 안에 들어오도록 합니다.
- AI 채팅과 사진 분석은 같은 회전 링을 사용합니다. 응답을 기다리는 동안 0.9초 주기로 계속 회전하며, 기기의 ‘움직임 줄이기’ 설정에서는 3초 주기로 속도를 낮춥니다.
- 신규 입력은 현재 한국 시간의 날짜·식사 구분을, 수정은 기존 값을 채웁니다. 탄단지는 음식별로 접힌 선택 입력입니다. 저장 전에는 식단 API를 호출하지 않으며 최대 20개를 한 번에 저장합니다. 저장 후 기록 날짜의 일별 조회로, 취소 후에는 이전 조회 날짜·달로 돌아갑니다.
- 사진은 OpenAI에 전송하며 앱에 원본을 보관하지 않습니다. 화면을 벗어난 뒤 도착한 분석 결과는 입력에 반영하지 않습니다. AI Coach를 다시 열면 식단에서 저장된 사진 분석 대화를 다시 조회합니다.

## API와 배포

JSON 요청은 기존 `/api` endpoint를 사용합니다. 사진은 `/api/ai/conversations/{id}/food-photos`에 `image` part를 FormData로 전송하며 Content-Type boundary를 직접 설정하지 않습니다. 인증 쿠키와 CSRF header는 공통 API helper가 유지합니다. 사진 전송에는 keepalive를 사용하지 않습니다.

식단의 월간 요약은 `GET /api/meals/calendar?month=yyyy-MM`, 일괄 등록은 `POST /api/meals/items/batch`를 사용합니다. 일괄 등록의 날짜·식사 구분은 공통이며 음식은 1~20개입니다. 서버 transaction에서 모두 저장하거나 모두 취소합니다.

일반 대화의 JEV 정책 판정과 사진 분석은 서버 책임입니다. 모델 키·정책·이미지 판별 로직을 프론트엔드에 두지 않습니다. 사진 분석의 열량은 일반적인 1인분 추정값이며 사용자가 최종 기록값을 수정할 수 있습니다.

`next.config.ts`의 정적 export 결과는 `out/`에 생성됩니다. `./gradlew build`가 export를 `BOOT-INF/classes/static`으로 묶습니다. 앱 release는 저장소의 GitHub Actions·ECR·SSM 배포 흐름을 따릅니다.
