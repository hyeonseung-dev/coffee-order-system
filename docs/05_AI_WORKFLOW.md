# Team OASIS AI 활용 워크플로우

## 1. 목적

AI를 많이 사용하는 것 자체는 성과가 아니다.

우리의 목표는 다음과 같다.

- 담당자가 자신이 구현한 코드와 테스트를 설명할 수 있어야 한다.
- AI가 잘못된 코드를 만들더라도 Issue, 계약 추적, 테스트, 리뷰가 걸러낼 수 있어야 한다.
- 도메인 설계, 트랜잭션, 데이터 정합성, 테스트, 성능 개선을 프로젝트의 중심에 둔다.
- 반복 구현과 검증은 AI가 돕되 중요한 판단과 최종 책임은 사람이 가진다.
- 자동화 운영 비용이 실제 백엔드 개발 시간을 침범하지 않도록 한다.

성과 비중은 다음을 지향한다.

```text
백엔드 문제 해결 70%
테스트·협업·문서화 20%
AI 활용과 통제 방식 10%
```

AI 하네스는 프로젝트의 주인공이 아니라 백엔드 결과물을 더 안전하고 일관되게 만들기 위한 협업 방식이다.

---

## 2. 현재 운영 결론

현재는 복잡한 Agent Teams나 Planner·Implementer·Reviewer 서브에이전트를 상시 사용하지 않는다.

하나의 Codex가 하나의 `READY` Issue를 기준으로 계획, 구현, 검증, 자체 Diff 리뷰, Commit, Push, Draft PR까지 순차적으로 수행한다.

```text
ChatGPT가 Draft Issue 작성
→ 담당자 Human 착수 검토
→ ChatGPT 재검증
→ Human·AI 최종 합의
→ Human READY 승인

→ 단일 Codex 실행
   Read
   → Minimal Preflight / 작업 브랜치 생성
   → Contract Traceability 작성
   → Plan
   → Implement
   → Verify
   → Review Diff
   → 계약별 증거 연결
   → Fix / Re-verify
   → PR Template Fidelity 확인
   → 실제 Diff 기반 질문 생성
   → Commit / Push / Draft PR

→ 위험도별 리뷰와 CI
→ 작성자·비담당 리뷰어 이해·오해 점검
→ AI의 코드 대조와 보완 설명
→ Human 최종 판단
→ Human 직접 Merge
```

중요한 것은 에이전트 수가 아니라, 각 단계의 완료 조건과 중단 조건을 명확히 하는 것이다.

---

## 3. 역할

### Human

- 서비스 목적과 우선순위 결정
- 요구사항과 도메인 규칙 해석
- API, ERD, 트랜잭션, 권한, 상태 전이 설계 승인
- 담당 Issue의 문제, 범위, 흐름, 실패 시나리오, 검증 계획 직접 검토
- 위험도와 리뷰 수준 최종 결정
- 중요한 기술 선택과 ADR 승인
- `DRAFT → READY` 전환과 구현 시작 승인
- 실제 PR Diff를 읽고 이해도 질문에 직접 답변
- Troubleshooting Gate의 선택지 결정
- 최종 리뷰와 Merge 수행
- 결과 책임

### ChatGPT

- 프로젝트 문서와 현재 코드에 근거한 상세 Draft Issue 작성
- Human 착수 검토를 API, ERD, ADR, 코드와 비교해 재검증
- 누락, 충돌, 위험, 대안과 트레이드오프 제시
- Human 결정을 반영한 Issue와 Markdown 정리
- PR의 실제 Diff, Commit, Actions, 테스트 증거 독립 검토
- Contract Traceability와 이해도 질문의 품질 검토
- 중요한 충돌을 발견하면 임의로 맞추지 않고 Human에게 보고

### Codex

