# SKALA Q&A

SKALA 학생과 교수자의 질문·답변을 관리하는 모듈러 모놀리스 MVP입니다.

## 구성

- `backend/`: Java 21, Spring Boot, Gradle
- `frontend/`: React, TypeScript, Vite
- PostgreSQL 18 (`compose.yaml`)

## 사전 준비

- Java 21
- Node.js 20.19 이상
- Docker와 Docker Compose

## 로컬 실행

1. 환경변수 예시를 복사합니다.

   ```bash
   cp .env.example .env
   cp frontend/.env.example frontend/.env
   ```

2. PostgreSQL을 실행합니다.

   ```bash
   docker compose up -d postgres
   ```

3. 백엔드를 실행합니다.

   ```bash
   cd backend
   set -a
   source ../.env
   set +a
   ./gradlew bootRun
   ```

4. 다른 터미널에서 프론트엔드를 실행합니다.

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. 브라우저에서 `http://localhost:5173`을 열고 백엔드 연결 상태가 `정상`인지 확인합니다. 백엔드 상태는 `http://localhost:8080/actuator/health`에서도 확인할 수 있습니다.

## 인증

`POST /api/auth/login`에 이메일과 비밀번호를 보내면 Bearer JWT를 발급합니다. `/api/auth/login`과 `/actuator/health`를 제외한 API는 `Authorization: Bearer <token>` 헤더가 필요합니다. 사용자 생성 시 비밀번호는 8~128자여야 하며, JWT 서명키는 `.env`의 `JWT_SECRET`에 32바이트 이상으로 설정합니다.

Slack 로그인을 사용하려면 Slack App의 OAuth Redirect URL을 `SLACK_REDIRECT_URI`와 동일하게 등록하고 다음 환경변수를 설정합니다. Slack OIDC의 `openid`, `email`, `profile` scope를 사용하며, `SLACK_ALLOWED_TEAM_ID`를 지정하면 해당 Workspace만 허용합니다.

```dotenv
SLACK_CLIENT_ID=...
SLACK_CLIENT_SECRET=...
SLACK_REDIRECT_URI=http://localhost:8080/login/oauth2/code/slack
SLACK_ALLOWED_TEAM_ID=T0123456789
```

프론트엔드의 `Slack으로 로그인` 버튼은 백엔드 `/oauth2/authorization/slack`에서 시작하고, 검증된 Slack identity를 기존 내부 JWT로 교환한 뒤 프론트엔드로 돌아옵니다. 이메일·비밀번호 로그인은 개발·관리자 fallback으로 유지합니다.

최초 ADMIN은 `ADMIN_BOOTSTRAP_EMAIL`과 `ADMIN_BOOTSTRAP_PASSWORD`를 배포 환경 secret으로 설정해 애플리케이션 시작 시 한 번 생성·승격합니다. 두 값이 비어 있으면 bootstrap은 비활성화되며, 값은 저장소에 기록하지 않습니다. 이후 ADMIN은 `/api/admin/staff-access`에서 교수·운영진 Slack 이메일 허용 목록을 관리합니다.

## 검증

```bash
cd backend && ./gradlew test
cd frontend && npm run lint && npm run build
docker compose config --quiet
```

## 환경변수와 비밀정보

`.env`와 로컬 Spring 설정 파일은 Git에서 제외됩니다. 실제 비밀번호, API 키, JWT 비밀키, Slack 토큰은 예제 파일이나 소스 코드에 작성하지 않고 환경변수로만 주입합니다.

## Slack 개발 환경

1. Slack API에서 개발용 앱을 만들고 `chat:write` 권한을 추가한 뒤 개발 workspace에 설치합니다.
2. 발급된 Bot User OAuth Token과 테스트 채널 ID를 `.env`에 입력합니다.

   ```dotenv
   SLACK_BOT_TOKEN=xoxb-...
   SLACK_TEST_CHANNEL_ID=C0123456789
   ```

3. 테스트 채널에 봇을 초대하고 백엔드를 실행합니다. 관리자 JWT로 아래 요청을 보내면 테스트 메시지가 전송됩니다.

   ```bash
   curl -X POST http://localhost:8080/api/slack/test-message \
     -H 'Authorization: Bearer <admin-jwt>' \
     -H 'Content-Type: application/json' \
     -d '{"text":"SKALA Q&A Slack 연동 테스트"}'
   ```

토큰이나 채널 ID가 없으면 애플리케이션은 정상적으로 시작하고, 테스트 요청은 원인을 담은 `503 Service Unavailable`을 반환합니다. 시스템 사용자·Slack 사용자와 범위·Slack 채널 연결은 관리자 전용 `/api/slack/user-mappings`, `/api/slack/channel-mappings`에서 관리합니다.

## Free Test Deployment

실제 계정과 URL을 저장소에 넣지 않고 다음 무료 구성으로 테스트 환경을 만들 수 있습니다.

1. Supabase에서 Free project를 만들고 PostgreSQL connection string을 확인합니다. Render의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` secret으로만 입력합니다. 새 데이터베이스는 애플리케이션 시작 시 Flyway V1~V9가 순서대로 적용됩니다.
2. Render에서 이 저장소의 `main`을 연결합니다. `render.yaml` 또는 Dockerfile 설정을 사용하면 무료 Web Service와 `/actuator/health` health check가 구성됩니다. Render가 주입하는 `PORT`를 Spring Boot가 사용하며, 무료 인스턴스는 유휴 시 sleep/cold start가 발생할 수 있습니다.
3. Cloudflare Pages에서 이 저장소의 `frontend` 디렉터리를 연결하고 build command를 `npm run build`, output directory를 `dist`로 설정합니다. `VITE_API_BASE_URL`에는 Render backend HTTPS URL을 입력합니다. `frontend/public/_redirects`가 SPA 새로고침을 처리합니다.
4. Render의 `FRONTEND_ORIGIN`에는 Cloudflare Pages의 정확한 HTTPS origin만 입력합니다. wildcard CORS는 사용하지 않습니다.
5. Slack App OAuth Redirect URL과 Render의 `SLACK_REDIRECT_URI`를 `https://<render-host>/login/oauth2/code/slack`로 맞추고, `SLACK_CLIENT_ID`, `SLACK_CLIENT_SECRET`, `SLACK_ALLOWED_TEAM_ID`를 Render secret으로 입력합니다. 최초 ADMIN은 `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`를 입력합니다.

배포 후에는 `/actuator/health`가 `UP`인지 확인하고 Slack 로그인 redirect, `/api/auth/me`, 신규 Supabase persistence, 학생 onboarding, 교수·ADMIN 권한을 순서대로 smoke test합니다. 실제 Slack DM/Broadcast E2E와 운영 Workspace 적용은 각각 Issue #25와 #26의 절차를 따릅니다.
