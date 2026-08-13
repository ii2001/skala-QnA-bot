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

## 검증

```bash
cd backend && ./gradlew test
cd frontend && npm run lint && npm run build
docker compose config --quiet
```

## 환경변수와 비밀정보

`.env`와 로컬 Spring 설정 파일은 Git에서 제외됩니다. 실제 비밀번호, API 키, JWT 비밀키, Slack 토큰은 예제 파일이나 소스 코드에 작성하지 않고 환경변수로만 주입합니다.