- 승인된 `READY` Issue 구현
- Issue 전용 작업 브랜치 생성
- 권위 있는 최종 계약 추출
- Contract Traceability 작성
- 코드와 테스트 구현
- 로컬 실행, 디버깅, 테스트, 빌드, Diff 검증
- Issue 범위 안의 일반 오류 자체 해결
- 실제 Diff 자체 리뷰와 계약별 증거 연결
- PR 템플릿 전체 보존
- 실제 Diff 기반 이해도 질문 생성
- Commit, Push, Draft PR 생성
- 승인된 리뷰 수정 사항을 같은 PR에 반영
- 설계·정합성·보안·성능 재결정이 필요하면 중단하고 보고
- Human 답변 작성과 Merge는 수행하지 않음

### GitHub

- Issue로 요구사항, 완료 조건, 상태 관리
- API·ERD·ADR·문서로 설계 근거 보존
- Branch와 Commit으로 구현 이력 관리
- Draft PR로 구현 결과 인계
- Actions로 테스트와 빌드 결과 제공
- PR에 계약 추적, 이해도 질문, Human 답변, 리뷰 결과 보존

---

## 4. Issue Refinement

### 4.1 AI Draft Issue

ChatGPT는 프로젝트 문서, 현재 코드, 선행 Issue를 읽고 다음을 작성한다.

- 작업 목적과 지금 필요한 이유
- 현재 문서와 코드에서 확인한 사실
- 권장 API와 도메인 정책
- 예상 코드·데이터 흐름
- 대안과 트레이드오프
- 정상·실패·경계 시나리오
- 테스트와 직접 검증 초안
- 포함·제외 범위
- Human이 결정해야 하는 항목

AI의 제안은 확정 계약이 아니다.

### 4.2 Human 착수 검토

담당자는 AI Draft를 그대로 승인하지 않고 자신의 언어로 다음을 검토한다.

- 내가 이해한 문제
- 왜 지금 필요한지
- 포함 범위와 제외 범위
- 핵심 도메인 규칙
- 예상 코드·데이터 흐름
- 정상·실패·경계 시나리오
- 대안과 트레이드오프
- 테스트·직접 검증 계획
- 예상 트러블슈팅
- 아직 이해되지 않는 부분

Human 최초 작성 영역은 학습 증거로 보존하며 AI가 덮어쓰지 않는다.

### 4.3 AI 재검증과 최종 합의

ChatGPT는 Human 작성 내용을 프로젝트 docs, API, ERD, ADR, 현재 코드와 비교한다.

- 범위가 과하거나 모호하지 않은가
- 도메인 규칙과 실패 시나리오가 누락되지 않았는가
- API·ERD·Migration과 충돌하지 않는가
- 위험도가 적절한가
- 테스트가 실제 규칙을 증명하는가
- 불필요한 기술이나 구조가 들어가지 않았는가
- 중요한 설계 선택에 ADR이 필요한가

최종적으로 다음을 기록한다.

```text
AI 최초 제안
→ Human 의견
→ 최종 합의
→ 선택 근거
```

---

## 5. Issue 상태와 READY 의미

```text
DRAFT
→ READY
→ IN_PROGRESS
→ REVIEW
→ BLOCKED 또는 DONE
```

### DRAFT

전체 개발 지도를 위한 변경 가능한 초안이다. Codex가 구현하지 않는다.

### READY

다음이 이미 완료된 구현 계약이다.

- 담당자 착수 검토
- AI 재검증과 Human·AI 최종 합의
- 최종 구현 계약과 완료 조건
- 포함·제외 범위
- 수정 허용·금지 범위
- 테스트·직접 검증 계획
- 위험도 확정
- 필요한 ADR 승인
- Human 구현 시작 승인

`READY`는 구현 승인이다.

Codex는 짧은 계획과 영향 범위를 보고한 뒤 추가 승인을 기다리지 않고 구현을 계속한다. 계약과 현재 코드가 충돌하거나 설계 재결정이 필요할 때만 `BLOCKED`로 전환한다.

---

## 6. Codex 단일 실행 흐름

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

Codex는 다음을 지킨다.

