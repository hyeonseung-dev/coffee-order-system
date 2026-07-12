---
name: coffee-order-issue-loop
description: 하나의 GitHub Issue를 확인하고 컨텍스트 라우팅, 계획, 구현, 검증, 리뷰, 수정, 기록, PR 준비까지 처리하는 저장소 전용 절차다. 사용자가 coffee-order-issue-loop 실행을 명시하거나 AGENTS.md가 이 Skill로 라우팅한 Issue 단위 작업에 사용한다.
---

# Coffee Order Issue Loop

## 1. 목적

- 하나의 GitHub Issue를 계획부터 PR 준비까지 일관된 절차로 처리한다.
- Human이 각 실행 단계를 별도 프롬프트로 연결하는 작업을 줄인다.
- 메인 Codex가 오케스트레이터 역할을 수행하고 Planner, Implementer, Reviewer 서브에이전트를 순서대로 호출한다.
- Merge와 운영 배포는 자동 수행하지 않는다.

## 2. 호출 조건과 구축 단계 제한

사용자가 이 Skill을 명시적으로 호출하거나 `AGENTS.md`가 이 경로로 라우팅할 때 사용한다. 자동 발동을 가정하지 않는다.

필수 입력은 **Issue 번호 또는 Issue URL**이다. Issue가 없으면 `BLOCKED: ISSUE REQUIRED`로 종료한다. Issue 본문에 작업 목적, 구현 범위, 완료 기준, 수정 가능·금지 범위, 검증 방법, 위험도가 부족하면 임의로 보충하지 않고 `BLOCKED: WORK CONTEXT REQUIRED`로 종료한다.

HIGH 위험 작업에 Human의 설계 승인이 없으면 `BLOCKED: HUMAN DESIGN APPROVAL REQUIRED`로 종료한다.

Planner·Implementer·Reviewer 서브에이전트, logs 구조, scripts 또는 hooks, GitHub Issue·PR 연결이 모두 구축되기 전에는 실제 GitHub Issue를 대상으로 이 Skill을 실행하거나 검증하지 않는다.

## 3. 상태 원칙

- `PASS`: 완료 기준, 필수 검증, 독립 Review, 필요한 Human Gate를 모두 충족하고 Issue 범위 이탈과 핵심 미검증 항목이 없다.
- `FAIL`: Attempt 1에서 원인과 수정 방향이 명확하고 자동 수정 1회가 가능한 상태다.
- `BLOCKED`: Issue나 필수 컨텍스트 부족, 요구사항 불명확, 저장소 충돌, 보호 경계, 권한·환경 문제, Human Gate, 재시도 한도 때문에 자동 진행할 수 없다.

실패한 검증을 성공으로 보고하지 않는다. 실행하지 않은 검증을 통과로 기록하지 않는다.

## 4. 재시도 한도

- `MAX_ATTEMPTS = 2`
- Attempt 1은 최초 구현이다.
- Attempt 2는 Reviewer FAIL 이후 허용되는 유일한 자동 수정 시도다.
- Attempt 2 이후 Reviewer가 다시 수정 필요 문제를 발견하면 `BLOCKED: RETRY LIMIT`로 종료한다.
- 같은 실패 원인이 반복되면 남은 횟수와 관계없이 즉시 `BLOCKED: REPEATED FAILURE`로 종료한다.
- Reviewer가 BLOCKED를 반환하면 자동 수정하지 않는다.
- 재시도 횟수는 Issue 단위로 계산한다.
- 새로운 Issue나 Human이 승인한 새 계획은 별도 실행으로 취급한다.

## 5. 전체 실행 절차

### 1. Inspect Issue

- 목적: Issue 번호 또는 URL로 대상 Issue와 본문의 필수 입력을 확인한다.
- 다음 단계 조건: Issue와 필수 작업 컨텍스트가 모두 확인된다.
- BLOCKED 조건: Issue가 없으면 `BLOCKED: ISSUE REQUIRED`, 필수 내용이 부족하면 `BLOCKED: WORK CONTEXT REQUIRED`다.

### 2. Route Context

- 목적: `AGENTS.md`의 라우팅에 따라 Issue에 필요한 원본 문서만 읽는다.
- 다음 단계 조건: 요구사항, 완료 기준, 관련 설계와 작업 경계를 설명할 수 있다.
- BLOCKED 조건: 문서가 충돌하거나 요구사항을 확정할 수 없으면 `BLOCKED: REQUIREMENT UNCLEAR`다.

