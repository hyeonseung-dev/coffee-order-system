# Team OASIS AI 활용 워크플로우

## 1. 목적

최종 프로젝트에서 AI를 많이 사용하는 것 자체는 성과가 아니다.

이 워크플로우의 목적은 다음과 같다.

- 팀원 모두가 자신이 구현한 코드와 테스트를 설명할 수 있게 한다.
- AI가 잘못된 코드를 만들더라도 사람, 계약 추적, 테스트, 리뷰가 걸러낼 수 있게 한다.
- 도메인 설계, 트랜잭션, 데이터 정합성, 테스트, 성능 개선을 프로젝트의 중심에 둔다.
- 반복 지시와 전달 비용은 줄이되 중요한 Human 판단은 유지한다.
- 구현 전 설계 이해와 구현 후 실제 코드 이해를 각각 검증한다.
- 상세 요구사항은 Issue와 문서에 한 번만 기록하고 실행 프롬프트는 Issue 번호 한 문장으로 유지한다.

성과 비중은 다음을 지향한다.

```text
백엔드 문제 해결 70%
테스트·협업·문서화 20%
AI 활용과 통제 방식 10%
```

AI 하네스는 프로젝트의 주인공이 아니라 백엔드 결과물을 더 안전하고 일관되게 만들기 위한 협업 방식이다.

## 2. 기본 역할

### Human

- 서비스 목적과 우선순위 결정
- 요구사항과 도메인 규칙 해석
- API, ERD, 트랜잭션, 권한, 상태 전이 설계 승인
- Issue 착수 전 문제·범위·흐름·실패 시나리오·검증 계획 검토
- 위험도와 리뷰 수준 최종 결정
- 중요한 기술 선택과 ADR 승인
- `DRAFT → READY`와 구현 시작 승인
- 실제 PR Diff를 보며 이해도 질문에 직접 답변
- Troubleshooting Gate 선택지 결정
- 최종 리뷰와 Merge 직접 수행
- 결과 책임

### ChatGPT

- 프로젝트 문서와 코드에 근거한 상세 Draft Issue 작성
- Human 착수 검토를 문서·코드와 비교해 누락·충돌·위험·대안 제시
- Human 결정을 반영한 Issue, ADR, Markdown 정리
- PR의 실제 Diff, 커밋, Actions, 테스트 증거를 독립적으로 검토
- Codex가 만든 Contract Traceability와 이해도 질문의 품질 검토
- Human 답변과 실제 코드·테스트의 일치 여부를 필요 시 독립 검증
- 중요한 충돌을 발견하면 임의로 맞추지 않고 Human에게 보고

### Codex

- 승인된 `READY` Issue 구현
- 작업 브랜치 자동 생성
- 최종 계약을 원자 항목으로 분해한 Contract Traceability 작성
- 코드와 테스트 작성
- 로컬 실행, 디버깅, 테스트, 빌드, Diff 검증
- Issue 범위 안의 일반 오류 자체 해결
- 실제 Diff 자체 리뷰와 계약별 코드·테스트 증거 연결
- 베이스 브랜치 PR 템플릿 전체 보존
- 실제 Diff 기반 이해도 질문 생성
- Human 답변을 최신 Diff와 대조해 `PASS | HOLD` 검증
- Commit, 작업 브랜치 Push, Draft PR 생성
- 승인된 리뷰 수정 사항을 같은 PR에 반영
- 설계·정합성·보안·성능 재결정이 필요하면 중단하고 보고
- Human 답변 작성과 Merge는 수행하지 않음

### GitHub

- Issue로 요구사항, 완료 조건, 상태 관리
- API·ERD·ADR·문서로 설계 근거 보존
- Branch와 Commit으로 구현 이력 관리
- Draft PR로 구현 결과 인계
- Actions로 테스트와 빌드 결과 제공
- PR에 계약 추적, 실제 Diff 기반 질문, Human 답변, 검증 결과 보존
- 리뷰와 Human Merge 이력 보존

## 3. 프로젝트 시작 기준선

프로젝트 시작 시 다음 기준선을 준비한다.

- `AGENTS.md`
- 프로젝트 목표와 요구사항 문서
- API 명세
- ERD와 Migration 기준
- `EVIDENCE_GUIDE`
- ADR 기준과 템플릿
- Issue / PR / Code Review 템플릿
- Codex Issue Loop Skill
- Contract Traceability와 Diff 기반 이해도 Gate
- 공통 테스트·빌드 검증 명령
- GitHub Actions
- 보호 브랜치 직접 Push를 막는 최소 Hook
- Human-only Merge 정책

세부 실행 기준:

- `.codex/skills/coffee-order-issue-loop/SKILL.md`
- `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md`

## 4. 전체 Workflow

```text
프로젝트 기준선 준비
→ 전체 Draft Issue 초안 생성
→ 팀 백로그 검토
→ 담당자 착수 검토
→ AI 재검증 1차
→ Human·AI 쟁점 협의와 최종 합의
→ Human 승인과 READY 전환
→ Codex가 권위 있는 최종 계약 추출
→ 작업 브랜치 자동 생성
→ Contract Traceability 작성
→ 구현·테스트·검증·Diff 리뷰
→ 계약별 코드·테스트 증거 연결
→ 베이스 브랜치 PR 템플릿 재확인
→ 실제 Diff 기반 이해도 질문 생성
→ Commit / Push / Draft PR
→ 위험도별 코드 리뷰와 CI
→ Human 직접 답변
→ Codex 답변 검증 PASS/HOLD
→ 필요 시 ChatGPT 독립 검증
→ Human Merge
→ 배포·통합 확인과 필요한 문서 갱신
```

## 5. Issue Refinement

### 5.1 AI 상세 Draft Issue

AI는 프로젝트 문서, 현재 코드, 선행 Issue를 읽고 다음을 먼저 작성한다.

- 작업 목적과 현재 필요한 이유
- 현재 코드와 문서에서 확인한 사실
- 권장 API와 도메인 정책
- 예상 코드·데이터 흐름
- 고려한 대안과 트레이드오프
- 정상·실패·경계 시나리오
- 테스트 및 직접 검증 초안
- 포함·제외 범위
- Human이 결정해야 하는 항목

AI는 선택지를 단순 나열하지 않고 권장안과 근거를 제시한다. 확정되지 않은 내용은 `AI 권장안` 또는 `Human 결정 필요`로 표시한다.

### 5.2 Human 담당자 착수 검토

담당자는 AI Draft를 그대로 승인하지 않고 자신의 언어로 다음을 검토한다.

- 내가 이해한 문제
- 왜 지금 필요한지
- 포함 범위와 제외 범위
- 핵심 도메인 규칙
- 예상 코드·데이터 흐름
- 주요 정상·실패·경계 시나리오
- 대안과 트레이드오프
- 테스트·직접 검증 계획
- 예상 트러블슈팅
- 아직 이해되지 않는 부분

Human 최초 작성 영역은 학습 증거로 보존하며 AI가 덮어쓰지 않는다.

### 5.3 AI 재검증과 최종 합의

ChatGPT는 Human 작성 내용을 프로젝트 docs, API, ERD, ADR, 현재 코드와 비교한다.

- 범위가 과하거나 모호하지 않은가
- 도메인 규칙과 실패 시나리오가 누락되지 않았는가
- API·ERD·Migration과 충돌하지 않는가
- 위험도가 적절한가
- 테스트가 실제 규칙을 증명하는가
- 불필요한 기술이나 구조가 들어가지 않았는가
- 중요한 설계 선택에 ADR이 필요한가

공식 AI 재검증은 한 번만 기록한다. 이후 쟁점은 대화로 협의하고 다음을 최종 계약으로 정리한다.

```text
AI 최초 제안
→ Human 의견
→ 최종 합의
→ 선택 근거
```

### 5.4 READY 승인

다음 조건을 통과해야 Issue가 `READY`가 된다.

- 담당자가 Issue를 자신의 언어로 해석함
- 문제, 규칙, 흐름, 실패 시나리오, 테스트·검증을 검토함
- AI 재검증 후 Human·AI 최종 합의가 기록됨
- 최종 구현 계약이 명확함
- 완료 조건이 테스트하거나 검증 가능함
- 제외 범위와 수정 경계가 명확함
- 선행 Issue가 완료됨
- API·ERD·설정 영향이 확인됨
- 위험도와 리뷰 수준이 결정됨
- 필요한 ADR이 Accepted 상태임
- Human이 구현 시작을 승인함

`READY`는 구현 승인이다. Codex는 계획 보고나 브랜치 생성을 이유로 추가 승인을 요청하지 않는다.

READY 이후 설계가 바뀌면 Codex가 임의 반영하지 않고 Issue를 다시 Refinement한다.

## 6. Codex 구현과 Contract Traceability

### 6.1 권위 있는 계약 영역

Codex는 Issue 전체를 읽되 다음 영역을 구현 기준으로 사용한다.

1. Human·AI 최종 합의
2. 최종 구현 계약
3. 완료 조건
4. 테스트·직접 검증 계획
5. 포함·제외 범위
6. 수정 허용·금지 범위
7. 위험도와 승인 상태