- 한 번에 하나의 `READY` Issue만 처리한다.
- Issue 범위 안에서 최소 변경한다.
- 불필요한 리팩터링과 새 기술 도입을 하지 않는다.
- 보호 브랜치에 직접 Commit·Push하지 않는다.
- Markdown 문서는 Issue에서 허용한 경우에만 수정한다.
- 실행하지 않은 테스트나 검증을 성공으로 기록하지 않는다.
- 테스트 통과만으로 완료를 선언하지 않는다.

---

## 7. Contract Traceability Gate

Issue의 최종 계약을 원자 항목으로 나누고 실제 코드와 검증 증거를 연결한다.

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 구체적인 요구사항 또는 구현 제약 | `path#symbol` | `TestClass#method` 또는 직접 검증 | `PASS | HOLD | N/A` |

다음도 별도 계약으로 관리한다.

- API 요청과 응답
- 도메인 상태와 데이터 원본
- 시간대, 정렬, 동률, 빈 결과 정책
- 트랜잭션, Rollback, 권한, 동시성 규칙
- Human이 확정한 구현 방식
- 제외 범위와 도입 금지 기술
- 테스트·직접 검증 시나리오

한 항목이라도 `HOLD`이면 완료 보고, Commit, Draft PR 생성을 진행하지 않는다.

```text
테스트 통과
≠ Issue 계약 전체 충족
```

---

## 8. Troubleshooting Gate

### Codex가 자체 해결하는 문제

- 컴파일 오류
- import 누락
- 단순 테스트 데이터 오류
- 명확한 Mock 설정 오류
- 코드 스타일과 정적 검사 오류
- 기존 패턴으로 해결 가능한 국소적 테스트 실패

### 반드시 중단하는 문제

- 요구사항 변경 필요
- API·DB·트랜잭션·권한 정책 재결정 필요
- 동시성 정합성 미보장
- 데이터 손실 또는 중복 가능성
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR과 구현 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 변경 필요
- 보호 파일 변경 필요
- 최종 계약과 현재 코드 구조 충돌

Codex는 다음을 구분해 보고한다.

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 영향 범위
- 선택지와 권장 방향
- 필요한 Human 결정

`BLOCKED`는 자동화 실패가 아니라 AI가 임의 결정하면 안 되는 문제를 발견한 정상적인 결과다.

---

## 9. 위험도별 리뷰

위험도는 에이전트 수가 아니라 테스트 강도, 리뷰 참여, 이해 점검 수준을 조절한다.

### LOW

예: 문서, 오타, 단순 CRUD, 국소 DTO·Validation·테스트 보완, 작은 버그.

- 관련 테스트와 전체 CI
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 질문 3개
- 작성자 답변
- 비담당 리뷰어 1명 확인

### MEDIUM

예: 새로운 API, 서비스 비즈니스 로직, 조회 Query, 인덱스, 캐시, 이벤트, 여러 계층 변경.

- 정상·실패·경계 테스트
- Contract Traceability
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 질문 5개
- 작성자 답변
- 비담당 리뷰어 최소 1명 이해 요약
- 필요 시 ChatGPT 또는 독립 AI 리뷰
- Human 리뷰와 CI

### HIGH

예: 인증·인가, 금액·포인트·재고, 상태 전이, 트랜잭션, 동시성, DB 구조, 메시지 전달 보장, 운영 설정.

- 구현 전 설계, 트레이드오프, ADR, Human 승인 확인
- 권한·Rollback·동시성·정합성 등 위험에 맞는 강화 테스트
- Contract Traceability
- 실제 Diff 기반 질문 5~8개
- 작성자 답변
- 비담당 리뷰어 최소 2명 이해 요약
- 독립 AI 리뷰 필수
- 강화된 Human 리뷰와 CI

HIGH의 독립 AI 리뷰는 상시 Reviewer 서브에이전트를 둔다는 뜻이 아니다. 위험한 PR에만 구현 문맥과 분리된 검토를 추가하는 것이다.

---

## 10. 구현 후 팀 이해·오해 점검