### 3. Preflight

- 목적: `git branch --show-current`, `git status`, `git log --oneline -5`로 브랜치와 작업 트리를 확인한다.
- 다음 단계 조건: 현재 브랜치가 예상과 일치하고 기존 변경과 충돌 없이 Issue 범위에서 작업할 수 있다.
- BLOCKED 조건: 잘못된 브랜치는 `BLOCKED: WRONG BRANCH`, 무관한 변경이나 충돌 가능성은 `BLOCKED: WORKTREE NOT CLEAN`, 보호 파일 변경 필요는 `BLOCKED: PROTECTED FILE CHANGE REQUIRED`다.

기존 변경사항을 임의로 삭제하거나 수정하지 않는다. 상세 보호 경계와 중단 조건은 `docs/06_CODEX_RULES.md`를 따른다.

### 4. Plan

- 목적: Planner로 전환해 Issue와 관련 컨텍스트를 분석하고 실행 계획과 검증 기준을 작성한다.
- 다음 단계 조건: 계획이 Issue 범위, 완료 기준, 위험도와 일치한다.
- BLOCKED 조건: 범위 확장, 보호 경계 위반, 미승인 HIGH 위험 설계가 필요하면 BLOCKED로 전환한다.

세부 역할 규칙과 출력 형식은 역할 정의 단계에서 별도로 정의한다.

이 단계에서는 `.codex/agents/planner.md` 서브에이전트를 호출한다.

### 5. Implement

- 목적: Implementer로 전환해 승인된 계획과 Issue 범위 안에서 구현한다.
- 다음 단계 조건: 계획된 변경이 완료되고 검증 가능한 상태다.
- BLOCKED 조건: Issue 밖 기능, 불필요한 리팩터링, 보호 경계 위반, 문서와 다른 구현이 필요하면 BLOCKED로 전환한다.

세부 역할 규칙과 출력 형식은 역할 정의 단계에서 별도로 정의한다.

이 단계에서는 `.codex/agents/implementer.md` 서브에이전트를 호출한다.

### 6. Verify

- 목적: 가장 작은 관련 검증부터 관련 테스트, 전체 테스트, 전체 빌드, `git status`, `git diff --stat`, `git diff` 순으로 확인한다.
- 다음 단계 조건: 실행 결과와 미실행 항목이 사실대로 정리된다.
- BLOCKED 조건: 권한이나 환경 문제로 핵심 검증을 수행할 수 없으면 BLOCKED로 전환한다. 검증 실패 자체는 FAIL로 Review에 전달한다.

### 7. Review

- 목적: Reviewer로 전환해 Issue, 실제 diff, 검증 결과를 기준으로 독립 검토한다.
- 다음 단계 조건: PASS, FAIL, BLOCKED 판정과 근거를 확정하고 Record로 이동한다.
- BLOCKED 조건: 필수 근거 부족이나 Human Gate 누락이면 BLOCKED로 판정하며 자동 수정하지 않는다.

세부 역할 규칙과 출력 형식은 역할 정의 단계에서 별도로 정의한다.

이 단계에서는 `.codex/agents/reviewer.md` 서브에이전트를 호출한다.

### 8. Fix

- 목적: Attempt 1의 Reviewer FAIL일 때만 Implementer를 다시 호출해 자동 Fix를 한 번 수행한다.
- 다음 단계 조건: 수정이 끝나고 다시 검증할 수 있다.
- BLOCKED 조건: Issue 범위 밖 변경이나 설계 재결정이 필요하면 `BLOCKED: ISSUE SCOPE CHANGE REQUIRED`, Reviewer 지침만으로 안전하게 수정할 수 없으면 `BLOCKED: REVIEW INSTRUCTION INSUFFICIENT`로 종료한다.

Planner 계획 전체를 다시 작성하지 않는다. 전체 대화나 전체 리뷰 대신 실패 원인, 수정 대상, 수정 지침, 수정 금지 범위, 재검증 명령만 포함한 Reviewer 최소 수정 패킷을 Implementer에게 전달한다.

### 9. Re-verify

