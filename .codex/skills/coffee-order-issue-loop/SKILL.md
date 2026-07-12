---
name: coffee-order-issue-loop
description: Human 사전 검토와 Refinement를 마친 READY GitHub Issue를 기준으로 위험도 확인, 구현, 테스트·검증, Troubleshooting Gate, 간단 리뷰, Commit·Push·PR 인계까지 수행하는 저장소 전용 절차다.
---

# Coffee Order Issue Loop

## 1. 목적

- Human의 반복 지시 없이 승인된 READY Issue를 PR까지 처리한다.
- Codex는 코드, 테스트, 로컬 검증에 집중한다.
- 설계·정합성·보안·성능에 영향을 주는 문제는 Human에게 반환한다.
- PR을 ChatGPT와 Human으로의 공식 인계 지점으로 사용한다.
- Merge는 어떤 경우에도 자동 수행하지 않는다.
- AI Draft가 아니라 Human이 다시 해석하고 승인한 Issue만 구현한다.

## 2. 필수 입력

- GitHub Issue 번호 또는 URL
- Issue 상태 `READY`
- 담당 Human 사전 검토 완료
- ChatGPT Refinement 결과와 Human 최종 결정
- 작업 목적과 완료 조건
- 핵심 도메인 규칙
- 제외 범위
- 수정 허용·금지 범위
- 위험도 `LOW | MEDIUM | HIGH`
- 테스트 계획
- 검증 방법과 성공 기준
- 예상 트러블슈팅과 Human 공유 조건
- 필요한 ADR과 Human 승인 상태

필수 입력이 없으면 임의로 보충하지 않는다.

- Human 사전 검토가 없으면 `BLOCKED: HUMAN PRE-REFINEMENT REQUIRED`
- 그 외 Issue 계약 정보가 부족하면 `BLOCKED: ISSUE REFINEMENT REQUIRED`

Codex는 Human의 생각이 충분히 깊은지 평가하지 않는다. Issue에 Human 사전 검토와 최종 승인 기록이 존재하는지만 확인한다.

## 3. 상태

- `PASS`: 구현, 필수 테스트·빌드·diff 검증과 간단 Review가 완료돼 PR 생성 가능
- `FAIL: FIXABLE`: Issue 범위 안에서 일반 디버깅으로 수정 가능
- `BLOCKED`: Human 판단, 설계 변경, 환경·권한 조치가 필요해 자동 진행 불가

실행하지 않은 검증을 통과로 기록하지 않는다.

## 4. 위험도별 진입 규칙

### LOW

기존 패턴을 따르는 명확한 작업이다.

- 최소 Preflight 후 바로 구현한다.
- 별도 Planner 승인 없이 구현부터 PR 생성까지 진행한다.
- 관련 테스트와 전체 검증은 생략하지 않는다.

### MEDIUM

설계 영향이 있는 작업이다.

- Planner가 짧은 계획, 영향 범위, 수정 파일, 테스트를 작성한다.
- Human에게 계획을 보고하고 승인 전에는 코드를 수정하지 않는다.
- 승인 후 구현부터 PR 생성까지 진행한다.

### HIGH

데이터 손실, 보안, 동시성, 운영 장애 가능성이 큰 작업이다.

- Human 사전 검토에서 설계와 트레이드오프가 정리됐는지 확인한다.
- 관련 ADR이 Accepted인지 확인한다.
- 명시적 Human 구현 승인을 확인한다.
- 조건이 부족하면 `BLOCKED: HIGH RISK APPROVAL REQUIRED`다.
- 구현 중 새로운 설계 선택이 발견되면 즉시 Troubleshooting Gate로 전환한다.

별도 실행 모드를 추가하지 않는다. 위험도 하나로 Gate 수준을 결정한다.

## 5. 실행 절차

### 1. Inspect Issue

확인 항목:

- Issue 상태가 READY인지
- 담당 Human 사전 검토가 작성됐는지
- ChatGPT 검토 후 Human 최종 결정이 기록됐는지
- 해결하려는 문제와 핵심 도메인 규칙이 명확한지
- 위험도와 Human Gate가 명확한지
- 완료 조건이 테스트·검증 가능한지
- 관련 ADR 상태가 올바른지
- 수정 경계와 문서 영향이 명확한지