Codex는 최신 실제 Diff와 테스트 결과를 기반으로 질문을 작성한다.

질문은 다음을 확인한다.

- 이 변경이 해결한 문제
- 적용 전과 적용 후 코드·데이터 흐름
- 핵심 도메인 규칙의 구현 위치
- 정상·실패·Rollback 시 데이터 상태
- 동시성·권한·캐시·시간 등 위험 조건
- 테스트가 보장하는 것과 보장하지 않는 것
- 선택한 대안과 트레이드오프
- 현재 구현의 한계

작성자는 실제 Diff를 읽고 자신의 언어로 답한다. 모르는 내용은 모른다고 작성할 수 있다.

AI는 사람의 이해도를 `PASS | HOLD`로 채점하지 않는다.

AI는 다음만 제공한다.

- 실제 코드와 일치하는 부분
- 보완 설명이 필요한 부분
- 미검증 사항과 잔여 위험
- 중대한 오해 후보

중대한 오해 여부와 Merge 보류 여부는 Human 리뷰어가 최종 판단한다.

- LOW·MEDIUM: 중대한 오해가 남아 있을 때만 Merge 보류
- HIGH: 필수 참여자의 이해 점검과 보완 설명 확인이 끝날 때까지 Merge 보류

---

## 11. PR과 Merge 경계

PR은 Codex에서 ChatGPT와 Human으로 넘어가는 공식 인계 지점이다.

PR에는 다음을 기록한다.

- 작업 목적과 Human 최종 결정
- 구현 내용과 주요 변경 파일
- Contract Traceability
- 실행한 명령과 결과
- 테스트가 보장하는 것과 보장하지 않는 것
- 직접 검증과 미검증 항목
- 트러블슈팅과 알려진 제한
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 질문과 Human 답변

Merge는 어떤 경우에도 자동화하지 않는다.

```text
Codex Merge 금지
ChatGPT Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

Merge 전 Human이 직접 확인한다.

- Issue 완료 조건 충족
- Contract Traceability 전 항목 PASS
- 최신 Commit 기준 CI 성공
- 위험도에 맞는 리뷰 완료
- 문서와 코드 충돌 없음
- 범위 밖 변경 없음
- 중대한 오해가 남아 있지 않음
- 미검증 항목과 잔여 위험 확인

---

## 12. 프로젝트에 적용하지 않는 것

- Issue부터 Merge까지 완전 자동화
- Planner·Implementer·Reviewer 상시 Subagent 분리
- 모든 Issue의 멀티에이전트 처리
- 모든 PR의 3중 AI 리뷰
- 무제한 자동 수정 반복
- 모든 AI 대화 로그 저장
- 복잡한 Agent Teams와 Worktree 병렬 구현
- AI 자동 Merge
- 하네스 자체를 프로젝트의 핵심 기술로 발표

필요성이 실제로 확인될 때만 Hook, Skill, 독립 리뷰를 추가한다.

---

## 13. 팀원에게 설명할 핵심

```text
우리는 AI에게 프로젝트를 맡기는 것이 아니라,
Issue 하나를 문제 정의부터 검증까지 끝내는 과정에 AI를 사용한다.

구현 전에는 담당자가 문제, 도메인 규칙, 실패 시나리오와
테스트 계획을 자신의 말로 검토한다.

Human이 READY로 승인한 뒤에는 하나의 Codex가
계획, 구현, 테스트, Diff 자체 리뷰, PR 생성까지 수행한다.

문서 충돌이나 설계 재결정이 필요하면 AI가 멋대로 진행하지 않고 멈춘다.

구현 후에는 작성자와 리뷰어가 실제 Diff를 읽고
코드 흐름과 테스트 범위를 자신의 말로 설명한다.

최종 Merge와 결과 책임은 항상 사람이 가진다.
```

최종 원칙은 다음과 같다.

```text
설계와 위험 판단은 Human
문서와 코드 교차 검증은 ChatGPT
승인된 구현과 반복 검증은 단일 Codex
최종 판단과 Merge는 Human
```