- 목적: Attempt 2에서 Verify와 Review를 다시 수행한다.
- 다음 단계 조건: PASS이면 PASS 근거를 확정하고 Record로 이동한다.
- BLOCKED 조건: 수정 필요 문제가 남으면 추가 Fix 없이 `BLOCKED: RETRY LIMIT`, 이전과 동일한 실패이면 `BLOCKED: REPEATED FAILURE`로 판정하고 Record로 이동한다. Reviewer가 BLOCKED를 반환해도 Record로 이동한다.

### 10. Record

- 목적: Planner, Implementer, Verify, Reviewer 결과를 Attempt Log에 기록하고 검증 결과를 Verification Log에 기록한 뒤 현재 Attempt와 상태를 확정한다.
- 다음 단계 조건: Issue 식별자, 실행 단계, 변경 파일, 검증 결과, Review 결과, 재시도 결과, 최종 상태, Human 확인 항목이 사실대로 기록되면 Enforce로 이동한다.
- BLOCKED 조건: 필수 실행 근거가 누락되어 최종 상태를 설명할 수 없으면 BLOCKED로 전환한다.

로그 기록 전에 Enforce를 실행하지 않는다. 검사 실패 후 존재하지 않는 근거를 만들거나 실패 결과를 성공처럼 바꾸지 않는다. 단순 기록 누락이고 실제 근거가 존재할 때만 Record로 돌아가 사실에 맞게 한 번 보완한 뒤 Enforce를 한 번 다시 수행할 수 있다.

### 11. Enforce

- 목적: Record 완료 후 `.codex/hooks/require-log.sh`를 절차적으로 호출해 현재 상태가 로그와 재시도 규칙을 충족하는지 읽기 전용으로 검사한다.
- 다음 단계 조건: 실제 종료 코드와 핵심 출력을 확인한 뒤 아래 상태 전이를 따른다.
- BLOCKED 조건: PASS 또는 FAIL 검사에서 종료 코드 1이면 모든 자동 진행을 중단하고 Human Handoff로 이동한다. BLOCKED 검사는 종료 코드를 기록한 뒤 GitHub Report dry-run을 거쳐 Human Handoff로 이동한다.

다음 형식으로 실행한다.

```bash
bash .codex/hooks/require-log.sh \
  --issue {issue-number} \
  --attempt {current-attempt} \
  --status {PASS|FAIL|BLOCKED}
```

`issue-number`는 현재 GitHub Issue 번호, `current-attempt`는 실제 시도 번호 1 또는 2, `status`는 Reviewer 또는 실행 흐름이 확정한 실제 상태를 사용한다. Enforce는 로그를 생성하거나 수정하지 않는다.

실행 기록에는 명령, Issue 번호, Attempt 번호, 전달 상태, 실제 종료 코드, 핵심 출력, 검사한 Attempt Log와 Verification Log 경로, 다음 단계 결정을 남긴다. 종료 코드를 추정하지 않는다.

상태별 전이를 적용한다.

- Attempt 1 + PASS: PASS 근거를 Record한 뒤 검사한다. 종료 코드 0이면 GitHub Report, 1이면 Human Handoff로 이동한다.
- Attempt 1 + FAIL: 실패 원인, 수정 대상, 다음 시도 지침, 수정 금지 범위, 재검증 명령, 자동 재시도 허용 여부를 Record한다. 종료 코드 0일 때만 Fix로 이동해 Implementer를 Attempt 2로 한 번 호출한다. 검사 통과는 수정 패킷의 기록 완료를 의미하며 구현 완료를 의미하지 않는다.
- Attempt 2 + PASS: 재검증 결과와 최종 PASS 근거를 Record한다. 종료 코드 0이면 GitHub Report, 1이면 Human Handoff로 이동한다.
- BLOCKED: BLOCKED 상태, Attempt 횟수, 마지막 실패 원인, 마지막 테스트 또는 빌드 결과, 수정된 파일, 미검증 항목, Human 결정 사항을 Record한 뒤 검사한다. 검사 결과를 확인한 후 GitHub Report dry-run을 거쳐 Human Handoff로 이동한다. Fix나 Prepare Pull Request로 이동하지 않는다. 종료 코드 0은 중단 정보가 정상 기록됐다는 의미일 뿐 작업 성공을 뜻하지 않는다.
- Attempt 2 + FAIL: 방어적으로 스크립트를 실행해 `BLOCKED: RETRY LIMIT`로 상태를 확정하고 Record한 뒤 BLOCKED GitHub Report dry-run과 Human Handoff로 이동한다. Attempt 3을 만들거나 Implementer를 다시 호출하지 않는다.