DRAFT Issue는 구현하지 않는다.

Human 사전 검토가 비어 있거나 AI Draft를 그대로 복사한 것으로 명시돼 있으면 구현하지 않고 Human에게 반환한다. 단, 내용의 학습 수준을 Codex가 임의 평가하지는 않는다.

### 2. Route Context

`AGENTS.md`의 라우팅에 따라 현재 Issue에 필요한 문서와 코드만 읽는다.

확인 우선순위:

1. Issue의 Human 사전 검토와 최종 결정
2. 관련 API·ERD·ADR
3. 현재 구현 코드와 테스트
4. 수정 허용·금지 범위

모든 문서와 전체 저장소를 반복해서 읽지 않는다.

### 3. Minimal Preflight

```bash
git branch --show-current
git status
git log --oneline -5
```

확인 항목:

- 올바른 작업 브랜치인지
- 기존 미커밋 변경과 충돌하지 않는지
- 보호 파일 변경이 필요한지
- 관련 테스트와 실행 환경이 있는지

형식적 메타데이터 누락만으로 BLOCKED 처리하지 않는다. 다만 READY 계약의 핵심인 Human 사전 검토, 완료 조건, 수정 경계가 없으면 진행하지 않는다.

### 4. Plan

- LOW: 메인 Codex가 수정 파일, 구현 순서, 테스트만 짧게 정리하고 바로 진행한다.
- MEDIUM: `.codex/agents/planner.md`를 사용해 계획하고 Human 승인 후 진행한다.
- HIGH: Accepted ADR과 Human 설계·구현 승인 여부를 확인한 뒤 계획한다.

계획은 다음 항목만 포함한다.

- 구현 범위
- 수정 예상 파일
- 구현 순서
- 검증할 테스트
- 직접 검증 방법
- 제외 범위

계획이 Human 최종 결정이나 ADR과 충돌하면 구현하지 않고 보고한다.

### 5. Implement

`.codex/agents/implementer.md` 기준으로 다음을 수행한다.

- Issue 범위 안에서 최소 변경
- production 코드와 필요한 테스트 작성
- 기존 API·DB·예외 계약 유지
- Human이 정리한 핵심 도메인 규칙 반영
- 실패와 롤백 흐름 검증
- 불필요한 리팩터링과 새 기술 도입 금지

Markdown 문서는 Issue에서 허용한 경우에만 수정한다.

### 6. Debug or Troubleshooting Gate

#### 자체 해결

다음은 Issue 범위 안에서 스스로 해결한다.

- 컴파일 오류
- import 누락
- 단순 테스트 데이터 오류
- 명확한 Mock 설정 오류
- 정적 검사와 코드 스타일 오류
- 기존 패턴으로 해결 가능한 테스트 실패

#### Human 보고 후 중단

다음은 `BLOCKED: TROUBLESHOOTING GATE`다.

- Human이 결정한 요구사항 또는 설계 변경 필요
- API 계약, DB 스키마, 트랜잭션 경계 변경 필요
- 인증·권한 정책 변경 필요
- 동시성 정합성, 데이터 손실, 중복 처리 위험
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR과 구현 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 또는 보호 파일 변경 필요
- Human 사전 검토에서 가정한 흐름과 실제 코드 구조가 충돌함

보고 형식:

- 발생한 문제
- 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- Human 사전 검토 또는 ADR과의 충돌 지점
- 영향 범위
- 가능한 선택지
- 권장 방향
- 필요한 Human 결정

중요 문제가 발생하면 Human과 ChatGPT가 Issue 또는 ADR을 수정하고 Human이 다시 승인할 때까지 기다린다.

### 7. Verify

다음 순서로 검증한다.

1. 가장 작은 관련 테스트
2. 필요한 통합·동시성·성능 테스트
3. `./gradlew test`
4. `./gradlew build`
5. `git diff --check`
6. `git status`, `git diff --stat`, `git diff`

전체 빌드가 같은 테스트를 반복하는 경우 중복 실행은 줄일 수 있다. 핵심 검증 자체는 생략하지 않는다.

결과를 다음과 같이 구분한다.

- 테스트가 보장하는 것
- 테스트가 보장하지 않는 것
- 직접 검증 결과
- Human이 정의한 완료 조건 충족 여부
- 미검증 항목

