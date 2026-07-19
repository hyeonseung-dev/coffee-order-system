# Human-in-the-loop AI-assisted 개발 워크플로우

## 1. 목적

AI를 많이 사용하는 것 자체는 프로젝트 성과가 아니다. 이 프로젝트의 AI 워크플로우는 다음 목표를 지원하기 위한 최소 협업 장치다.

- 담당자가 구현한 코드와 테스트를 자신의 말로 설명한다.
- AI가 만든 누락·오류·범위 이탈을 Issue, 테스트, 증거와 리뷰가 걸러낸다.
- 도메인·트랜잭션·정합성·성능·장애 검증이 프로젝트의 중심에 남는다.
- 반복 구현과 문서화는 AI가 돕고 중요한 정책·반영 범위·Merge는 Human이 결정한다.
- 자동화 유지 비용이 실제 백엔드 학습과 구현 시간을 침범하지 않는다.

```text
백엔드 문제 해결 70%
테스트·협업·문서화 20%
AI 활용·통제 방식 10%
```

## 2. 현재 운영 결론

상시 Planner·Implementer·Reviewer 서브에이전트나 완전 자동화 파이프라인을 사용하지 않는다.

하나의 `READY` Issue를 기준으로 단일 Codex가 구현·검증·Draft PR까지 처리하고, 이후 Human과 ChatGPT가 분리된 책임으로 검토한다.

```text
ChatGPT 상세 Draft Issue
→ Human 착수 검토
→ ChatGPT 재검증 1회
→ Human·AI 쟁점 협의와 최종 계약
→ Human READY 승인

→ Codex 구현·검증·Draft PR
→ Human 실제 Diff 이해도 작성
→ ChatGPT PR 전체 검증 및 리뷰 댓글
→ Human 리뷰 반영 범위 결정
→ Codex 승인된 리뷰만 수정·재검증
→ ChatGPT 최신 Head 재검토
→ Human 최종 Merge
```

핵심은 AI 수가 아니라 각 단계의 권위, 완료 조건과 중단 조건이다.

## 3. 역할과 금지 경계

### Human 작성자

책임:

- 서비스 목적·우선순위 결정
- 도메인 규칙, API, ERD, 트랜잭션과 실패 정책 검토
- AI Draft를 자신의 언어로 설명
- 미이해 항목을 그대로 기록
- `READY` 승인
- 구현 후 실제 Diff 기반 질문에 직접 답변

하지 않는 일:

- AI 답변을 복사해 이해도 기록을 대체
- 실행하지 않은 결과 승인

### ChatGPT

책임:

- 현재 docs·코드·Issue·PR에 근거한 상세 Draft와 대안 작성
- Human 착수 검토의 누락·충돌·과도한 설계 재검증
- PR의 실제 Diff, Commit, CI, 테스트 증거와 Human 답변을 포함한 전체 검증
- 코드 문제와 문서·증거 불일치를 리뷰 댓글로 기록
- 승인된 수정 뒤 최신 Head 재검토

하지 않는 일:

- Human 최초 의견 원문 변경
- 리뷰 반영 범위 결정
- 승인되지 않은 코드 수정 지시를 확정 계약으로 처리
- Merge

### Codex

책임:

- 승인된 `READY` Issue의 권위 있는 최종 계약 추출
- 전용 브랜치 생성
- 구현·테스트·직접 검증
- Contract Traceability 작성
- 자체 Diff 리뷰와 PR Template Fidelity 확인
- 실제 Diff 기반 질문 생성
- Commit·Push·Draft PR
- Human이 `반영`으로 승인한 ChatGPT 리뷰 수정
- 수정 후 재검증·Push와 최신 Head 재검토 인계

하지 않는 일:

- Human 착수 검토나 이해도 답변 대신 작성
- Human 답변 평가·채점·보완 답안 작성
- ChatGPT 리뷰를 Human 결정 전에 임의 반영
- 설계 재결정이 필요한 문제를 임의 해결
- Merge

### Human 리뷰어

책임:

- ChatGPT 리뷰 항목별 `반영 | 미반영 | 후속 Issue` 결정
- 미반영 근거와 잔여 위험 확인
- 최신 CI·문서·코드·이해도 결과 최종 판단
- Merge

### 비담당 리뷰어·독립 AI

- 위험도에 따라 추가 관점을 제공한다.
- Human의 최종 판단을 대체하지 않는다.

## 4. Issue Refinement

### 4.1 ChatGPT 상세 Draft

Draft에는 가능한 범위에서 다음을 포함한다.

- 문제와 지금 필요한 이유
- 현재 코드·문서의 확인된 사실
- 권장안과 근거
- 대안과 트레이드오프
- 정상·실패·경계 흐름
- API·ERD·시간·정렬·상태 정책
- 테스트·직접 검증 계획
- 포함·제외 범위
- Human 결정 항목

AI 제안은 확정 계약이 아니다.

### 4.2 Human 착수 검토