초기 제안과 최초 이해 기록은 배경이다. 최종 계약과 충돌하면 최종 계약을 따른다.

### 6.2 Contract Traceability

구현 전:

| ID | 최종 계약 | 예상 구현 위치 | 필요한 증거 |
|---|---|---|---|
| C-01 | 요구사항 또는 구현 제약 | 클래스·메서드·Query·설정 | 테스트·직접 검증·문서 |

구현 후:

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 계약 | `path#symbol` | `TestClass#method` 또는 사유 | `PASS | HOLD | N/A` |

규칙:

- 결과 요구사항과 구현 방식 제약을 모두 추적한다.
- 테스트가 필요한 항목에는 테스트 또는 직접 검증 증거가 있어야 한다.
- `N/A`에는 사유가 필요하다.
- 한 항목이라도 `HOLD`이면 완료 보고, Commit, Draft PR을 진행하지 않는다.
- 테스트 통과는 계약 전체 PASS를 대신하지 않는다.

### 6.3 Codex 실행 순서

```text
Read authoritative contract
→ Create Branch
→ Build Contract Map
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Link Evidence
→ Fix
→ Re-verify
→ Verify PR Template
→ Generate Actual-Diff Questions
→ Report
→ Commit / Push / Draft PR
```

상세 절차는 저장소 Skill을 따른다.

## 7. Troubleshooting Gate

다음 상황에서는 Codex가 임의 진행하지 않는다.

- 권위 있는 최종 계약 영역끼리 충돌함
- 문서와 실제 코드가 충돌함
- Issue 범위를 확장해야 함
- API·ERD·DB 스키마 변경 필요
- 인증·권한·트랜잭션 설계 재결정 필요
- 동시성 정합성, 데이터 손실, 중복 처리 위험 발견
- 테스트를 삭제·비활성화·약화해야 함
- 성능 목표 미달 또는 운영 장애 가능성 발견
- ADR과 구현이 충돌함
- 임시 우회와 근본 해결 중 선택 필요

```text
Codex 중단
→ 확인된 사실과 추정 원인 분리 보고
→ Human + ChatGPT 검토
→ 필요하면 Issue·API·ERD·ADR 수정
→ Human 재승인
```

다음은 중단 사유가 아니다.

- 깨끗한 보호 브랜치에서 Issue 전용 브랜치 생성
- READY Issue의 짧은 계획 보고
- `gh` CLI 없이 표준 `git`으로 Commit·Push

## 8. Draft PR과 Template Fidelity

Draft PR 생성 직전에 Codex는 베이스 브랜치의 최신 `.github/PULL_REQUEST_TEMPLATE.md`를 다시 읽는다.

- 모든 `##`·`###` 제목을 보존한다.
- 빈 섹션을 자체 요약으로 대체하거나 삭제하지 않는다.
- Contract Traceability 표를 실제 증거로 채운다.
- Codex가 직접 완료한 항목만 체크한다.
- Human·ChatGPT·독립 리뷰·CI·이해도 Gate는 실제 완료 전에 체크하지 않는다.
- 실제 Diff 기반 이해도 질문을 작성한다.
- PR 생성 후 본문을 다시 읽어 누락과 선행 체크를 확인한다.

`gh` CLI 부재는 Commit·Push 중단 사유가 아니다.

## 9. 위험도별 리뷰

### LOW

- 관련 테스트와 CI
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 질문 3개
- 작성자 Human 리뷰

### MEDIUM

- 정상·실패·경계 테스트
- Contract Traceability
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 질문 5개
- 필요 시 독립 AI 또는 다른 팀원 리뷰
- 작성자 Human 리뷰
- ChatGPT 문맥 리뷰
- CI

### HIGH

- 구현 전 설계와 ADR
- 권한·Rollback·동시성·정합성 테스트
- Contract Traceability
- 실제 Diff 기반 질문 5~8개
- 독립 AI 리뷰
- 강화된 Human 리뷰
- ChatGPT 문맥 리뷰
- CI

모든 PR에 동일한 다중 리뷰를 강제하지 않는다. 위험도와 학습 가치에 따라 검증 비용을 조정한다.

## 10. 구현 후 이해도 검증 Gate

### 10.1 질문 생성

Codex는 구현·테스트·자체 Diff 리뷰가 끝난 최신 상태를 읽고 질문을 작성한다.

- 고정된 일반 질문을 그대로 복사하지 않는다.
- 질문마다 실제 파일, 클래스, 메서드, Query, 설정 또는 테스트를 명시한다.
- Human 답변을 대신 작성하지 않는다.

질문 관점:

