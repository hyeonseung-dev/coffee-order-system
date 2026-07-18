---
name: coffee-order-issue-loop
description: Human이 승인한 READY GitHub Issue를 Codex가 구현·검증·Commit·Push·Draft PR까지 처리하고, Human이 승인한 리뷰 항목만 수정하는 저장소 전용 절차다.
---

# Coffee Order Issue Loop

## 1. 목적

- 승인된 `READY` Issue를 구현하고 Draft PR까지 만든다.
- Issue 최종 계약을 코드·테스트·직접 검증 증거에 연결한다.
- 실제 Diff 기반 질문을 작성해 Human이 변경 내용을 직접 읽도록 한다.
- Draft PR 이후에는 ChatGPT 전체 검증과 Human 결정에 따라 승인된 수정만 수행한다.
- Codex는 Human 답변을 평가·보완하거나 대신 작성하지 않는다.
- Codex는 Merge하지 않는다.

상세 계약 추적은 `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md`, 질문 난이도는 `docs/15_DIFF_QUESTION_POLICY.md`를 따른다.

## 2. 최종 워크플로우

```text
Codex 구현·Draft PR
→ Human 이해도 작성
→ ChatGPT PR 전체 검증 및 리뷰 댓글
→ Human 반영 범위 결정
→ Codex 승인된 리뷰 수정
→ ChatGPT 최신 Head 재검토
→ Human 최종 Merge
```

역할 경계:

- Codex: 구현, 테스트, 로컬 검증, 자체 Diff 리뷰, 질문 생성, Commit·Push·Draft PR, 승인된 리뷰 수정
- Human 작성자: 실제 Diff를 읽고 이해도 답변 작성
- ChatGPT: Human 답변을 포함한 PR 전체 검증, 리뷰 댓글, 수정 후 최신 Head 재검토
- Human 리뷰어: 반영 범위 결정과 최종 Merge

## 3. 필수 입력과 READY 의미

필수 입력:

- GitHub Issue 번호 또는 URL
- Issue 상태 `READY`
- 담당 Human 착수 검토
- AI 재검증 결과와 Human 최종 합의
- 작업 목적과 완료 조건
- 핵심 도메인 규칙
- 포함·제외 범위
- 수정 허용·금지 범위
- 위험도 `LOW | MEDIUM | HIGH`
- 테스트·직접 검증 계획
- 필요한 ADR과 승인 상태

필수 입력이 부족하거나 현재 코드와 충돌하면 임의로 보충하지 않는다.

- Human 착수 검토 없음: `BLOCKED: HUMAN PRE-REFINEMENT REQUIRED`
- Issue 계약 부족: `BLOCKED: ISSUE REFINEMENT REQUIRED`
- HIGH 설계·ADR·승인 부족: `BLOCKED: HIGH RISK APPROVAL REQUIRED`

## 4. 권위 있는 계약 영역

구현 기준은 다음 순서다.

1. Human·AI 최종 합의
2. 최종 구현 계약
3. 완료 조건
4. 테스트·직접 검증 계획
5. 포함·제외 범위
6. 수정 허용·금지 범위
7. 위험도와 승인 상태

권위 있는 영역끼리 충돌하거나 현재 코드에서 이행할 수 없으면 Troubleshooting Gate로 전환한다.

## 5. 위험도별 실행 규칙

### LOW

- 짧은 계획 후 구현한다.
- 관련 테스트, 전체 검증, Contract Traceability, 자체 Diff 리뷰를 수행한다.
- 실제 Diff 기반 질문 3개를 작성한다.

### MEDIUM

- 구현 범위, 예상 파일, 영향 범위, 정상·실패·경계 테스트를 보고한다.
- Contract Traceability와 필요한 통합 검증을 수행한다.
- 실제 Diff 기반 질문 5개를 작성한다.

### HIGH

