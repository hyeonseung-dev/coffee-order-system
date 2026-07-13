---
name: coffee-order-issue-loop
description: Human 착수 검토와 AI 재검증을 마친 READY GitHub Issue를 주 Codex가 Read, Plan, Implement, Verify, Review Diff, Fix, Re-verify, Report 순서로 처리하고 Draft PR까지 인계하는 저장소 전용 절차다.
---

# Coffee Order Issue Loop

## 1. 목적

- Human의 반복 지시 없이 승인된 `READY` Issue를 Draft PR까지 처리한다.
- Codex는 코드, 테스트, 로컬 검증과 실제 diff 자체 리뷰에 집중한다.
- 복잡한 멀티에이전트 구조를 기본값으로 사용하지 않는다.
- 설계·정합성·보안·성능 재결정이 필요한 문제는 Human에게 반환한다.
- PR을 ChatGPT와 Human으로의 공식 인계 지점으로 사용한다.
- Merge는 어떤 경우에도 자동 수행하지 않는다.

## 2. 필수 입력

- GitHub Issue 번호 또는 URL
- Issue 상태 `READY`
- 담당 Human 착수 검토
- AI 재검증 결과와 Human 최종 결정
- 작업 목적과 완료 조건
- 핵심 도메인 규칙
- 제외 범위
- 수정 허용·금지 범위
- 위험도 `LOW | MEDIUM | HIGH`
- 테스트 계획
- 직접 검증 방법과 성공 기준
- 예상 트러블슈팅과 Human 공유 조건
- 필요한 ADR과 승인 상태

필수 입력이 부족하면 임의로 채우지 않는다.

- Human 착수 검토가 없으면 `BLOCKED: HUMAN PRE-REFINEMENT REQUIRED`
- Issue 계약 정보가 부족하면 `BLOCKED: ISSUE REFINEMENT REQUIRED`
- HIGH 작업의 설계·ADR·Human 승인이 부족하면 `BLOCKED: HIGH RISK APPROVAL REQUIRED`

## 3. 위험도별 진입 규칙

### LOW

- 기존 패턴을 따르는 명확한 작업이다.
- 주 Codex가 짧은 계획을 작성하고 바로 구현할 수 있다.
- 관련 테스트, 전체 CI, diff 리뷰는 생략하지 않는다.

### MEDIUM

- 새로운 API, 서비스 로직, 조회 쿼리, 인덱스, 캐시, 이벤트처럼 설계 영향이 있다.
- 주 Codex가 구현 범위, 예상 파일, 테스트, 영향 범위를 보고한다.
- Human 확인 후 구현한다.
- 독립 AI 리뷰는 필요 시 별도 단계에서 수행한다.

### HIGH

- 인증·인가, 금액·포인트·재고, 상태 전이, 트랜잭션, 동시성, DB 구조처럼 데이터 손실·보안·운영 장애 가능성이 크다.
- 구현 전 설계와 트레이드오프가 정리돼 있어야 한다.
- 필요한 ADR이 Accepted 상태여야 한다.
- Human의 명시적 구현 승인이 있어야 한다.
- 구현 후 독립 AI 리뷰와 위험에 맞는 강화 테스트가 필요하다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 4. 실행 절차

```text
Read
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Fix
→ Re-verify
→ Report
→ Commit / Push / Draft PR
```

### 4.1 Read

다음을 확인한다.

- Issue 상태가 `READY`인지
- Human 착수 검토와 최종 결정이 기록됐는지
- 해결하려는 문제와 핵심 도메인 규칙이 명확한지
- 위험도와 리뷰 수준이 명확한지
- 완료 조건이 테스트·검증 가능한지
- 관련 ADR 상태가 올바른지
- 수정 경계와 문서 영향이 명확한지

`AGENTS.md` 라우팅에 따라 현재 Issue에 필요한 문서와 코드만 읽는다.

확인 우선순위:

1. Issue의 Human 착수 검토와 최종 결정
2. 관련 API·ERD·ADR
3. 현재 구현 코드와 테스트
4. 수정 허용·금지 범위
5. `docs/12_EVIDENCE_GUIDE.md`

DRAFT Issue는 구현하지 않는다.

### 4.2 Minimal Preflight

```bash
git branch --show-current
git status
git log --oneline -5
```

확인 항목:

- `main` 또는 `develop` 보호 브랜치가 아닌지
- 기존 미커밋 변경과 충돌하지 않는지
- Issue 범위에 맞는 작업 브랜치인지
- 보호 파일 변경이 필요한지
- 관련 테스트와 실행 환경이 있는지

형식적인 메타데이터 누락만으로 중단하지 않는다. READY 계약의 핵심인 Human 검토, 완료 조건, 수정 경계가 없으면 중단한다.

### 4.3 Plan

주 Codex가 다음만 짧게 정리한다.

- 구현 범위
- 수정 예상 파일
- 구현 순서
- 검증할 테스트
- 직접 검증 방법
- 제외 범위
- 문서 영향

LOW는 계획 후 바로 진행할 수 있다. MEDIUM은 Human 확인 후 진행한다. HIGH는 Accepted ADR과 명시적 Human 승인까지 확인한다.

계획이 Human 결정이나 ADR과 충돌하면 구현하지 않는다.

### 4.4 Implement

