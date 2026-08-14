# Slack 배포 환경 E2E 체크리스트

이 문서는 Issue #25의 개발용 Slack Workspace 검증 절차다. 실제 URL, 계정 이메일, 토큰은 문서와 Git에 기록하지 않고 테스트 담당자가 별도로 관리한다.

## 사전 조건

- [ ] Render backend URL과 Cloudflare Pages frontend URL을 준비했다.
- [ ] ADMIN 1명, STUDENT 1명, PROFESSOR 1명의 별도 Slack identity를 준비했다.
- [ ] `SLACK_ALLOWED_TEAM_ID`, OAuth redirect URI, Slack Bot token을 배포 secret에 입력했다.
- [ ] ADMIN bootstrap과 Staff allowlist에 교수 이메일을 등록했다.
- [ ] 테스트용 campus/class와 교수 assignment, CLASS/CAMPUS/GLOBAL channel mapping을 등록했다.

## 흐름

1. ADMIN Slack 로그인 후 ADMIN console과 `/api/auth/me` role을 확인한다.
2. 신규 STUDENT Slack 로그인 후 campus/class onboarding을 완료한다.
3. 같은 STUDENT로 다시 로그인해 중복 사용자·enrollment가 생성되지 않는지 확인한다.
4. Staff allowlist에 등록한 PROFESSOR로 로그인해 담당 질문 dashboard에 접근한다.
5. 학생이 질문을 등록하고 담당 교수 DM 도착을 확인한다.
6. 교수가 답변을 등록하고 학생 DM 도착을 확인한다.
7. 다음 broadcast를 각각 실행하고 지정된 채널만 수신하는지 확인한다.

| 공개 범위 | 확인할 대상 |
| --- | --- |
| PRIVATE | channel broadcast 없음 |
| CLASS | 질문 class channel 하나 |
| CAMPUS | 질문 campus의 매핑 channel |
| GLOBAL | 구성된 전체 announcement channel |

## 음성 시나리오

- [ ] 다른 Slack Workspace identity의 로그인은 거부된다.
- [ ] allowlist에 없는 identity는 PROFESSOR/ADMIN으로 승격되지 않는다.
- [ ] 학생이 다른 campus/class 조합을 직접 API로 보내도 거부된다.
- [ ] 학생이 ADMIN API와 다른 class의 질문 URL에 접근해도 거부된다.
- [ ] Render cold start 직후 OAuth callback, `/api/auth/me`, 질문 등록, Slack 알림이 정상 동작한다.

## 결과 기록

| 실행일 | 환경 URL | 결과 | 실패 Issue/비고 |
| --- | --- | --- | --- |
| YYYY-MM-DD | 별도 비공개 기록 | 미실행 | - |

실제 Slack DM/Broadcast 결과와 개인정보가 포함된 화면 캡처는 저장소에 올리지 않는다. 실패 항목은 재현 절차와 함께 별도 GitHub Issue로 등록한다.
