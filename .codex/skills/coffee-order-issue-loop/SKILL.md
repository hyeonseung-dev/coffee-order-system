---
name: coffee-order-issue-loop
description: Human이 승인한 READY GitHub Issue를 Codex가 작업 브랜치 생성, 계약 추적, 구현, 검증, Diff 리뷰, 실제 Diff 기반 질문, Commit, Push, Draft PR까지 처리하는 저장소 전용 절차다.
---

# Coffee Order Issue Loop

## 1. 목적

- 승인된 `READY` Issue를 Draft PR까지 처리한다.
- Issue 최종 계약을 코드와 테스트 증거에 1:1로 연결한다.
- 베이스 브랜치의 최신 PR 템플릿을 그대로 사용한다.
- 실제 Diff 기반 질문을 작성해 작성자와 팀원이 변경을 읽게 한다.
- 답변을 AI 정답 시험으로 만들지 않고 오해가 있으면 코드 근거로 설명한다.
- Merge는 어떤 경우에도 자동 수행하지 않는다.

상세 계약 추적과 팀 이해·오해 점검은 `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md`를 따른다.

## 2. 필수 입력과 READY 의미

필수 입력:

- GitHub Issue 번호 또는 URL
- Issue 상태 `READY`
- 담당 Human 착수 검토
- AI 재검증 결과와 Human 최종 합의
- 작업 목적과 완료 조건
- 핵심 도메인 규칙
- 제외 범위
- 수정 허용·금지 범위
- 위험도 `LOW | MEDIUM | HIGH`
- 테스트·직접 검증 계획
- 필요한 ADR과 승인 상태

`READY`는 다음이 이미 끝났다는 뜻이다.

- 담당자 착수 검토
- AI 재검증과 Human 최종 합의
- 위험도 확정
- 테스트·검증 기준 확정
- 필요한 ADR 승인
- Human 구현 시작 승인

필수 입력이 부족하거나 현재 코드와 충돌하면 임의로 보충하지 않는다.

- Human 착수 검토가 없으면 `BLOCKED: HUMAN PRE-REFINEMENT REQUIRED`
- Issue 계약 정보가 부족하면 `BLOCKED: ISSUE REFINEMENT REQUIRED`
- HIGH 작업의 설계·ADR·Human 승인이 없으면 `BLOCKED: HIGH RISK APPROVAL REQUIRED`

## 3. 권위 있는 계약 추출

구현 기준은 다음 영역이다.

1. Human·AI 최종 합의
2. 최종 구현 계약
3. 완료 조건
4. 테스트·직접 검증 계획
5. 포함·제외 범위
6. 수정 허용·금지 범위
7. 위험도와 승인 상태

권위 있는 영역끼리 충돌하거나 현재 코드상 이행할 수 없으면 Troubleshooting Gate로 전환한다.

## 4. 위험도별 실행 규칙

### LOW

- 짧은 계획 후 구현한다.
- 관련 테스트, 전체 CI, Contract Traceability, Diff 리뷰를 수행한다.
- 실제 Diff 기반 질문 3개를 작성한다.
- 작성자가 답변하고 비담당 리뷰어 1명이 확인한다.
- 이해 점검 자체는 Merge 차단 조건이 아니며 중대한 오해가 있을 때만 보류한다.

### MEDIUM

- 구현 범위, 예상 파일, 테스트, 영향 범위를 짧게 보고한다.
- 정상·실패·경계 테스트와 Contract Traceability를 수행한다.
- 실제 Diff 기반 질문 5개를 작성한다.
- 작성자가 답변하고 비담당 리뷰어 최소 1명이 변경 목적·핵심 흐름·실패 위험을 요약한다.
- 중대한 오해가 있을 때만 Merge를 보류한다.

### HIGH

- 구현 전 설계, 트레이드오프, 필요한 ADR, Human 승인이 Issue에 기록돼 있어야 한다.
- 위험에 맞는 권한·Rollback·동시성·정합성 테스트와 Contract Traceability를 수행한다.
- 실제 Diff 기반 질문 5~8개를 작성한다.
- 독립 AI 리뷰가 필수다.
- 작성자가 답변하고 비담당 리뷰어 최소 2명이 이해 요약을 작성한다.
- 팀 공통 영역에 영향이 크면 팀원 전원이 확인한다.
- 필수 참여와 보완 설명 확인이 끝날 때까지 Merge를 보류한다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 5. 실행 절차

