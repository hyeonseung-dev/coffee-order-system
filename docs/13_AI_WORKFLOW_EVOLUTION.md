# AI-assisted Workflow Evolution

## 1. 문서 목적

이 문서는 개인 프로젝트에서 AI-assisted 개발 방식을 실제로 사용하면서 발견한 실패와 개선 과정을 기록한다.

최종 규칙만 제시하지 않고 다음을 보존한다.

- 왜 처음 방식이 비효율적이거나 위험했는가?
- Human과 AI의 역할을 어떻게 다시 분리했는가?
- 프롬프트를 늘리는 대신 어떤 실행 Gate를 추가했는가?
- 어떤 자동화를 제거하거나 축소했는가?
- 포트폴리오에서 무엇을 문제 해결 경험으로 설명할 수 있는가?

구체적인 PR 사건은 [AI Review Log](10_AI_REVIEW_LOG.md)와 `docs/reviews/`를 따르고, 현재 운영 규칙은 [AI Workflow](05_AI_WORKFLOW.md)를 따른다.

## 2. 1단계 — 빈 Issue 템플릿 중심

### 시작 방식

```text
AI가 빈 양식 중심 Draft Issue 작성
→ Human이 설계를 대부분 처음부터 작성
→ AI 재검증
→ Human 승인
→ Codex 구현
```

### 문제

- Issue 작성 시간이 구현 시간보다 길어짐
- 학습 검증보다 빈칸 채우기가 됨
- 배우지 않은 세부 구현을 Human이 추측해야 함
- AI의 코드·문서 분석과 대안 제시 능력을 활용하지 못함

### 개선

AI가 현재 코드와 문서를 읽고 다음을 포함한 상세 Draft를 먼저 작성했다.

- 권장안과 근거
- 대안과 트레이드오프
- 정상·실패·경계 시나리오
- 테스트·직접 검증 초안
- 포함·제외 범위
- Human 결정 항목

## 3. 2단계 — AI가 Human 판단까지 대신한 문제

Issue #9 착수 과정에서는 반대 문제가 발생했다. Human 작성이 끝나지 않았는데 AI가 정책을 확정하고 Issue를 `READY`로 전환했다.

### 위험

- 도메인 정책 결정 대체
- 테스트 기준 확정 대체
- 구현 시작 승인 대체
- Human-in-the-loop의 형식화

### 개선

```text
AI 상세 Draft
→ Human 최초 이해 원문 보존
→ AI 재검증 별도 기록
→ Human·AI 쟁점 협의
→ 최종 계약 정리
→ Human만 READY 승인
```

AI가 상세한 선택지를 주는 것과 Human 결정을 대신하는 것을 분리했다.

## 4. 3단계 — 재검증 반복 과도화

### 문제

`AI 재검증 1차 → 2차 → 3차`를 반복하면 구현보다 문서 절차가 프로젝트 중심이 된다.

### 개선

- 공식 재검증 기록은 1회
- 이후 쟁점은 대화로 협의
- 마지막에 최종 합의와 근거를 1회 정리

```text
AI 최초 제안
→ Human 의견
→ 최종 합의
→ 선택 근거
```

## 5. 4단계 — 구현 전 이해와 실제 코드 이해 분리

Issue Refinement가 상세해도 구현 전에는 예상 흐름만 검토한다. 실제 구현에서 다음이 구체화되거나 달라진다.

- 클래스·메서드
- Repository Query
- 트랜잭션 경계
- Fixture와 예외 처리
- 리뷰 반영 후 최종 Diff

따라서 구현 전 Human 검토와 구현 후 실제 Diff 이해도 기록을 분리했다.

## 6. 5단계 — READY 이후 추가 승인을 다시 요구한 문제

### 문제

Human이 `READY`를 승인했는데도 Codex가 MEDIUM 위험도라는 이유로 계획과 브랜치 생성을 다시 승인받으려 했다.

### 원인

- `READY = 구현 승인`과 `MEDIUM = 계획 후 확인` 규칙 충돌
- 보호 브랜치에서 작업 브랜치를 자동 생성하는 규칙 부족

### 개선

```text
READY = 구현 시작 승인 완료
```

- READY 이후 위험도와 관계없이 추가 계획 승인 없음
- 깨끗한 보호 브랜치에서는 Issue 전용 브랜치 생성
- 미커밋 변경·계약 충돌·설계 재결정 때만 중단

## 7. 6단계 — 특정 도구 부재를 전체 중단으로 해석

### 문제

Codex가 `gh` CLI 부재를 Commit·Push·Draft PR 전체 불가로 판단했다.

### 개선

- 로컬 Commit·Push 가능 여부와 PR 생성 수단을 분리
- Draft PR은 GitHub Connector·API·`gh` 중 사용 가능한 수단 선택
- 수행 불가능한 단계만 BLOCKED
- 도구 부재를 실행한 검증으로 꾸미지 않음

## 8. 7단계 — 상세 Issue가 있어도 계약을 누락한 PR #32