테스트 통과만으로 PASS를 선언하지 않는다. Issue 완료 조건, 핵심 도메인 규칙, 실제 코드, 직접 검증 결과를 함께 대조한다.

### 8. Human Understanding Handoff

PR 생성 전 결과 보고에는 Human이 확인해야 할 핵심 흐름을 포함한다.

- 호출 흐름
- 핵심 규칙이 적용되는 위치
- 트랜잭션 경계
- 실패 시 데이터 상태
- 테스트가 증명하는 것
- 선택한 설계와 남은 한계

Codex는 Human이 실제로 이해했다고 대신 선언하지 않는다. 최종 이해 확인은 Human이 수행한다.

### 9. Lightweight Review

`.codex/agents/reviewer.md`가 Issue, Human 사전 검토, 실제 diff, 테스트·빌드 결과만 확인한다.

- 일반 PR: 완료 조건, 범위 이탈, 핵심 버그와 테스트 검토
- 중요 PR: 트랜잭션, 정합성, 동시성, 보안, 성능, ADR 일치 여부 추가 검토

Reviewer는 코드를 수정하거나 테스트를 재실행하지 않는다.

판정:

- `PASS`
- `FAIL: FIXABLE`
- `BLOCKED`

### 10. Fix and Re-verify

`FAIL: FIXABLE`이면 Issue 범위 안에서 수정하고 다시 검증한다.

고정된 Attempt 숫자를 맞추기 위해 중단하지 않는다. 같은 실패를 반복하거나 설계 변경이 필요하면 Troubleshooting Gate로 전환한다.

### 11. Record

모든 Issue에 장문 로그를 강제하지 않는다.

반드시 남길 내용:

- 실행한 테스트와 결과
- 테스트가 보장하는 것과 보장하지 않는 것
- 직접 검증 결과
- 미검증 항목
- Reviewer 판정
- Human 결정이 필요한 BLOCKED 사유

별도 로그를 남길 가치가 있는 경우:

- Human 사전 가정과 실제 동작의 차이
- 트랜잭션·동시성·정합성 문제
- 성능 목표 미달
- 캐시·메시징·DB·CI 환경 문제
- 재발 방지 테스트를 추가한 문제
- 기술 선택이나 ADR을 변경한 문제

단순 오타와 import 수정은 별도 Attempt Log를 강제하지 않는다.

예상 트러블슈팅과 실제 트러블슈팅을 구분한다. 발생하지 않은 문제를 실제 해결 사례로 기록하지 않는다.

### 12. Commit·Push·PR

PASS이면 별도 단계별 Human 승인 없이 다음을 수행한다.

1. Issue 범위의 코드와 테스트를 Commit한다.
2. 작업 브랜치를 Push한다.
3. PR을 생성한다.
4. PR 본문에 다음을 기록한다.
   - 관련 Issue
   - Human 사전 검토와 최종 설계 결정 요약
   - 구현 내용과 주요 변경 파일
   - 테스트와 빌드 결과
   - 테스트가 보장하는 것과 보장하지 않는 것
   - 직접 검증 결과
   - 의미 있는 실제 트러블슈팅
   - 미검증 항목
   - ChatGPT 집중 리뷰 대상
   - Human이 확인해야 할 핵심 코드 흐름
   - 문서 보완 필요 항목
5. PR 번호를 Human에게 전달한다.

PR 이후에는 전체 결과를 대화에 복사하지 않는다.

### 13. PR Handoff

Human은 ChatGPT에 PR 번호만 전달한다.

```text
PR #<번호> 검토하자.
```

ChatGPT는 GitHub에서 Issue의 Human 사전 검토, ADR, diff, 커밋, Actions, 테스트와 문서 정합성을 직접 확인한다.

Codex는 Human과 ChatGPT가 반영하기로 결정한 리뷰만 같은 브랜치와 같은 PR에 반영한다.

## 6. Merge 경계

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

- PASS는 PR 생성 가능 상태이지 Merge 승인 상태가 아니다.
- Human이 승인 문구를 남겨도 Codex는 Merge 명령이나 GitHub Merge 작업을 수행하지 않는다.
- 실제 Merge는 Human이 GitHub UI 또는 본인의 명령으로 직접 수행한다.
- 운영 배포도 별도 Human 승인과 실행이 필요하다.
