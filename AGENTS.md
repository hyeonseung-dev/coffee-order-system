# Codex 작업 진입점

이 파일은 Codex가 코드 구현 작업을 시작할 때 확인하는 저장소 진입 규칙이다. 상세 규칙은 연결된 문서를 따른다.

## 1. 역할 경계

- Human은 기능 방향, 설계 승인, 위험 작업 승인, 최종 Merge를 판단한다.
- ChatGPT는 요구사항 정리, 설계, GitHub Issue 작성, Markdown 문서 작성·수정, PR 리뷰와 GitHub Actions 확인을 담당한다.
- Codex는 승인된 Issue를 기준으로 Java/Spring 코드와 테스트 구현, 로컬 실행·디버깅, Commit·Push·PR 생성을 담당한다.
- GitHub Issue와 PR은 ChatGPT와 Codex 사이의 기본 인계 수단이다.

Codex는 별도 요청이 없는 한 README, 설계 문서, 작업 규칙, 리뷰 로그 같은 Markdown 문서를 새로 만들거나 구조를 바꾸지 않는다. 구현 결과에 따라 문서 보완이 필요하면 PR 본문에 필요한 변경 사항만 보고한다.

## 2. 작업 흐름

1. 대상 GitHub Issue를 확인한다.
2. 아래 컨텍스트 라우팅에서 구현에 필요한 문서만 읽는다.
3. Issue의 목적, 범위, 완료 기준을 확인한다.
4. Issue 범위 안에서 코드와 테스트를 계획하고 구현한다.
5. 가장 작은 관련 검증부터 실행한 뒤 전체 테스트와 빌드를 확인한다.
6. 변경 사항을 Commit하고 작업 브랜치를 Push한다.
7. PR을 생성하고 Issue 연결, 구현 요약, 테스트·빌드 결과, 미검증 항목을 본문에 기록한다.
8. PR 번호를 Human에게 전달하고 ChatGPT 리뷰를 기다린다.
9. 반복 실행은 `.codex/skills/coffee-order-issue-loop/SKILL.md`를 따른다.

Push 또는 PR 생성 전에 로컬 오류로 중단된 경우에만 오류 메시지, 실행 명령, 현재 상태를 Human에게 전달한다. PR이 생성된 뒤에는 결과 전체를 대화에 복사하지 않고 PR 번호로 인계한다.

## 3. 컨텍스트 라우팅

| 작업 종류 | 읽을 문서 |
| --- | --- |
| 프로젝트 목표와 개발 단계 | `docs/01_PROJECT_CONTEXT.md` |
| 기능 요구사항 | `docs/02_REQUIREMENTS.md` |
| API 생성 또는 변경 | `docs/03_API_SPEC.md` |
| Entity, 연관관계, DB 구조 | `docs/04_ERD.md` |
| AI 역할 분리와 작업 인계 | `docs/05_AI_WORKFLOW.md` |
| Codex 작업 범위와 중단 조건 | `docs/06_CODEX_RULES.md` |
| AI 자동화 방향과 위험도 기준 | `docs/11_AI_AUTOMATION_EXPERIMENT.md` |
| 테스트 범위와 검증 단계 | `logs/README.md`, `logs/verification-log.md` |
| 실패와 재시도 기록 | `logs/issues/issue-<번호>/`, `logs/attempt-log-template.md` |
| Issue 작성 | `.github/ISSUE_TEMPLATE/feature.md` |
| PR 작성 | `.github/PULL_REQUEST_TEMPLATE.md` |

## 4. 반드시 지킬 경계

- 한 번에 하나의 Issue만 처리한다.
- Issue 범위 밖 기능을 추가하지 않는다.
- 불필요한 리팩터링을 하지 않는다.
- 요구사항이나 완료 기준이 불명확하면 임의 구현하지 않고 질문한다.
- 검증하지 않은 결과를 완료로 보고하지 않는다.
- 실행하지 못한 검증은 미검증 항목으로 명시한다.
- Markdown 문서 변경이 필요하면 임의 수정하지 않고 PR 본문에 ChatGPT 보완 항목으로 남긴다.
- Merge와 운영 배포는 Human 승인 후 수행한다.