- 구현 전 설계, 트레이드오프, ADR, Human 승인을 확인한다.
- 권한·Rollback·동시성·정합성 등 위험에 맞는 강화 테스트를 수행한다.
- 실제 Diff 기반 질문 5~8개를 작성한다.
- 필요한 독립 리뷰 항목을 PR에 인계한다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 6. Codex 구현 절차

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
→ Commit / Push / Draft PR
→ Stop and hand off to Human
```

### 6.1 Read

- Issue가 `READY`인지 확인한다.
- Human 최종 결정과 권위 있는 계약 영역을 확인한다.
- 위험도, 완료 조건, ADR, 수정 경계를 확인한다.
- DRAFT Issue는 구현하지 않는다.

### 6.2 Minimal Preflight와 작업 브랜치

```bash
git branch --show-current
git status --short
git log --oneline -5
```

1. `main` 또는 `develop`이고 작업 트리가 깨끗하면 `codex/issue-{번호}-{slug}` 브랜치를 만든다.
2. 대상 Issue 전용 브랜치면 계속한다.
3. 미커밋 변경이나 다른 Issue와 충돌하면 덮어쓰지 않고 중단한다.
4. 보호 브랜치에 직접 Commit·Push하지 않는다.

### 6.3 Contract Traceability Map

| ID | 최종 계약 | 예상 구현 위치 | 필요한 증거 |
|---|---|---|---|
| C-01 | 구체적인 결과 또는 구현 제약 | 클래스·메서드·Query·설정 | 테스트·직접 검증·문서 |

다음은 별도 항목으로 분리한다.

- API·요청·응답 계약
- 도메인 상태와 데이터 원본
- 기간·시간대·정렬·동률·빈 결과 정책
- 트랜잭션·Rollback·권한·동시성 규칙
- Human이 확정한 구현 방식
- 도입 금지 기술과 제외 범위
- 테스트·직접 검증 시나리오

### 6.4 Plan

- 구현 범위
- 수정 예상 파일
- 구현 순서
- 테스트와 직접 검증 방법
- 제외 범위
- 문서 영향

`READY`가 구현 승인이다. 추가 승인을 기다리지 않지만 계약 재결정이 필요하면 중단한다.

### 6.5 Implement

- Issue 범위 안에서 최소 변경한다.
- production 코드와 필요한 테스트를 함께 작성한다.
- 기존 API·DB·예외 계약을 유지한다.
- 정상·실패·경계·Rollback 흐름을 고려한다.
- 불필요한 리팩터링과 새 기술 도입을 하지 않는다.
- Markdown 문서는 Issue에서 허용한 경우에만 수정한다.

### 6.6 Troubleshooting Gate

자체 해결 가능:

- 컴파일·import·정적 검사 오류
- 단순 테스트 데이터·Mock 설정 오류
- 기존 패턴으로 해결 가능한 테스트 실패

중단 필요:

- 요구사항·설계·API·DB·트랜잭션·권한 정책 재결정
- 동시성 정합성, 데이터 손실, 중복 처리 위험
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR·계약·현재 코드 구조 충돌
- Issue 범위 밖 또는 보호 파일 변경 필요
- 다른 작업 내용을 덮어쓸 위험

보고 시 확인된 사실, 추정, 시도 결과, 영향 범위, 선택지, 필요한 Human 결정을 구분한다.

### 6.7 Verify

순서:

1. 변경과 직접 관련된 가장 작은 테스트
2. 필요한 통합·권한·Rollback·동시성·성능 테스트
3. 전체 테스트
4. 전체 빌드
5. Diff 검사
6. 필요한 API·DB·로그 직접 검증

```bash
./gradlew test
./gradlew build
git diff --check
git status
git diff --stat
git diff
```

실행한 명령, 보장 범위, 미검증 범위, 직접 검증, 알려진 제한을 구분한다.

### 6.8 Review Diff와 Contract Traceability

Commit 전에 실제 Diff를 다시 읽는다.

- Issue 완료 조건 충족
- 범위 이탈과 불필요한 리팩터링
- API·ERD·ADR·예외 계약 불일치
- 핵심 버그와 실패 흐름
- 테스트 누락과 의미 없는 테스트
- 실행하지 않은 검증의 허위 기록
- 필요한 문서 보완

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 구체적인 계약 | `path#symbol` | `TestClass#method` 또는 미검증 사유 | `PASS | HOLD | N/A` |

한 행이라도 `HOLD`이면 Draft PR 단계로 넘어가지 않는다.

### 6.9 PR Template Fidelity와 질문 생성

최신 `.github/PULL_REQUEST_TEMPLATE.md`를 다시 읽고 다음을 확인한다.

1. 모든 필수 제목 보존
2. Contract Traceability 실제 증거 작성
3. Codex가 완료한 항목만 체크
4. Human·ChatGPT·CI 단계 선행 체크 금지
5. 실제 Diff 기반 질문 작성
6. Human 답변은 비워 둠
7. `docs/15_DIFF_QUESTION_POLICY.md` 준수

Codex는 질문만 작성한다. Human 답변의 정답, 평가, 보완 설명, 중대한 오해 판정란을 만들지 않는다.

### 6.10 Commit·Push·Draft PR

Contract Traceability 전 항목 PASS와 검증이 끝나면:

1. Issue 범위 변경만 Commit
2. 작업 브랜치 Push
3. Draft PR 생성
4. PR 본문 재확인
5. PR 번호와 미검증 범위 전달
6. Human 이해도 작성 단계로 인계

Draft PR 생성 후 Codex의 최초 구현 단계는 종료된다.

## 7. Draft PR 이후 Codex 금지 역할

Codex는 다음을 수행하지 않는다.

- Human 답변의 정확도·이해도 평가
- Human 답변에 대한 보완 답변·모범 답안 작성
- Human 답변을 대신 작성하거나 재작성 요구
- 중대한 오해 여부의 최종 판정
- ChatGPT 리뷰 댓글의 임의 선택·반영
- 리뷰 완료 또는 Merge 가능 상태 선언
- Merge

Human 답변과 PR 전체의 검증은 ChatGPT가 담당하고, 반영 범위는 Human이 결정한다.

## 8. Human이 승인한 리뷰 수정

Codex는 Human이 반영 범위를 명시한 뒤에만 다시 작업한다.

필수 입력:

- 대상 PR과 최신 Head
- ChatGPT 리뷰 댓글 또는 Human이 정리한 항목
- `반영 | 미반영 | 후속 Issue` 결정
- 수정 허용 범위

실행 절차:

```text
Read latest Head and approved review scope
→ Map approved items to files and tests
→ Modify only approved items
→ Verify
→ Review Diff
→ Update Contract evidence if needed
→ Commit / Push
→ Hand off to ChatGPT latest-Head review
```

규칙:

- 승인되지 않은 리뷰 항목은 수정하지 않는다.
- 설계 재결정이 필요하면 Troubleshooting Gate로 전환한다.
- Human 답변 문구는 수정하지 않는다.
- 수정 결과와 검증 증거만 PR 본문에 갱신한다.
- ChatGPT 재검토와 Human Merge 항목을 선행 체크하지 않는다.

## 9. 리뷰와 Merge 경계

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

Human은 다음을 최신 Head 기준으로 확인한다.

- CI 성공
- 문서 충돌과 범위 밖 변경 없음
- Contract Traceability 전체 PASS
- ChatGPT 최신 Head 재검토 완료
- 미해결 리뷰와 설계 충돌 없음
- 최종 Diff 확인