Issue #9는 상세한 최종 계약이 있었지만 다음 누락이 PR 리뷰에서 발견됐다.

- `Asia/Seoul` 고정 대신 `Clock.systemDefaultZone()` 사용
- SQL·`JdbcTemplate` 대신 Reflection Fixture 사용
- TOP 3 초과·미만, ACTIVE 0개, 빈 배열 테스트 일부 누락
- PR 템플릿 축약
- 수행 전 Human·ChatGPT 리뷰 완료 체크
- 실제 Diff 기반 질문 누락

### 원인

1. Issue에 최초 제안·Human 원문·최종 합의가 함께 있어 권위 있는 영역 추출이 약했다.
2. 자체 리뷰가 `완료 조건 충족` 한 줄이었고 계약별 증거 연결이 없었다.
3. 작성되지 않은 테스트는 실패할 수 없었다.
4. 테스트 Double 결과만으로 운영 Bean·설정을 검증했다고 오판했다.
5. PR 템플릿 제목 보존과 외부 단계 선행 체크 금지가 강제되지 않았다.

### 개선

- Contract Traceability Gate
- PR Template Fidelity Gate
- 운영 Bean·실제 DB·실제 Redis 경로를 위험에 따라 추가 검증
- 실제 Diff 이후 질문 생성

## 9. 8단계 — Contract Traceability Gate

Issue의 최종 계약을 원자 항목으로 분해하고 실제 코드·테스트 증거를 연결한다.

| ID | 최종 계약 | 실제 코드·문서 증거 | 테스트·직접 검증 | 상태 |
|---|---|---|---|---|
| C-01 | 구체 결과 또는 제약 | `path#symbol` | `TestClass#method` 또는 직접 증거 | `PASS | HOLD | N/A` |

### 효과

- 긴 Issue에서 권위 있는 최종 계약만 추출
- 시간대·정렬·Fixture·실패 정책 같은 비기능 제약 누락 방지
- 테스트 통과와 계약 전체 충족 구분
- `HOLD`가 있으면 완료·Draft PR 중단

## 10. 9단계 — PR Template Fidelity Gate

### 문제

Codex가 긴 템플릿 대신 자체 요약 PR을 만들거나 실행 전 외부 리뷰·CI를 완료로 표시했다.

### 개선

- PR 생성 직전 base branch 최신 템플릿 읽기
- 필수 `##`·`###` 제목 보존
- 빈 섹션을 자체 요약으로 대체하지 않기
- 실행하지 않은 테스트·Human·ChatGPT·CI 완료 체크 금지
- PR 생성 뒤 본문 재검증
- 실제 Diff 기반 코드 읽기 순서와 질문 작성

## 11. 10단계 — 상시 멀티에이전트·Hook 과도화

### 시도

Planner·Implementer·Reviewer 분리, 여러 Hook, 로그·검증 Gate를 계속 추가했다.

### 확인한 문제

- 작은 개인과제보다 하네스 유지비가 커짐
- 역할 사이 문맥 전달 비용 증가
- 로그와 체크리스트가 백엔드 학습 시간을 침범
- 자동화 자체가 포트폴리오 주인공이 될 위험

### 개선

```text
하나의 READY Issue
→ 단일 Codex가 계획·구현·검증·Draft PR
→ 위험도에 따라 Human·ChatGPT·비담당 리뷰어 추가
```

상시 분리 대신 실제 위험이 높은 변경에만 독립 검증을 추가했다.

## 12. 11단계 — Codex가 Human 답변을 평가한 역할 충돌

### 기존 흐름

```text
Codex가 실제 Diff 질문 생성
→ Human 답변
→ Codex가 최신 Head와 답변을 대조해 PASS/HOLD
→ 필요 시 보완 답안 작성
```

### 문제

- 구현자가 자신의 결과와 Human 이해도를 동시에 평가해 독립성이 약함
- Codex가 질문·채점·보완 답안을 모두 맡으면 Human 이해도 기록의 의미가 줄어듦
- 코드 리뷰와 학습 평가가 혼합됨
- Human이 리뷰 반영 범위를 정하기 전에 AI가 다음 절차를 결정할 수 있음

### 최종 개선

```text
Codex: 실제 Diff 기반 질문만 생성
Human: 자신의 말로 답변
ChatGPT: Human 답변을 포함한 PR 전체 검증·리뷰 댓글
Human: 항목별 반영·미반영·후속 Issue 결정
Codex: 승인된 리뷰만 수정
ChatGPT: 최신 Head 재검토
Human: Merge
```

Codex는 Human 답변을 평가·보완·대신 작성하지 않는다.

## 13. 프로젝트 구현에서 실제로 발견한 AI 누락

### 후속 처리 학습 빌드업 누락

처음부터 Spring Event·AFTER_COMMIT을 완료 조건으로 넣어 동기 외부 호출의 지연·실패를 재현하지 않았다. Human이 문제를 지적해 동기 → Event → AFTER_COMMIT → Async → Outbox 순으로 개선 이유를 확인했다.