Enforce가 실패하면 오류 또는 BLOCKED 코드, 누락·위반 항목, 두 로그 경로, Human 확인 항목을 기록한다. 검사 실패를 무시하거나, 실제 상태·Attempt와 다른 값을 전달하거나, 검사를 생략하거나, 직접 GitHub Report·PR 준비 또는 추가 Fix로 우회하지 않는다.

### 12. GitHub Report

- 목적: Enforce 후 최종 PASS 또는 BLOCKED 상태를 `.codex/hooks/report-github-status.sh`로 GitHub 댓글 형식의 dry-run으로 정리한다.
- 다음 단계 조건: PASS dry-run 종료 코드가 0이면 Prepare Pull Request, BLOCKED이면 dry-run 결과와 관계없이 Human Handoff로 이동한다.
- BLOCKED 조건: dry-run 종료 코드가 1이면 Prepare Pull Request와 실제 게시를 수행하지 않고 Human Handoff로 이동한다.

Attempt 1의 FAIL은 내부 자동 수정 상태이므로 GitHub Report를 실행하지 않는다. Record와 Enforce를 통과한 뒤 Fix로 이동하며, Attempt 2의 최종 PASS 또는 BLOCKED만 보고 대상으로 삼는다.

기본 실행은 dry-run이다. PASS에서 PR 번호가 아직 없으면 Issue를 대상으로 사용하고, PR이 이미 존재하며 번호를 신뢰할 수 있을 때만 `pr`을 사용한다.

```bash
bash .codex/hooks/report-github-status.sh \
  --target issue \
  --number {issue-number} \
  --status {PASS|BLOCKED} \
  --attempt {current-attempt} \
  --reason "{final-reason}" \
  --log-path "logs/issues/issue-{issue-number}/attempt-log.md" \
  --verification-log "logs/verification-log.md" \
  --changed-files "{changed-files-summary}" \
  --unverified "{unverified-summary}" \
  --human-action "{required-human-action}"
```

실행 기록에는 모드, target, 번호, 상태, Attempt, reason, 실제 종료 코드, 핵심 출력, 실제 게시 여부, Human 승인 여부, 다음 단계 결정을 남긴다. dry-run은 `Published: NO`로 기록한다.

PASS는 Enforce 종료 코드 0 이후 dry-run을 실행한다. dry-run 종료 코드가 0이면 Human 승인 없이 publish하지 않은 채 Prepare Pull Request로 이동한다. BLOCKED는 Enforce 결과를 확인한 뒤 dry-run 댓글을 Human에게 보여주고 자동 구현을 종료한다.

dry-run 실패 시 원인, 대상 종류와 번호, 로그 경로와 입력값, 민감 정보 포함 여부를 확인해 Human Handoff로 이동한다. 실패를 무시하거나 FAIL을 다른 상태로 바꾸거나 잘못된 번호로 게시하거나 로그 전체·민감 정보를 게시하지 않는다.

실제 게시에는 별도 Human Gate가 필요하다. dry-run 결과, 대상 종류와 번호, 민감 정보 없음, 실제 상태와 Attempt 일치를 Human이 확인하고 이번 댓글 게시를 명시적으로 승인한 경우에만 별도 명령에 `--publish`를 추가한다. 댓글 게시 승인과 PR Merge 승인은 서로 다른 승인이고, 게시 승인은 Merge 승인을 의미하지 않는다.

### 13. Prepare Pull Request

- 목적: PASS일 때 Issue 연결 정보, PR 제목, PR 본문 초안을 준비한다.
- 다음 단계 조건: Reviewer가 PASS이고, Attempt가 1 또는 2이며, Record와 두 로그가 완료되고, Enforce PASS 검사의 실제 종료 코드가 0이고, GitHub Report PASS dry-run 종료 코드가 0이며, 핵심 미검증 항목이 없고 필요한 Human Gate가 충족됐을 때만 진입한다.
- BLOCKED 조건: FAIL·BLOCKED 상태, Enforce 또는 GitHub Report dry-run 실패, Issue 연결이나 검증 근거 부족 중 하나라도 해당하면 PR 준비를 수행하지 않는다. 실제 댓글 publish는 진입 필수 조건이 아니다.

