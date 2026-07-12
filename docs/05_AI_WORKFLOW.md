# AI-assisted Development Workflow

## 목적

본 프로젝트는 AI를 단순 코드 생성 도구로 사용하지 않고 Issue 기반 개발 프로세스 안에서 설계 검토, 구현 보조, 리뷰, 문서화에 활용한다.

최종 설계 결정, 코드 반영 여부, 테스트 결과 확인, Merge 여부는 Human이 판단한다.

## Workflow

1. Human이 GitHub Issue와 작업 범위를 승인한다.
2. Codex가 Issue와 관련 문서를 확인하고 Preflight를 수행한다.
3. Preflight를 통과하면 Planner, Implementer, Verify, Reviewer 순으로 실행한다.
4. Reviewer가 Attempt 1에서 수정 가능한 FAIL을 반환한 경우에만 Attempt 2를 한 번 수행한다.
5. Codex가 Attempt Log와 Verification Log를 기록하고 Enforce를 실행해 PASS 또는 BLOCKED로 종료한다.
6. 요구사항 충돌, 보호 파일 변경, 설계 재결정, 권한·환경 문제는 BLOCKED로 Human에게 반환한다.
7. Commit, Push, PR 생성은 별도 Human 승인 후에만 수행하며, Merge는 Human만 수행한다.

## 역할 분리

- Human: Issue와 작업 범위 승인, 최종 의사결정, Merge 판단
- ChatGPT: 설계 검토, 작업 범위 분리, PR 리뷰
- Codex: 승인된 하네스 실행 안에서 계획, 구현, 검증, 독립 리뷰와 기록 수행
- GitHub: Issue, Branch, Commit, PR 이력 관리

## PR 3중 리뷰

- ChatGPT 리뷰: 설계 의도와 코드 일치 여부, 테스트 관점, 문서 불일치 확인
- Codex 문맥 리뷰: Issue 요구사항 충족 여부와 기존 코드 흐름 충돌 확인
- Codex Review 독립 리뷰: 잠재 버그, 과도한 변경, 보안/성능 리스크 확인

## 세부 규칙 위임

Codex가 반드시 지켜야 할 작업 전 보고, 수정 금지 파일, 작업 중단 조건, 구현 후 보고 규칙은 [06_CODEX_RULES.md](06_CODEX_RULES.md)를 기준으로 한다.

## 기록 원칙

AI 리뷰 결과는 PR 또는 [10_AI_REVIEW_LOG.md](10_AI_REVIEW_LOG.md)에 기록한다.
