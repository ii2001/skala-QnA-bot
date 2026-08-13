# Contributing

## Branch Strategy

- `main`: 안정 및 배포 버전
- `dev`: 통합 개발 버전
- `feat/{part}/{feature}`: 기능 개발
- `fix/{part}/{feature}`: 오류 수정
- `chore/{feature}`: 설정 및 환경 작업

예시:

- `feat/backend/project-init`
- `feat/frontend/report-editor`
- `feat/ai/report-generator`

## Workflow

1. 최신 `dev`를 가져온다.
2. `dev`에서 작업 브랜치를 생성한다.
3. 작업 완료 후 `dev` 대상 Pull Request를 생성한다.
4. CodeRabbit 리뷰와 팀원 리뷰를 확인한다.
5. 승인 후 Squash Merge한다.
6. 병합된 작업 브랜치는 삭제한다.

## Issue 자동 종료

`dev` 대상 PR 본문에 `Closes #123`, `Fixes #123`, `Resolves #123`과 같은
closing keyword를 작성하면 PR이 `dev`에 병합된 후 해당 Issue가 자동으로
완료 처리된다. 각 keyword의 표준 변형(`close/closes/closed`,
`fix/fixes/fixed`, `resolve/resolves/resolved`)과 선택적 콜론을 대소문자
구분 없이 지원한다.

PR을 병합하지 않고 닫은 경우 Issue는 종료되지 않는다.

## Issue Convention

Issue 제목은 `[담당 영역] 한글 작업명` 형식을 사용한다.

- 담당 영역: `Common`, `Backend`, `Frontend`, `Infra`, `Security`
- 여러 영역에 걸치면 `/`로 연결한다: `[Backend/Frontend]`
- 버그는 영역 뒤에 `[Bug]`를 추가한다: `[Backend][Bug]`

예시:

- `[Common] 프로젝트 초기 개발 환경 구성`
- `[Backend/Frontend] 학생 질문 등록 및 조회 구현`
- `[Backend][Bug] PostgreSQL 시간 바인딩 오류 수정`

## Commit and PR Convention

Commit과 PR 제목은 Gitmoji와 Conventional Commits를 함께 사용한다.

형식: `<Gitmoji> <type>(<scope>): <한글 요약>`

- `✨ feat`: 기능 추가
- `🐛 fix`: 버그 수정
- `📝 docs`: 문서 변경
- `♻️ refactor`: 리팩터링
- `✅ test`: 테스트 추가·수정
- `👷 ci`: CI 변경
- `🔧 chore`: 기타 설정·관리
- `💄 style`: UI 스타일 변경
- 보안 성격이 핵심이면 `🔒` 또는 `🔐`을 사용할 수 있다.

예시:

- `✨ feat(backend): 질문 등록 API 구현`
- `🐛 fix(frontend): 질문 목록 오류 처리`
- `📝 docs: Slack 개발 환경 설정 추가`
- `🔧 chore: 개발 환경 구성`

PR 본문에는 변경 내용과 검증 결과를 작성하고, Issue 작업이면 `Closes #번호`를 포함한다.

## Rules

- `main`, `dev`에 직접 Push하지 않는다.
- 하나의 브랜치에서는 하나의 작업만 수행한다.
- API Key, 비밀번호, 개인정보를 커밋하지 않는다.
- `contracts/` 변경은 AI, Backend, Frontend 담당자가 함께 확인한다.
