---
name: coffee-order-issue-loop
description: 하나의 GitHub Issue를 확인하고 컨텍스트 라우팅, 계획, 구현, 검증, 리뷰, 수정, 기록, PR 준비까지 처리하는 저장소 전용 절차다. 사용자가 coffee-order-issue-loop 실행을 명시하거나 AGENTS.md가 이 Skill로 라우팅한 Issue 단위 작업에 사용한다.
---

# Coffee Order Issue Loop

## 1. 목적

- 하나의 GitHub Issue를 계획부터 PR 준비까지 일관된 절차로 처리한다.
- Human이 각 실행 단계를 별도 프롬프트로 연결하는 작업을 줄인다.
- 하나의 Codex 실행 안에서 Planner, Implementer, Reviewer 역할을 순서대로 전환한다.
- Merge와 운영 배포는 자동 수행하지 않는다.

## 2. 호출 조건과 구축 단계 제한

사용자가 이 Skill을 명시적으로 호출하거나 `AGENTS.md`가 이 경로로 라우팅할 때 사용한다. 자동 발동을 가정하지 않는다.

필수 입력은 **Issue 번호 또는 Issue URL**이다. Issue가 없으면 `BLOCKED: ISSUE REQUIRED`로 종료한다. Issue 본문에 작업 목적, 구현 범위, 완료 기준, 수정 가능·금지 범위, 검증 방법, 위험도가 부족하면 임의로 보충하지 않고 `BLOCKED: WORK CONTEXT REQUIRED`로 종료한다.

HIGH 위험 작업에 Human의 설계 승인이 없으면 `BLOCKED: HUMAN DESIGN APPROVAL REQUIRED`로 종료한다.

Planner·Implementer·Reviewer 역할 정의, logs 구조, scripts 또는 hooks, GitHub Issue·PR 연결이 모두 구축되기 전에는 실제 GitHub Issue를 대상으로 이 Skill을 실행하거나 검증하지 않는다. 현재 단계에서는 SKILL.md 문서 구조만 정의한다.

## 3. 상태 원칙

- `PASS`: 완료 기준, 필수 검증, 독립 Review, 필요한 Human Gate를 모두 충족하고 Issue 범위 이탈과 핵심 미검증 항목이 없다.
- `FAIL`: 원인과 수정 방향이 확인된 구현 또는 검증 문제이며 재시도 한도 안에서 수정할 수 있다.
- `BLOCKED`: Issue나 필수 컨텍스트 부족, 요구사항 불명확, 저장소 충돌, 보호 경계, 권한·환경 문제, Human Gate, 재시도 한도 때문에 자동 진행할 수 없다.

실패한 검증을 성공으로 보고하지 않는다. 실행하지 않은 검증을 통과로 기록하지 않는다.

## 4. 전체 실행 절차

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

### 5. Implement

- 목적: Implementer로 전환해 승인된 계획과 Issue 범위 안에서 구현한다.
- 다음 단계 조건: 계획된 변경이 완료되고 검증 가능한 상태다.
- BLOCKED 조건: Issue 밖 기능, 불필요한 리팩터링, 보호 경계 위반, 문서와 다른 구현이 필요하면 BLOCKED로 전환한다.

세부 역할 규칙과 출력 형식은 역할 정의 단계에서 별도로 정의한다.

### 6. Verify

- 목적: 가장 작은 관련 검증부터 관련 테스트, 전체 테스트, 전체 빌드, `git status`, `git diff --stat`, `git diff` 순으로 확인한다.
- 다음 단계 조건: 실행 결과와 미실행 항목이 사실대로 정리된다.
- BLOCKED 조건: 권한이나 환경 문제로 핵심 검증을 수행할 수 없으면 BLOCKED로 전환한다. 검증 실패 자체는 FAIL로 Review에 전달한다.

### 7. Review