착수 검토는 사전 시험이 아니다. 담당자가 구현 전에 다음 핵심을 자신의 말로 확인하는 과정이다.

- 작업 목적
- 핵심 코드·데이터 흐름
- 가장 중요한 실패 위험
- 테스트와 직접 검증 방향
- 대안과 포함·제외 범위
- 미이해·추가 검토 사항

질문 수 기본값:

| 위험도 | 질문 수 |
|---|---:|
| LOW | 3~4 |
| MEDIUM | 4~5 |
| HIGH | 5~6 |

7번째 질문이 필요하면 이유를 기록하고 7개를 초과하지 않는다. 명령어 암기나 같은 주제의 세분화는 구현·PR 단계로 미룬다.

Human 최초 답변은 학습 증거로 보존하고 AI가 덮어쓰지 않는다.

### 4.3 재검증과 최종 합의

ChatGPT는 Human 답변을 docs·코드·API·ERD·ADR과 비교해 다음을 확인한다.

- 범위가 모호하거나 과도하지 않은가?
- 데이터 원본·트랜잭션·Rollback·동시성 규칙이 빠지지 않았는가?
- API·ERD·현재 코드와 충돌하지 않는가?
- 테스트가 실제 계약을 증명하는가?
- 불필요한 기술이 추가되지 않았는가?
- 설계 재결정과 ADR이 필요한가?

공식 재검증 기록은 한 번만 만들고 이후 쟁점은 대화로 협의한다.

```text
AI 최초 제안
→ Human 의견
→ 최종 합의
→ 선택 근거
```

## 5. Issue 상태와 READY 의미

```text
DRAFT → READY → IN_PROGRESS → REVIEW → BLOCKED 또는 DONE
```

### DRAFT

변경 가능한 초안이다. Codex는 구현하지 않는다.

### READY

다음이 완료된 구현 계약이다.

- Human 착수 검토
- ChatGPT 재검증과 쟁점 협의
- 최종 계약·완료 조건
- 포함·제외·변경 허용 범위
- 테스트·직접 검증 계획
- 위험도
- Human 구현 시작 승인

`READY`는 구현 승인이다. Codex는 영향 범위를 보고한 뒤 추가 승인을 기다리지 않고 작업한다.

다음 상황에서만 중단하고 `BLOCKED`를 보고한다.

- 최종 계약과 현재 코드가 충돌
- API·ERD·DB·권한·정합성 재결정 필요
- 보호 파일 변경 승인 없음
- 사용자 미커밋 변경과 작업 범위 충돌
- 필수 도구·권한 부재로 해당 단계 수행 불가

도구 하나가 없다고 수행 가능한 모든 단계를 중단하지 않는다. 예를 들어 `gh` 부재와 git Commit·Push 가능 여부를 분리한다.

## 6. Codex 실행 흐름

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
```

규칙:

- 한 번에 하나의 READY Issue를 처리한다.
- Issue 범위 안에서 최소 변경한다.
- 보호 브랜치에 직접 Commit·Push하지 않는다.
- 실행하지 않은 테스트를 성공으로 기록하지 않는다.
- 테스트 통과와 계약 전체 충족을 구분한다.
- 일반 구현 오류는 범위 안에서 자체 수정하지만 설계 재결정은 중단한다.

## 7. Contract Traceability Gate

Issue의 최종 계약을 원자 항목으로 나누고 실제 코드·테스트·직접 검증에 연결한다.

| ID | 최종 계약 | 코드·문서 증거 | 테스트·직접 검증 | 상태 |
|---|---|---|---|---|
| C-01 | 구체적인 결과 또는 구현 제약 | `path#symbol` | `TestClass#method` 또는 직접 증거 | `PASS | HOLD | N/A` |

별도 계약으로 관리할 항목:

- API 요청·응답과 예외
- 데이터 원본과 상태
- 시간대·범위·정렬·빈 결과
- 트랜잭션·Rollback·동시성·권한
- Human이 확정한 구현 방식
- 제외 기술과 범위
- 테스트·직접 검증 시나리오
- 문서와 코드 정합성

한 항목이라도 `HOLD`이면 완료·Draft PR을 진행하지 않는다. `N/A`에는 적용 제외 사유가 있어야 한다.

## 8. Troubleshooting Gate

실제 문제가 발생했을 때만 작성한다.

- 발생한 현상
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 최종 해결 또는 BLOCKED
- 재발 방지

단순 import·오타 수정이나 발생하지 않은 예상 문제를 해결 사례로 꾸미지 않는다.

선택지가 여러 개고 계약이 바뀌는 경우 Human 결정 전 구현하지 않는다.

## 9. 검증과 증거 원칙

### 자동 테스트

- 정상·실패·경계·Rollback 규칙을 검증한다.
- Test Double 결과와 실제 운영 Bean·DB·Redis 경로를 구분한다.
- 환경 의존 테스트는 기본 test와 분리할 수 있다.

### 직접 검증

필요한 경우 다음을 추가한다.

