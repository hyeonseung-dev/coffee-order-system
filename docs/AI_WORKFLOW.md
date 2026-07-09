# AI-assisted Development Workflow

## 목적

본 프로젝트는 AI를 단순 코드 생성 도구로 사용하지 않고,
Issue 기반 개발 프로세스 안에서 설계 검토, 구현 보조, 리뷰, 문서화에 활용한다.

최종 설계 결정, 코드 반영 여부, 테스트 결과 확인, Merge 여부는 개발자가 직접 판단한다.

## Workflow

1. Human이 기능 요구사항을 정의한다.
2. Human이 설계 초안을 작성한다.
3. ChatGPT가 설계 누락, 테스트 기준, 작업 범위를 검토한다.
4. ChatGPT가 Codex에게 전달할 구현 프롬프트를 작성한다.
5. Human이 구현 프롬프트와 Issue 범위를 검토한다.
6. GitHub Issue를 생성한다.
7. Codex는 구현 전 현재 브랜치, git status, 수정 예정 파일, 테스트 계획을 보고한다.
8. Human 승인 후 Codex가 구현한다.
9. Codex는 테스트, 빌드, git diff 결과를 보고한다.
10. PR 생성 후 ChatGPT, Codex, Codex Review로 3중 리뷰한다.
11. AI 리뷰 결과를 기록한다.
12. Human이 반영할 항목과 보류할 항목을 판단한다.
13. 리뷰 반영 후 Human이 최종 점검하고 Merge한다.

## AI 변경 리스크 통제

- Issue 범위 밖 기능 추가 금지
- 새 브랜치 임의 생성 금지
- 수정 가능 파일 / 수정 금지 파일 명시
- 구현 전 변경 계획 제출
- Human 승인 전 파일 수정 금지
- 구현 후 git status / git diff 확인
- PR 단계에서 작업 범위 이탈 여부 검토

## PR 3중 리뷰

### ChatGPT 리뷰

- 설계 의도와 코드 일치 여부
- README/docs와 코드 불일치 여부
- 트랜잭션 경계
- 예외 처리
- 테스트 누락
- 유지보수성

### Codex 문맥 리뷰

- Issue 요구사항 충족 여부
- 구현 의도와 코드 일치 여부
- 기존 코드 흐름과 충돌 여부

### Codex Review 독립 리뷰

- 잠재 버그
- 과도한 변경
- 테스트 부족
- 구조적 문제
- 보안/성능 리스크
- Issue 범위 이탈 여부

## 기록 원칙

AI 리뷰 결과는 PR 또는 `docs/AI_REVIEW_LOG.md`에 기록한다.

각 리뷰 항목은 다음 기준으로 관리한다.

- 리뷰어
- 리뷰 내용
- 반영 여부
- 미반영 사유
- 재검증 결과
