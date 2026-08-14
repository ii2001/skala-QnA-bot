# 실제 SKALA Slack Workspace 도입 체크리스트

이 문서는 운영진 승인 전 검토용이다. 실제 Team ID, URL, token, client secret은 문서와 Git에 기록하지 않는다.

## 사용자 로그인 권한

Sign in with Slack OIDC에는 다음 user scope만 요청한다.

| Scope | 목적 |
| --- | --- |
| `openid` | OIDC subject와 ID token 기반 로그인 |
| `email` | 내부 User와 Slack identity 연결 |
| `profile` | 표시 이름과 team/user profile 수집 |

로그인 flow는 [Slack Sign in with Slack](https://docs.slack.dev/authentication/sign-in-with-slack/)의 OIDC authorize/token endpoint를 사용한다. OIDC scope와 Bot scope는 같은 authorization 요청에 섞지 않는다.

## Bot 권한

현재 구현에 필요한 최소 Bot scope는 다음과 같다.

| Scope | 사용처 |
| --- | --- |
| `chat:write` | DM과 broadcast 메시지 전송 |
| `im:write` | 담당 교수·질문자 1:1 DM 열기 |

Bot이 broadcast 대상 public/private channel에 초대되어 있어야 한다. 모든 public channel에 자동 접근하는 `chat:write.public`은 요청하지 않는다. 각 Web API의 최신 scope 요구사항은 [conversations.open](https://api.slack.com/methods/conversations.open)과 [chat.postMessage](https://api.slack.com/methods/chat.postMessage)에서 재확인한다.

## 운영진 승인 요청에 포함할 내용

- 서비스 목적: SKALA 학생 질문을 웹에 저장하고 교수 답변과 Slack 알림을 연결한다.
- System of Record: 질문·답변은 웹 애플리케이션과 PostgreSQL에 저장하고 Slack은 로그인 identity·DM·broadcast에 사용한다.
- 요청할 Team ID와 production OAuth redirect URI.
- 필요한 OIDC/Bot scope와 각 scope의 목적.
- Bot을 초대할 broadcast 채널 목록.
- 개인정보: broadcast에는 질문 내용과 답변만 포함하고 학생 이름·이메일은 포함하지 않는다.
- 비활성화/rollback 담당자와 연락 방법.

## ADMIN 초기 설정

- [ ] `FRONTEND_ORIGIN`, `SLACK_ALLOWED_TEAM_ID`, `SLACK_REDIRECT_URI`를 production URL로 설정한다.
- [ ] 개발용과 production용 Slack Client ID/Secret·Bot token을 분리한다.
- [ ] `ADMIN_BOOTSTRAP_EMAIL`·`ADMIN_BOOTSTRAP_PASSWORD`를 secret으로 한 번 설정한 뒤 더 이상 공유하지 않는다.
- [ ] campus와 classroom을 등록한다.
- [ ] PROFESSOR/ADMIN Staff allowlist를 등록·확인한다.
- [ ] 교수별 담당 classroom을 배정한다.
- [ ] CLASS/CAMPUS/GLOBAL channel mapping을 등록하고 Bot을 해당 채널에 초대한다.
- [ ] [#25 E2E 체크리스트](slack-e2e-checklist.md)를 완료하고 결과를 첨부한다.

## 적용·rollback

1. 운영진 승인 후 Slack App에 redirect URI와 scope를 설정하고 production workspace에 설치한다.
2. 배포 secret을 입력하고 `/actuator/health`와 Slack 로그인 smoke test를 실행한다.
3. 문제가 발생하면 `SLACK_BOT_TOKEN`과 OIDC client secret을 폐기·교체하고 Render service를 중지하거나 `FRONTEND_ORIGIN`을 점검용 origin으로 되돌린다.
4. 데이터 보존이 필요한 경우 DB는 삭제하지 않고 Slack App만 비활성화한다. 재개 시 token·mapping·redirect URI를 다시 검증한다.

실제 SKALA Workspace 설치는 운영진 승인 없이는 수행하지 않는다.
