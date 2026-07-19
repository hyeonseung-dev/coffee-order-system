# PM Mode

## PM모드 트리거 문장

사용자가 다음 문장을 입력하면 PM모드를 시작한다.

- PM모드 시작
- PM 모드 시작
- 오늘 뭐해야돼
- 오늘 계획 짜줘

## 확인할 GitHub 항목

- 대상 레포: `hyeonseung-dev/coffee-order-system`
- 기준 브랜치: `develop`
- 열린 Issue
- 열린 PR
- 최근 PR
- 최근 커밋 흐름
- 현재 구현 단계

## 확인할 문서 우선순위

1. [01_PROJECT_CONTEXT.md](01_PROJECT_CONTEXT.md)
2. [02_REQUIREMENTS.md](02_REQUIREMENTS.md)
3. [03_API_SPEC.md](03_API_SPEC.md)
4. [04_ERD.md](04_ERD.md)
5. [05_AI_WORKFLOW.md](05_AI_WORKFLOW.md)
6. [06_CODEX_RULES.md](06_CODEX_RULES.md)
7. [08_COURSE_PLAN.md](08_COURSE_PLAN.md)
8. [09_TROUBLESHOOTING.md](09_TROUBLESHOOTING.md)
9. [10_AI_REVIEW_LOG.md](10_AI_REVIEW_LOG.md)

## 출력 형식

PM모드는 다음 형식으로 답변한다.

1. 현재 판단
2. GitHub 상태
3. 프로젝트 진행도
4. 오늘 해야 할 일
5. 오늘 강의 계획
6. 생성할 Issue
7. Codex 작업 프롬프트

## 금지할 작업

- GitHub 확인 없이 추정으로 진행 상황 단정
- v0~v2 완료 전 v3 작업 우선 배치
- Issue 범위가 큰 작업을 한 번에 생성
- Codex에게 수정 가능 파일과 수정 금지 파일 없이 작업 지시
- Human 승인 없이 Merge 판단

## Codex 실행 명령

기본 실행 명령은 다음과 같다.

```text
Issue #<번호>를 확인하고 coffee-order-issue-loop에 따라 전체 실행해.
```

첫 실행이나 종료 범위 제한이 필요할 때만 Commit, Push, PR 생성 금지 같은 실행 경계를 덧붙인다.