- HTTP 요청·응답
- DB 행·카운트·락 결과
- Redis Key·TTL·역할
- 복제 상태와 장애 로그
- 성능 측정 환경·원본 결과

### 문서화

항상 구분한다.

```text
실행한 사실
확인된 결과
추정
해석 제한
미검증 범위
```

테스트가 통과했다는 이유만으로 성능, 가용성, 보안, Exactly Once를 일반화하지 않는다.

## 10. 위험도별 리뷰

### LOW

- 문서·낮은 영향 변경
- Codex 자체 Diff 리뷰
- 실제 Diff 질문 3개
- Human·ChatGPT 검토와 CI

### MEDIUM

- API 내부 흐름·캐시·쿼리·일반 설정 변경
- Contract Traceability
- 실제 Diff 질문 5개
- Human 이해도 작성
- ChatGPT PR 전체 검증
- 필요 시 비담당 리뷰어 1명
- CI

### HIGH

- 금액·포인트·재고, 권한, 트랜잭션, 동시성, DB 구조, 메시지 전달, 운영 장애 설정
- 구현 전 설계·대안·위험 시나리오 확인
- 강화 테스트와 직접 검증
- 실제 Diff 질문 5~8개
- Human 이해도 작성
- ChatGPT PR 전체 검증
- 비담당 리뷰어 최소 2명과 독립 AI 리뷰
- 수정 후 최신 Head 재검토와 CI

위험도는 질문 난이도보다 검증·리뷰 범위를 조절한다.

## 11. 구현 후 이해도 기록

Codex는 최신 실제 Diff를 기준으로 질문만 작성한다.

질문 관점:

- 해결한 문제
- 변경 전·후 데이터 흐름
- 핵심 규칙의 구현 위치
- 정상·실패·Rollback 결과
- 테스트가 보장·비보장하는 범위
- 대안·트레이드오프와 잔여 위험

Human 작성자는 코드와 테스트를 읽고 자신의 말로 답한다. 모르는 내용은 모른다고 기록할 수 있다.

Codex는 다음을 하지 않는다.

- 답변 정확도 평가
- PASS/HOLD 채점
- 모범 답안·보완 설명 작성
- 재제출 요구

ChatGPT는 Human 답변을 포함한 PR 전체를 실제 코드와 대조한다. 단순 표현 부족과 사고로 이어질 수 있는 중대한 오해를 구분하고 리뷰 근거를 제시한다. 최종 반영·Merge 판단은 Human이 한다.

## 12. PR 리뷰와 수정 흐름

```text
1. Codex 구현·검증·Draft PR
2. Human 이해도 답변 작성
3. ChatGPT PR 전체 검증·리뷰 댓글
4. Human 리뷰 항목별 결정
   - 반영
   - 미반영
   - 후속 Issue
5. Codex가 승인된 항목만 수정·재검증·Push
6. ChatGPT가 최신 Head와 CI 재검토
7. Human 최종 Merge
```

Codex는 ChatGPT 리뷰를 자동으로 수정하지 않는다. Human 결정에는 다음을 기록한다.

| 리뷰 항목 | 결정 | 근거 |
|---|---|---|
| 항목 | `반영 | 미반영 | 후속 Issue` | 범위·위험·시간·대안 |

## 13. Merge 경계

```text
Codex Merge 금지
ChatGPT Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

Merge 전 Human 확인:

- Issue 완료 조건
- Contract Traceability 전 항목 PASS
- 최신 Commit CI 성공
- 위험도별 리뷰 완료
- 승인된 수정 반영과 최신 Head 재검토
- 문서·코드 충돌 없음
- 범위 밖 변경 없음
- 미검증·잔여 위험 확인
- 중대한 오해가 남지 않았는지 판단

## 14. 적용하지 않는 자동화

- Issue부터 Merge까지 완전 자동화
- 상시 Planner·Implementer·Reviewer 분리
- 모든 PR의 다중 AI 리뷰
- Human 답변 자동 작성·채점
- 리뷰 자동 반영
- 무제한 자동 수정 반복
- 과도한 Hook·Skill·Worktree 병렬 처리
- AI 자동 Merge
- AI 하네스를 프로젝트 핵심 기술처럼 과장

필요성이 실제로 확인될 때만 자동화와 독립 리뷰를 추가한다.

## 15. 프로젝트에서 얻은 운영 원칙

```text
AI는 상세한 선택지와 증거 구조를 제공한다.
Human은 정책·트레이드오프·반영 범위를 결정한다.
READY는 구현 승인이다.
Codex는 승인된 계약만 구현하고 Draft PR까지 인계한다.
ChatGPT는 PR 전체와 최신 Head를 독립적으로 검증한다.
테스트 통과와 문제 해결·운영 보장을 구분한다.
Merge와 결과 책임은 Human에게 있다.
```

관련 실제 오류와 개선 과정은 [AI Review Log](10_AI_REVIEW_LOG.md)와 [AI Workflow Evolution](13_AI_WORKFLOW_EVOLUTION.md)에 기록한다.
