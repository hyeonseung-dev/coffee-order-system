# Harness Logs

## 1. 목적

`logs` 디렉터리는 `coffee-order-issue-loop` 실행 과정과 검증 근거를 저장하는 기록의 원본 위치다.

로그는 다음 목적을 가진다.

- 이전 실패 원인을 다음 시도에 전달한다.
- 동일한 실패 반복을 방지한다.
- 실제 테스트와 빌드 실행 근거를 보존한다.
- Planner, Implementer, Reviewer 결과를 추적한다.
- Human 개입 시점과 이유를 기록한다.
- PASS, FAIL, BLOCKED 판정 근거를 보존한다.
- PR 작성과 최종 회고에 사용할 실행 이력을 제공한다.

로그는 에이전트의 최종 설명을 대신하는 증거다. 실행하지 않은 명령을 실행한 것으로 기록하지 않는다. 실패한 결과를 성공으로 변경하지 않는다.

## 2. 로그 종류

### Attempt Log

하나의 Issue에서 구현, 검증, 리뷰, 수정이 반복될 때 시도별로 기록한다.

실제 Issue 실행 시 다음 경로를 사용한다.

```text
logs/issues/issue-{번호}/attempt-log.md
```

현재 단계에서는 실제 Issue 디렉터리를 만들지 않는다. 양식은 `logs/attempt-log-template.md`를 사용한다.

### Verification Log

테스트, 빌드, diff 확인, Reviewer 판정 결과를 `logs/verification-log.md`에 누적 기록한다.

## 3. 기록 책임

- Planner
  - Issue 식별자
  - 위험도
  - 계획 요약
  - 완료 기준
  - 검증 계획
- Implementer
  - 시도한 변경
  - 변경 파일
  - 계획과 달라진 부분
  - 미구현 항목
  - 구현 중 실행한 명령과 결과
- Verify 단계
  - 테스트와 빌드 명령
  - 종료 결과
  - 실패 원인
  - 미검증 항목
  - 증거 경로
- Reviewer
  - PASS, FAIL, BLOCKED 판정
  - 완료 기준별 근거
  - FAIL 수정 지침
  - BLOCKED 사유
  - 재검증 요구사항
- Human
  - 승인 또는 보류 결정
  - 개입 시점과 이유
  - Merge 여부

## 4. 기록 순서

1. Issue 시작 시 Attempt 1을 생성한다.
2. Implementer 실행 내용을 기록한다.
3. Verify 결과를 기록한다.
4. Reviewer 판정을 기록한다.
5. FAIL이면 다음 시도 지침을 기록한다.
6. 다음 Implementer는 마지막 실패 원인과 다음 시도 지침을 먼저 읽는다.
7. 재시도마다 Attempt 번호를 증가시킨다.
8. PASS 또는 BLOCKED가 되면 최종 상태를 기록한다.
9. 검증 결과를 `verification-log.md`에도 요약한다.
10. PR 준비 시 로그의 실행 근거를 사용한다.

## 5. 기록 원칙

- 사실과 추측을 구분한다.
- 실제 명령과 결과를 기록한다.
- 실패 원인을 삭제하거나 덮어쓰지 않는다.
- 이전 Attempt를 수정해 성공처럼 만들지 않는다.
- 새로운 Attempt를 추가하는 방식으로 기록한다.
- 민감 정보, 토큰, 비밀번호, 개인정보를 기록하지 않는다.
- 전체 콘솔 출력을 무조건 붙이지 않고 핵심 결과와 증거 경로를 기록한다.
- Issue 범위 밖 문제는 별도 항목으로 분리한다.
- Human 승인이 필요한 항목을 명확히 표시한다.

## 6. 완료 상태

각 실행은 PASS, FAIL, BLOCKED 중 하나로 종료한다. 상태 정의와 전환 규칙은 `.codex/skills/coffee-order-issue-loop/SKILL.md`를 따른다.

## 7. 현재 구축 제한

현재 단계에서는 로그 구조와 양식만 정의한다. 다음은 아직 수행하지 않는다.

- 실제 Issue 로그 생성
- 로그 자동 작성
- 로그 누락 차단
- 재시도 횟수 강제
- hooks 또는 scripts 연결
- GitHub Issue와 PR 자동 연결

위 기능은 후속 단계에서 구축한다.