```text
Read authoritative contract
→ Minimal Preflight / Create Branch
→ Build Contract Traceability Map
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Link Evidence
→ Fix / Re-verify
→ Verify PR Template Fidelity
→ Generate Actual-Diff Questions
→ Report
→ Commit / Push / Draft PR
```

### 5.1 Read

확인 항목:

- Issue가 `READY`인지
- Human 착수 검토와 최종 결정이 기록됐는지
- 권위 있는 최종 계약 영역이 명확한지
- 위험도와 리뷰 수준이 명확한지
- 완료 조건이 검증 가능한지
- 관련 ADR 상태가 올바른지
- 수정 경계와 문서 영향이 명확한지

DRAFT Issue는 구현하지 않는다.

### 5.2 Minimal Preflight와 작업 브랜치

```bash
git branch --show-current
git status --short
git log --oneline -5
```

처리 규칙:

1. 현재 브랜치가 `main` 또는 `develop`이고 작업 트리가 깨끗하면 `codex/issue-{번호}-{slug}` 브랜치를 생성한다.
2. 대상 Issue 전용 브랜치면 계속한다.
3. 미커밋 변경이나 다른 Issue 브랜치와 충돌하면 덮어쓰지 않고 중단한다.
4. 보호 브랜치에 직접 Commit·Push하지 않는다.

### 5.3 Contract Traceability Map

구현 전에 최종 계약을 원자 항목으로 나눈다.

| ID | 최종 계약 | 예상 구현 위치 | 필요한 증거 |
|---|---|---|---|
| C-01 | 구체적인 결과 또는 구현 제약 | 클래스·메서드·Query·설정 | 테스트·직접 검증·문서 |

반드시 분리할 항목:

- API·요청·응답 계약
- 도메인 상태와 데이터 원본
- 기간·시간대·정렬·동률·빈 결과 정책
- 트랜잭션·Rollback·권한·동시성 규칙
- Human이 확정한 구현 방식
- 도입 금지 기술과 제외 범위
- 테스트·직접 검증 시나리오

### 5.4 Plan

다음만 짧게 보고한다.

- 구현 범위
- 수정 예상 파일
- 구현 순서
- 검증할 테스트
- 직접 검증 방법
- 제외 범위
- 문서 영향

`READY`가 구현 승인이다. 계획 보고 후 추가 승인을 기다리지 않는다. 단, 계약과 현재 코드가 충돌하면 Troubleshooting Gate로 전환한다.

### 5.5 Implement

- Issue 범위 안에서 최소 변경
- production 코드와 필요한 테스트 작성
- 기존 API·DB·예외 계약 유지
- 정상·실패·경계 흐름 구현
- 필요한 Rollback·권한·동시성 흐름 고려
- Human이 확정한 구현 방식 준수
- 불필요한 리팩터링과 새 기술 도입 금지

Markdown 문서는 Issue에서 허용한 경우에만 수정한다.

### 5.6 Debug 또는 Troubleshooting Gate

자체 해결 가능한 문제:

- 컴파일 오류
- import 누락
- 단순 테스트 데이터 오류
- 명확한 Mock 설정 오류
- 정적 검사와 코드 스타일 오류
- 기존 패턴으로 해결 가능한 테스트 실패

다음은 `BLOCKED: TROUBLESHOOTING GATE`다.

- 요구사항 또는 설계 변경 필요
- API·DB·트랜잭션·권한 정책 재결정 필요
- 동시성 정합성, 데이터 손실, 중복 처리 위험
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR과 구현 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 또는 보호 파일 변경 필요
- 권위 있는 계약끼리 충돌
- 최종 계약과 현재 코드 구조 충돌
- 미커밋 변경이나 다른 작업 브랜치 내용을 덮어쓸 위험

보고 내용:

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 영향 범위
- 선택지와 권장 방향
- 필요한 Human 결정

### 5.7 Verify

순서:

1. 변경과 직접 관련된 가장 작은 테스트
2. 필요한 통합·권한·Rollback·동시성·성능 테스트
3. 전체 테스트
4. 전체 빌드
5. diff 검사
6. 필요한 API·DB·로그 직접 검증

기본 명령:

```bash
./gradlew test
./gradlew build
git diff --check
git status
git diff --stat
git diff
```