### 시간대 불일치

주문 시각과 인기 메뉴 업무 날짜가 서로 다른 기준을 사용했다. UTC `Instant` 저장과 KST 업무 날짜 계산, 고정 Clock 테스트로 통일했다.

### 인덱스 전제 오류 가능성

예상 SQL의 `(ordered_at, menu_id)`를 바로 적용하지 않고 실제 ACTIVE 메뉴 LEFT JOIN과 50만 건 실행계획을 기준으로 후보를 재선택했다.

### K6 무효 측정

Codex의 Docker K6 시도에서 Spring Boot 프로세스가 유지되지 않아 요청이 유효하게 수행되지 않았다. 해당 결과를 제외하고 Human 로컬 측정만 최종 수치에 사용했다.

### Outbox `eventId` 전달 누락

Entity와 Payload에 `eventId`를 추가했지만 외부 Client 호출까지 전달되지 않은 누락을 Human이 발견했다. 필드 존재가 아니라 DB 저장→역직렬화→Client→재시도 동일성까지 end-to-end로 검증하도록 수정했다.

### 역할·문서 충돌

PR 템플릿에서는 Codex 답변 평가 역할을 제거했지만 기존 Workflow Evolution 문서에는 PASS/HOLD가 남았다. Issue #59 최종 문서 정리에서 정본 문서와 README·SA를 같은 역할 경계로 수정했다.

## 14. 최종 운영 흐름

```text
1. ChatGPT가 코드·문서 기반 상세 Draft Issue 작성
2. Human 담당자 착수 검토
3. ChatGPT 재검증 1회
4. Human·AI 쟁점 협의
5. 최종 계약 정리
6. Human READY 승인
7. Codex가 권위 있는 최종 계약 추출
8. 작업 브랜치 생성
9. Contract Traceability 작성
10. 구현·테스트·직접 검증·Diff 리뷰
11. 계약별 코드·테스트 증거 연결
12. PR Template Fidelity 확인
13. 실제 Diff 기반 질문 생성
14. Commit·Push·Draft PR
15. Human 이해도 답변 작성
16. ChatGPT PR 전체 검증 및 리뷰 댓글
17. Human 리뷰 반영 범위 결정
18. Codex 승인된 리뷰 수정·재검증
19. ChatGPT 최신 Head·CI 재검토
20. Human Merge
```

## 15. 최종 핵심 원칙

```text
AI는 판단 가능한 상세 선택지와 증거 구조를 제공한다.
Human은 정책·트레이드오프·반영 범위를 결정한다.
READY는 구현 시작 승인이다.
Codex는 승인된 계약을 구현하고 Draft PR까지 인계한다.
Codex는 Human 답변을 평가하거나 대신 쓰지 않는다.
ChatGPT는 PR 전체와 수정 후 최신 Head를 독립적으로 검증한다.
테스트 통과와 계약 전체 충족·운영 보장을 구분한다.
Merge와 결과 책임은 Human에게 있다.
```

프롬프트를 계속 길게 만드는 대신 저장소 계약과 실행 Gate를 강화하고, 효과가 낮은 자동화는 제거한다.

## 16. 포트폴리오 활용 문장

> AI가 만든 빈 Issue 때문에 개발자가 설계를 처음부터 작성해야 하는 문제와, 반대로 AI가 Human 검토·승인까지 대신하는 문제를 모두 경험했습니다. 이를 개선해 AI는 현재 코드와 문서를 근거로 상세 설계안과 대안을 제시하고, 개발자는 정책과 트레이드오프를 자신의 언어로 검토한 뒤 최종 계약을 승인하도록 역할을 분리했습니다. 또한 상세 Issue가 있어도 구현이 시간대·Fixture·테스트·PR 템플릿 계약을 놓친 사례를 확인해 Contract Traceability와 PR Template Fidelity Gate를 도입했습니다. 마지막으로 Codex가 Human 답변까지 평가하던 역할 충돌을 제거하고, Human 답변을 포함한 PR 전체 검증은 ChatGPT, 반영 범위와 Merge는 Human이 담당하도록 독립성을 높였습니다. 상시 멀티에이전트와 과도한 Hook은 실제 백엔드 작업 시간을 침범해 최소 하네스로 축소했습니다.

## 17. 관련 문서

- [현재 AI Workflow](05_AI_WORKFLOW.md)
- [실제 AI Review Log](10_AI_REVIEW_LOG.md)
- [Evidence Guide](12_EVIDENCE_GUIDE.md)
- [Contract Traceability Gate](14_CODEX_CONTRACT_TRACEABILITY_GATE.md)
- [PR Template](../.github/PULL_REQUEST_TEMPLATE.md)
- [Issue Refinement Template](../.github/ISSUE_TEMPLATE/feature-refinement.md)
- [Codex Issue Loop Skill](../.codex/skills/coffee-order-issue-loop/SKILL.md)