### 14. Human Handoff

- 목적: 최종 상태, 검증·Review 결과, 미검증 항목, Human 확인 항목, PR 초안을 전달한다.
- 다음 단계 조건: Human이 후속 GitHub 작업 또는 종료 여부를 판단한다.
- 종료 경계: Human 승인 없이는 Commit, Push, PR 실제 생성, Merge, 운영 배포로 진행하지 않는다.

BLOCKED 종료 시 대상 Issue, 최종 BLOCKED 상태, Attempt 횟수(예: `2/2`), 마지막 실패 원인, 마지막 테스트 또는 빌드 결과, 수정했던 파일, 미검증 항목, 향후 Attempt Log 경로, Human이 결정해야 할 사항을 즉시 보고한다.

Attempt Log 경로는 `logs/issues/issue-{번호}/attempt-log.md`를 사용한다. GitHub 연결 단계에서는 BLOCKED 시 상태, Attempt 횟수, 마지막 실패, 로그 경로, Human 결정 필요 사항을 Issue 또는 PR 댓글로 알리도록 구현할 예정이며 현재는 댓글을 작성하지 않는다.

Human Handoff에는 Enforce 결과, GitHub Report dry-run 결과, 게시 예정 대상과 번호, 댓글 본문 요약, 실제 publish 승인 필요 여부, Attempt Log와 Verification Log 경로도 포함한다. Human이 게시를 승인하면 별도 `--publish` 명령을 실행하고, 승인하지 않으면 dry-run 결과만 남긴다.

## 6. Enforce 연결의 현재 한계

- `require-log.sh`는 독립 실행형 검사 스크립트다.
- 현재 연결은 `coffee-order-issue-loop`를 수행하는 메인 Codex의 절차적 호출이다.
- Codex native hook으로 자동 등록된 상태가 아니다.
- Git commit, push 또는 CI 수준의 외부 차단은 구현되지 않았다.
- Git hook과 GitHub Actions 연결은 후속 단계에서 구현한다.
- GitHub 상태 보고는 다음 단계의 독립 실행형 스크립트 dry-run으로 연결되며 실제 publish는 자동 수행하지 않는다.

## 7. GitHub Report 연결의 현재 한계

- `report-github-status.sh`는 독립 실행형 스크립트다.
- 현재 연결은 메인 Codex가 절차적으로 호출하는 방식이다.
- Codex native hook으로 자동 등록된 상태가 아니다.
- dry-run은 자동 수행할 수 있지만 publish는 Human 승인이 필요하다.
- 댓글 중복 방지와 기존 댓글 갱신은 구현되지 않았다.
- GitHub Actions 또는 CI 연동은 구현되지 않았다.
- 데스크톱, Slack, 이메일 알림은 구현되지 않았다.
- 댓글 publish 실패가 애플리케이션 구현 결과를 자동으로 변경하지는 않는다.

## 8. Human Gate와 실행 경계

- 데이터 삭제, DB Migration 실행, 인증·인가 정책 변경, 외부 비용 발생 작업은 Human Gate를 요구한다.
- 미승인 HIGH 위험 설계는 자동 진행하지 않는다.
- Issue 범위 밖 기능을 추가하거나 불필요한 리팩터링을 하지 않는다.
- 기존 변경사항을 임의로 삭제하지 않는다.
- Merge와 운영 배포를 자동 수행하지 않는다.
- Human 승인 없이 Commit, Push, PR 실제 생성을 수행하지 않는다.

상세 경계는 `AGENTS.md`, `docs/06_CODEX_RULES.md`, `docs/11_AI_AUTOMATION_EXPERIMENT.md`의 원본 규칙을 따른다.

## 9. GitHub 처리 범위

현재는 Issue 내용을 확인하고 Issue와 변경사항의 연결 정보를 정리하며, PASS 또는 BLOCKED의 GitHub Report dry-run과 PASS일 때 PR 제목·본문 초안을 작성한다.

실제 댓글 publish는 별도 Human 승인 후에만 수행한다. Issue 생성, Commit, Push, PR 생성, Merge는 수행하지 않는다.