최종 보고에는 실행한 명령, 테스트 보장 범위, 미검증 범위, 직접 검증, 알려진 제한을 구분한다.

### 5.8 Review Diff와 Contract Traceability

Commit 전에 실제 Diff를 다시 읽는다.

- Issue 완료 조건 충족
- 범위 이탈과 불필요한 리팩터링
- API·ERD·ADR·예외 계약 불일치
- 핵심 버그와 실패 흐름
- 테스트 누락과 의미 없는 테스트
- 실행하지 않은 검증의 허위 기록
- 필요한 문서 보완

계약 추적표를 실제 증거로 갱신한다.

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 구체적인 계약 | `path#symbol` | `TestClass#method` 또는 미검증 사유 | `PASS | HOLD | N/A` |

한 행이라도 `HOLD`이면 Commit·Draft PR 단계로 넘어가지 않는다.

### 5.9 PR Template Fidelity와 질문 생성

Commit 전에 최신 `.github/PULL_REQUEST_TEMPLATE.md`를 다시 읽는다.

필수 규칙:

1. 모든 `##`·`###` 제목 보존
2. 빈 섹션 삭제 금지
3. Contract Traceability 실제 증거 작성
4. Codex가 완료한 항목만 체크
5. Human·ChatGPT·팀원·독립 리뷰·CI·이해 점검 선행 체크 금지
6. 실제 Diff 기반 질문 작성
7. Human·팀원 답변은 비워 둠

질문 수:

- LOW: 3개
- MEDIUM: 5개
- HIGH: 5~8개

### 5.10 Report·Commit·Push·Draft PR

Contract Traceability 전 항목 PASS, 검증, 자체 Diff 리뷰, 템플릿 확인이 끝나면:

1. Issue 범위 변경만 Commit
2. 작업 브랜치 Push
3. Draft PR 생성
4. 생성된 PR 본문 재확인
5. PR 번호 전달

PR 생성 후 다음을 확인한다.

- 베이스와 Head
- Draft 상태
- 템플릿 제목
- Contract Traceability
- 실제 Diff 기반 질문
- 선행 체크 여부

## 6. Human·팀원 답변 확인

작성자와 필요한 팀원이 답변한 뒤 Codex는 최신 Head의 코드와 테스트를 다시 읽는다.

Codex가 기록할 내용:

- 코드와 일치하는 부분
- 보완 설명이 필요한 부분
- 확인된 미검증·잔여 위험
- 중대한 오해 후보

상태는 다음을 사용한다.

- `확인 완료`
- `보완 설명 필요`
- `보완 설명 완료`
- `차단 필요`

Codex는 사람의 이해도를 `PASS | HOLD`로 채점하지 않는다. 답변을 대신 작성하거나 모범 답안 재제출을 요구하지 않는다.

중대한 오해 여부의 최종 판단은 Human 리뷰어가 한다.

중대한 오해 예시:

- 금액·포인트·재고 정합성
- 인증·인가
- 트랜잭션·Rollback
- 동시성 보호 대상
- 캐시와 원본 데이터
- 데이터 삭제·Migration
- 메시지 중복·유실
- 테스트 범위 과대평가

LOW·MEDIUM은 중대한 오해가 있을 때만 Merge를 보류한다. HIGH는 필수 참여와 보완 설명 확인이 끝날 때까지 Merge를 보류한다.

## 7. 리뷰와 Merge 경계

- LOW: 자체 Diff 리뷰 + 작성자 Human 리뷰 + 비담당 리뷰어 확인 + CI
- MEDIUM: 자체 Diff 리뷰 + 작성자 Human 리뷰 + 비담당 리뷰어 최소 1명 + 필요 시 독립 리뷰 + Human/ChatGPT 리뷰 + CI
- HIGH: 구현 전 설계 확인 + 작성자 답변 + 비담당 리뷰어 최소 2명 + 독립 AI 리뷰 + 강화된 Human 리뷰 + 위험 테스트 + CI

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

Merge 전 확인:

- 최신 Commit 기준 CI 성공
- 문서 충돌과 범위 밖 변경 없음
- Contract Traceability 전체 PASS
- LOW·MEDIUM: 중대한 오해 없음
- HIGH: 위험도별 팀 이해·오해 점검 완료 및 중대한 오해 없음
- Human 최종 승인
