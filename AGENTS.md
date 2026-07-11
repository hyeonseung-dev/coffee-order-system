# Codex 작업 진입점

이 파일은 실행 순서와 문서 경로만 안내한다. 상세 규칙은 연결된 문서를 따른다.

## 1. 작업 흐름

1. 대상 GitHub Issue를 확인한다.
2. 아래 컨텍스트 라우팅에서 필요한 문서만 찾아 읽는다.
3. Issue의 목적, 범위, 완료 기준을 확인한다.
4. Issue 범위 안에서 계획, 구현, 검증을 수행한다.
5. 가장 작은 관련 검증부터 실행한다.
6. 검증 결과와 미검증 항목을 기록한다.
7. 반복 실행은 `.codex/skills/coffee-order-issue-loop/SKILL.md`를 따른다.

## 2. 컨텍스트 라우팅

| 작업 종류 | 읽을 문서 |
| --- | --- |
| 프로젝트 목표와 개발 단계 | `docs/01_PROJECT_CONTEXT.md` |
| 기능 요구사항 | `docs/02_REQUIREMENTS.md` |
| API 생성 또는 변경 | `docs/03_API_SPEC.md` |
| Entity, 연관관계, DB 구조 | `docs/04_ERD.md` |
| 기존 AI 작업 흐름 | `docs/05_AI_WORKFLOW.md` |
| Codex 작업 범위와 중단 조건 | `docs/06_CODEX_RULES.md` |
| AI 자동화 방향과 위험도 기준 | `docs/11_AI_AUTOMATION_EXPERIMENT.md` |
| 테스트 범위와 검증 단계 | `docs/testing/test-strategy.md` |
| 실패와 재시도 기록 | `docs/testing/evidence/` |
| Issue 작성 | `.github/ISSUE_TEMPLATE/feature.md` |
| PR 작성 | `.github/PULL_REQUEST_TEMPLATE.md` |

> `docs/testing/`은 후속 단계에서 생성할 예정이다.

## 3. 반드시 지킬 경계

- 한 번에 하나의 Issue만 처리한다.
- Issue 범위 밖 기능을 추가하지 않는다.
- 불필요한 리팩토링을 하지 않는다.
- 요구사항이나 완료 기준이 불명확하면 임의 구현하지 않고 질문한다.
- 검증하지 않은 결과를 완료로 보고하지 않는다.
- 실행하지 못한 검증은 미검증 항목으로 명시한다.
- Merge와 운영 배포는 Human 승인 후 수행한다.