- Issue 범위 안에서 최소 변경
- production 코드와 필요한 테스트 작성
- 기존 API·DB·예외 계약 유지
- Human이 정리한 핵심 도메인 규칙 반영
- 정상·실패·경계 흐름 구현
- 필요한 Rollback·권한·동시성 흐름 고려
- 불필요한 리팩터링과 새 기술 도입 금지

Markdown 문서는 Issue에서 허용한 경우에만 수정한다.

### 4.5 Debug 또는 Troubleshooting Gate

Issue 범위 안에서 자체 해결할 수 있는 문제:

- 컴파일 오류
- import 누락
- 단순 테스트 데이터 오류
- 명확한 Mock 설정 오류
- 정적 검사와 코드 스타일 오류
- 기존 패턴으로 해결 가능한 테스트 실패

다음은 `BLOCKED: TROUBLESHOOTING GATE`다.

- Human이 결정한 요구사항 또는 설계 변경 필요
- API 계약, DB 스키마, 트랜잭션 경계 변경 필요
- 인증·권한 정책 재결정 필요
- 동시성 정합성, 데이터 손실, 중복 처리 위험
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR과 구현 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 또는 보호 파일 변경 필요
- Human 착수 검토의 가정과 실제 코드 구조 충돌

보고 형식:

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- Human 결정 또는 ADR과의 충돌 지점
- 영향 범위
- 가능한 선택지와 권장 방향
- 필요한 Human 결정

### 4.6 Verify

다음 순서로 검증한다.

1. 가장 작은 관련 테스트
2. 필요한 통합·권한·Rollback·동시성·성능 테스트
3. `./gradlew test`
4. `./gradlew build`
5. `git diff --check`
6. `git status`, `git diff --stat`, `git diff`
7. 필요한 API·DB·로그 직접 검증

결과를 다음과 같이 구분한다.

- 테스트가 보장하는 것
- 테스트가 보장하지 않는 것
- 직접 검증 결과
- Issue 완료 조건 충족 여부
- 미검증 항목과 알려진 제한

테스트 통과만으로 완료를 선언하지 않는다. Issue, 핵심 규칙, 실제 코드, 직접 검증 결과를 함께 대조한다.

### 4.7 Review Diff

주 Codex가 실제 diff를 다시 읽는다.

확인 항목:

- Issue 완료 조건 충족
- 범위 이탈과 불필요한 리팩터링
- API·ERD·ADR·예외 계약 불일치
- 핵심 버그와 실패 흐름
- 테스트 누락과 의미 없는 테스트
- 실행하지 않은 검증을 성공으로 기록했는지
- 문서 보완 필요 여부

자체 리뷰는 독립 AI 리뷰를 대신하지 않는다. MEDIUM은 필요 시, HIGH는 필수로 별도 독립 리뷰를 수행한다.

### 4.8 Fix와 Re-verify

Issue 범위 안에서 수정 가능한 문제는 고치고 관련 테스트부터 다시 실행한다.

무제한 자동 수정 반복을 하지 않는다. 같은 실패가 반복되거나 설계 변경이 필요하면 Troubleshooting Gate로 전환한다.

### 4.9 Report

반드시 다음을 보고한다.

- 구현 내용과 주요 변경 파일
- 실제 실행한 명령과 결과
- 테스트가 보장하는 것과 보장하지 않는 것
- 직접 검증 결과
- 실제 발생한 의미 있는 트러블슈팅
- 미검증 항목과 알려진 제한
- Human이 설명해야 할 핵심 흐름
- 독립 리뷰 필요 여부
- 문서 보완 필요 항목

모든 Issue에 별도 장문 로그를 강제하지 않는다. 일반적인 근거는 Issue와 PR에 남기고, 의미 있는 트러블슈팅만 별도 기록한다.

### 4.10 Commit·Push·Draft PR

검증과 자체 diff 리뷰가 끝나면 다음을 수행한다.

1. Issue 범위의 변경만 Commit한다.
2. 작업 브랜치를 Push한다.
3. Draft PR을 생성한다.
4. PR 본문에 실제 검증 증거와 위험도를 기록한다.
5. PR 번호를 Human에게 전달한다.

Draft PR 이후에는 구현 결과 전체를 대화에 복사하지 않는다. Human은 PR 번호를 ChatGPT에 전달해 실제 diff와 Actions를 검토하게 한다.

리뷰 수정은 같은 브랜치와 같은 PR에 반영한다.

## 5. 리뷰와 Merge 경계

- LOW: Codex 자체 diff 리뷰 + Human 리뷰 + CI
- MEDIUM: Codex 자체 diff 리뷰 + 필요 시 독립 AI 리뷰 + Human 리뷰 + CI
- HIGH: 구현 전 설계 확인 + 독립 AI 리뷰 + 강화된 Human 리뷰 + 위험에 맞는 테스트 + CI

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

- 자체 PASS는 Draft PR 생성 가능 상태이지 Merge 승인 상태가 아니다.
- 최신 Commit 기준 CI가 성공해야 한다.
- 문서 충돌과 범위 밖 변경이 없어야 한다.
- 작성자가 코드와 테스트를 설명할 수 있어야 한다.
- 실제 Merge는 Human이 GitHub UI 또는 본인의 명령으로 직접 수행한다.