- 목적: Reviewer로 전환해 Issue, 실제 diff, 검증 결과를 기준으로 독립 검토한다.
- 다음 단계 조건: PASS이면 Record로 이동하고, FAIL이면 Fix로 이동한다.
- BLOCKED 조건: 필수 근거 부족이나 Human Gate 누락이면 BLOCKED로 종료하며 자동 수정하지 않는다.

세부 역할 규칙과 출력 형식은 역할 정의 단계에서 별도로 정의한다.

### 8. Fix

- 목적: Verify 실패 또는 Reviewer FAIL의 확인된 원인과 수정 방향 안에서만 변경한다.
- 다음 단계 조건: 수정이 끝나고 다시 검증할 수 있다.
- BLOCKED 조건: Issue 범위 밖 변경이 필요하면 `BLOCKED: ISSUE SCOPE CHANGE REQUIRED`, 재시도 한도에 도달하면 `BLOCKED: RETRY LIMIT`로 종료한다.

### 9. Re-verify

- 목적: 수정 후 Verify와 Review를 다시 수행한다.
- 다음 단계 조건: PASS이면 Record로 이동하고, FAIL이면 한도 안에서 Fix로 돌아간다.
- BLOCKED 조건: Reviewer가 BLOCKED를 반환하거나 재시도 한도에 도달하면 자동 수정을 중단한다.

무한 반복을 막기 위한 재시도 한도를 둔다. 구체적인 횟수와 강제 방식은 scripts 또는 hooks 구축 단계에서 결정한다.

### 10. Record

- 목적: 실행 과정과 결과를 향후 구축될 logs 구조에 기록한다.
- 다음 단계 조건: Issue 식별자, 실행 단계, 변경 파일, 검증 결과, Review 결과, 재시도 결과, 최종 상태, Human 확인 항목이 정리된다.
- BLOCKED 조건: 필수 실행 근거가 누락되어 최종 상태를 설명할 수 없으면 BLOCKED로 전환한다.

구체적인 로그 경로와 파일 형식은 logs 구축 단계에서 정한다. 이번 단계에서는 로그 파일이나 logs 디렉터리를 만들지 않는다.

### 11. Prepare Pull Request

- 목적: PASS일 때 Issue 연결 정보, PR 제목, PR 본문 초안을 준비한다.
- 다음 단계 조건: 초안이 Issue 범위와 검증 결과를 정확히 반영한다.
- BLOCKED 조건: PASS가 아니거나 Issue 연결과 검증 근거가 불충분하면 PR 준비를 완료하지 않는다.

### 12. Human Handoff

- 목적: 최종 상태, 검증·Review 결과, 미검증 항목, Human 확인 항목, PR 초안을 전달한다.
- 다음 단계 조건: Human이 후속 GitHub 작업 또는 종료 여부를 판단한다.
- 종료 경계: Human 승인 없이는 Commit, Push, PR 실제 생성, Merge, 운영 배포로 진행하지 않는다.

## 5. Human Gate와 실행 경계

- 데이터 삭제, DB Migration 실행, 인증·인가 정책 변경, 외부 비용 발생 작업은 Human Gate를 요구한다.
- 미승인 HIGH 위험 설계는 자동 진행하지 않는다.
- Issue 범위 밖 기능을 추가하거나 불필요한 리팩터링을 하지 않는다.
- 기존 변경사항을 임의로 삭제하지 않는다.
- Merge와 운영 배포를 자동 수행하지 않는다.
- Human 승인 없이 Commit, Push, PR 실제 생성을 수행하지 않는다.

상세 경계는 `AGENTS.md`, `docs/06_CODEX_RULES.md`, `docs/11_AI_AUTOMATION_EXPERIMENT.md`의 원본 규칙을 따른다.

## 6. GitHub 처리 범위

현재는 Issue 내용을 확인하고 Issue와 변경사항의 연결 정보를 정리하며, PASS일 때 PR 제목과 본문 초안만 작성한다.

실제 Issue 생성, Commit, Push, PR 생성, Merge는 수행하지 않는다. 실행 권한과 방식은 향후 GitHub Issue·PR 연결 구축 단계에서 별도로 정한다.
