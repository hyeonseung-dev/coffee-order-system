# AI-assisted Development Workflow

## 목적

본 프로젝트는 AI를 단순 코드 생성 도구로 사용하지 않고 Issue 기반 개발 프로세스 안에서 설계 검토, 구현 보조, 리뷰, 문서화에 활용한다.

최종 설계 결정, 코드 반영 여부, 테스트 결과 확인, Merge 여부는 Human이 판단한다.

## Workflow

1. Human이 기능 요구사항을 정의한다.
2. ChatGPT가 설계 누락, 테스트 기준, 작업 범위를 검토한다.
3. Human이 GitHub Issue를 생성하거나 승인한다.
4. Codex는 구현 전 변경 계획을 보고한다.
5. Human 승인 후 Codex가 구현한다.
6. Codex는 테스트, 빌드, git diff 결과를 보고한다.
7. PR 생성 후 ChatGPT, Codex, Codex Review로 3중 리뷰한다.
8. AI 리뷰 결과를 기록한다.
9. Human이 반영할 항목과 보류할 항목을 판단한다.
10. 리뷰 반영 후 Human이 최종 점검하고 Merge한다.

## 역할 분리

- Human: 최종 의사결정, Merge 판단, Issue 범위 통제
- ChatGPT: 설계 검토, 작업 범위 분리, PR 리뷰
- Codex: Issue 범위 안의 구현과 자체 검증
- GitHub: Issue, Branch, Commit, PR 이력 관리

## PR 3중 리뷰

- ChatGPT 리뷰: 설계 의도와 코드 일치 여부, 테스트 관점, 문서 불일치 확인
- Codex 문맥 리뷰: Issue 요구사항 충족 여부와 기존 코드 흐름 충돌 확인
- Codex Review 독립 리뷰: 잠재 버그, 과도한 변경, 보안/성능 리스크 확인

## 세부 규칙 위임

Codex가 반드시 지켜야 할 작업 전 보고, 수정 금지 파일, 작업 중단 조건, 구현 후 보고 규칙은 [06_CODEX_RULES.md](06_CODEX_RULES.md)를 기준으로 한다.

## 기록 원칙

AI 리뷰 결과는 PR 또는 [10_AI_REVIEW_LOG.md](10_AI_REVIEW_LOG.md)에 기록한다.