- 해결한 문제와 요청→응답 흐름
- 핵심 도메인 규칙의 구현 위치와 계층 선택 이유
- 정상·실패 시 데이터 상태와 트랜잭션·Rollback
- 위험한 경계·동시성·권한·시간·캐시 조건
- 테스트 보장 범위와 미검증 범위
- 선택한 구현과 제외한 대안의 트레이드오프
- 운영 환경의 잔여 위험

### 10.2 Human 답변

담당 Human은 리뷰 반영과 최신 CI 확인 후 실제 PR Diff와 테스트를 보며 자신의 언어로 답변한다.

이 Gate는 폐쇄형 시험이나 암기 검사가 아니다. 코드와 문서를 참고할 수 있다.

### 10.3 Codex 검증

Codex는 최신 Head의 Diff와 테스트를 다시 읽고 문항별로 기록한다.

- `PASS | HOLD`
- 실제 코드와 일치하는 근거
- 수정이 필요한 오해
- 미검증·잔여 위험

다음 상황에서는 전체 결과가 `HOLD`다.

- 질문이 실제 Diff가 아닌 일반 문장에 머묾
- 실제 코드와 다른 흐름을 설명함
- 핵심 규칙의 구현 위치나 선택 이유를 설명하지 못함
- 실패 시 데이터 상태와 트랜잭션 경계를 이해하지 못함
- 테스트가 보장하지 않는 범위를 보장한다고 판단함
- 중요한 경계 조건이나 잔여 위험을 인지하지 못함

MEDIUM·HIGH에서는 ChatGPT가 코드 리뷰 또는 이해도 결과를 독립적으로 확인할 수 있다.

Codex PASS는 Merge 권한이 아니다.

## 11. Merge

Merge 전 Human은 다음을 확인한다.

- Issue 완료 조건 충족
- Contract Traceability 전 항목 PASS
- 최신 Commit 기준 GitHub Actions 성공
- 위험도에 맞는 리뷰 완료
- 수정 요청 반영 또는 미반영 사유 기록
- 문서와 코드 충돌 없음
- 범위 밖 변경 없음
- 테스트 삭제·약화 없음
- 미검증 항목과 알려진 제한 확인
- 구현 후 이해도 검증 PASS

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

## 12. Merge 이후

필요한 항목만 수행한다.

- 배포·통합 환경 확인
- API 변경 시 API 명세 갱신
- DB 구조 변경 시 ERD·Migration 문서 갱신
- 실제로 발생한 의미 있는 문제는 트러블슈팅 기록
- 성능 개선은 적용 전후 결과 기록
- 중요한 설계 결정은 ADR 기록
- 오래되거나 충돌하는 AI 지침 정리

모든 PR마다 모든 문서와 AI 로그를 갱신하지 않는다. 반복 실패나 워크플로우 구조 문제가 확인될 때만 하네스 문서를 수정한다.

## 13. 적용하지 않을 자동화

- Issue부터 Merge까지 완전 자동화
- 모든 Issue의 다중 Subagent 분리
- 모든 PR의 3중 AI 리뷰
- AI 자동 Merge
- 무제한 자동 수정 반복
- 모든 AI 실행 로그 저장
- 복잡한 Agent Teams와 Worktree 병렬 구현
- AI가 Human 이해도 답변을 작성하는 방식
- 하네스 자체를 프로젝트의 핵심 기술로 발표하는 방식

제외 이유는 4명의 신입 개발자가 5주 동안 유지하기에는 운영 비용이 백엔드 개발 효과보다 커질 수 있기 때문이다.

## 14. AI 레인저스 역할

### AI 작전참모

- `AGENTS.md`와 전체 작업 흐름 관리
- Issue→계약 추적→구현→검증→PR 흐름 점검
- 주요 정책과 Skill 도입 여부 판단

### AI 환각 검수원

- 존재하지 않는 코드·설정·테스트 결과 주장 확인
- 실행하지 않은 테스트 성공 보고 검수
- Issue, 계약 추적표, API, ERD, 코드, 증거 일치 확인
- 이해도 답변과 실제 Diff 불일치 확인

### AI 회유 담당

- Issue 범위 밖 리팩터링과 과잉 구현 차단
- 불필요한 기술과 갑작스러운 구조 변경 통제
- 보호 브랜치 직접 작업, 자동 Merge, 위험 Git 명령 방지

### AI 재교육 담당

- 반복 실패 원인 분석
- 프롬프트 추가보다 AGENTS, Skill, 템플릿, Gate 개선 우선
- 하네스 드라이런과 오래된 지침 정리
- 반복해서 설명하지 못하는 영역을 학습 항목으로 전환
