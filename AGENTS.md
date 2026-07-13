# Codex 작업 진입점

이 파일은 AI가 저장소 작업을 시작할 때 확인하는 최상위 규칙이다. 상세 기준은 연결된 문서를 따른다.

## 1. 기본 원칙

- AI 활용의 목적은 코드 생성량이 아니라 백엔드 결과물의 안전성, 일관성, 검증 가능성을 높이는 것이다.
- Human은 문제 정의, 도메인 규칙, 설계와 트레이드오프, 위험도, 테스트 기준, 최종 선택과 결과 책임을 맡는다.
- ChatGPT는 Draft Issue와 Human 착수 검토 재검증, 문서 정리, 독립적인 PR Diff·Actions 리뷰를 지원한다.
- Codex는 Human이 승인한 `READY` Issue를 기준으로 브랜치 생성, 구현, 테스트, 계약 추적, Commit, Push, Draft PR, 실제 Diff 기반 이해도 질문 생성을 수행한다.
- GitHub Issue는 작업 계약이고 Pull Request는 Codex에서 ChatGPT와 Human으로 넘어가는 공식 인계 지점이다.
- 테스트 통과와 Issue 최종 계약 전체 충족을 구분한다.
- Merge는 Human만 수행한다.

Markdown 문서는 Issue에서 수정 대상으로 명시한 경우에만 Codex가 변경한다. 구현 결과에 따라 문서 보완이 필요하면 Draft PR 본문에 남긴다.

## 2. Issue 상태와 구현 시작 조건

- `DRAFT`: 전체 개발 지도를 위한 변경 가능한 초안이다. Codex가 구현하지 않는다.
- `READY`: 담당자 착수 검토, AI 재검증, 최종 합의, 위험도 확정, 필요한 ADR, 테스트 기준, Human 구현 승인을 통과한 작업 계약이다.
- `IN_PROGRESS`: Codex가 구현 중이다.
- `REVIEW`: Draft PR이 생성되어 검토 중이다.
- `BLOCKED`: Human 판단이나 환경 조치가 필요하다.
- `DONE`: Human이 Merge하고 필요한 후속 확인을 마친 상태다.

Codex는 `READY` Issue만 구현한다. `READY`는 구현 시작 승인까지 완료됐다는 뜻이므로 LOW·MEDIUM·HIGH 모두 구현 계획이나 작업 브랜치 생성을 이유로 추가 승인을 요청하지 않는다.

Human 착수 검토, 완료 조건, 테스트·검증 계획, 수정 범위, 위험도, 필요한 ADR 또는 명시적 승인이 실제 Issue에 누락돼 있거나 현재 코드와 충돌할 때만 중단해 보고한다.

## 3. 권위 있는 Issue 계약

Issue 전체를 읽되 구현 기준은 다음 영역이다.

1. Human·AI 최종 합의
2. 최종 구현 계약
3. 완료 조건
4. 테스트·직접 검증 계획
5. 포함·제외 범위
6. 수정 허용·금지 범위
7. 위험도와 승인 상태

AI 최초 제안, Human 최초 이해, AI 재검증 기록은 배경과 학습 기록이다. 최종 계약과 충돌하면 최종 계약을 따른다.

권위 있는 영역끼리 충돌하거나 현재 코드에서 이행할 수 없으면 임의 선택하지 않고 Troubleshooting Gate로 전환한다.

## 4. 위험도와 검증 수준

### LOW

예: 문서, 오타, 단순 CRUD, 국소 DTO·Validation·테스트 보완, 작은 버그 수정.

- 관련 테스트와 전체 CI
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 이해도 질문 3개
- 작성자 Human 리뷰

짧은 계획을 남기고 추가 승인 없이 구현한다.

### MEDIUM

예: 새로운 API, 서비스 비즈니스 로직, 조회 쿼리, 인덱스, 캐시, 이벤트, 여러 계층에 영향을 주는 변경.

- 짧은 구현 계획과 영향 범위 보고
- 정상·실패·경계 테스트
- Contract Traceability Gate
- Codex 자체 Diff 리뷰
- 실제 Diff 기반 이해도 질문 5개
- 필요 시 독립 AI 또는 다른 팀원 리뷰
- 작성자 Human 리뷰와 CI

Issue가 `READY`라면 계획을 보고한 뒤 추가 Human 확인 없이 구현을 계속한다.

### HIGH

예: 인증·인가, 금액·포인트·재고, 상태 전이, 트랜잭션, 동시성, DB 구조 변경, 메시지 전달 보장, 운영 설정.

- 구현 전 설계와 트레이드오프가 Issue에 기록됨
- 필요한 ADR Accepted와 명시적 Human 승인이 Issue에 기록됨
- 권한·Rollback·동시성·정합성 등 위험에 맞는 강화 테스트
- Contract Traceability Gate
- 실제 Diff 기반 이해도 질문 5~8개
- 독립 AI 리뷰
- 강화된 Human 리뷰와 CI

Issue가 `READY`라면 위 선행 승인이 이미 완료된 것으로 본다. 기록이 실제로 누락됐거나 충돌할 때만 `BLOCKED`로 전환한다.

## 5. Codex 기본 실행 흐름

```text
Read authoritative contract
→ Minimal Preflight / Create Branch
→ Build Contract Traceability Map
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Link code and test evidence to every contract
→ Fix / Re-verify
→ Verify PR template fidelity
→ Generate actual-Diff questions
→ Report
→ Commit / Push / Draft PR
```

1. 대상 Issue가 `READY`인지 확인한다.
2. 권위 있는 최종 계약 영역을 추출한다.
3. `AGENTS.md` 라우팅에 따라 필요한 문서와 관련 코드만 읽는다.
4. 위험도와 READY 선행 조건 충족 여부를 확인한다.
5. 현재 브랜치와 작업 트리를 확인한다.
6. 현재 브랜치가 `main` 또는 `develop`이고 작업 트리가 깨끗하면 `codex/issue-{번호}-{간단한-slug}` 형식의 작업 브랜치를 자동 생성한다.
7. 이미 대상 Issue 전용 브랜치라면 그대로 계속한다.
8. 미커밋 변경이 있거나 다른 작업 브랜치와 충돌하면 덮어쓰지 말고 중단해 보고한다.
9. 최종 계약을 원자 항목으로 분해해 구현 위치와 필요한 증거를 연결한다.
10. Issue 범위, 예상 변경 파일, 테스트, 제외 범위를 짧게 계획하고 보고한다.
11. Issue 범위 안에서 최소 변경으로 코드와 테스트를 구현한다.
12. 가장 작은 관련 테스트부터 전체 테스트·빌드·Diff 검증까지 실행한다.
13. 실제 Diff를 다시 읽어 범위 이탈, 핵심 버그, 테스트 누락을 자체 리뷰한다.
14. 모든 계약 항목에 실제 코드와 테스트·직접 검증 증거를 연결한다.
15. 증거가 없는 계약이 있으면 완료 보고와 PR 생성을 중단하고 수정하거나 `HOLD`를 보고한다.
16. 베이스 브랜치의 최신 `.github/PULL_REQUEST_TEMPLATE.md`를 다시 읽고 모든 제목을 보존한다.
17. 최신 Diff 기반 이해도 질문을 위험도에 맞게 작성한다. Human 답변은 작성하지 않는다.
18. 실행한 명령과 결과, 계약 추적표, 테스트 보장·비보장 범위, 직접 검증, 미검증 항목을 보고한다.
19. 검증이 끝나면 작업 브랜치에 Commit·Push하고 Draft PR을 생성한다.
20. 생성된 PR 본문을 다시 읽어 템플릿 누락과 선행 체크를 확인한다.
21. PR 번호를 Human에게 전달하고 위험도에 맞는 리뷰를 기다린다.
22. 승인된 리뷰 수정은 같은 브랜치와 같은 PR에 반영한다.

작업 브랜치 생성과 짧은 구현 계획 보고는 정상 실행 절차이며 별도의 Human 승인 Gate가 아니다.

## 6. Contract Traceability Gate

Codex는 구현 전에 다음 형식의 내부 추적표를 만들고 Commit 전에 실제 증거로 갱신한다.

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 요구사항 또는 구현 제약 | `path#symbol` | `TestClass#method` 또는 미검증 사유 | `PASS | HOLD | N/A` |

- 결과 요구사항뿐 아니라 시간대, JOIN 위치, Fixture 방식, 도입 금지 기술 같은 구현 제약도 별도 항목으로 만든다.
- 모든 항목에 실제 증거가 있어야 한다.
- `N/A`는 사유를 기록한다.
- 한 항목이라도 `HOLD`이면 완료 보고, Commit, Draft PR 생성을 진행하지 않는다.
- 테스트 통과는 추적표 전체 PASS를 대신하지 않는다.

상세 규칙은 `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md`를 따른다.

## 7. PR Template Fidelity Gate

Draft PR 생성 직전에 베이스 브랜치의 최신 `.github/PULL_REQUEST_TEMPLATE.md`를 읽는다.

- 모든 `##`·`###` 제목을 보존한다.
- 빈 섹션을 자체 요약으로 대체하거나 삭제하지 않는다.
- Contract Traceability 표를 실제 증거로 채운다.
- Codex가 직접 완료한 항목만 체크한다.
- Human 리뷰, ChatGPT 리뷰, 독립 리뷰, CI, 이해도 검증은 실제 완료 전에 체크하지 않는다.
- 실제 Diff 기반 질문은 질문마다 파일·클래스·메서드·Query·설정·테스트 중 하나 이상을 명시한다.
- PR 생성 후 본문을 다시 읽어 누락을 확인한다.

`gh` CLI 부재는 Commit·Push 중단 사유가 아니다.

- Commit과 Push는 표준 `git` 명령으로 수행한다.
- Draft PR은 사용 가능한 GitHub 연동, API 또는 `gh`로 생성한다.
- PR 생성 수단이 전혀 없을 때만 Push까지 완료하고 PR 생성 단계만 `BLOCKED`로 보고한다.

## 8. 구현 후 이해도 검증

Codex는 구현·테스트·최종 Diff를 기준으로 질문을 생성한다.

- LOW: 3개
- MEDIUM: 5개
- HIGH: 5~8개

고정된 일반 질문을 그대로 복사하지 않는다. 다음 관점을 실제 코드에 연결한다.

- 해결한 문제와 요청→응답 흐름
- 핵심 도메인 규칙의 구현 위치와 계층 선택 이유
- 정상·실패 시 데이터 상태와 트랜잭션·Rollback
- 가장 위험한 경계·동시성·권한·시간·캐시 조건
- 테스트 보장 범위와 미검증 범위
- 대안과 트레이드오프
- 운영 환경의 잔여 위험

Human이 답변한 뒤 Codex는 최신 Head의 Diff와 테스트를 다시 읽고 문항별 `PASS | HOLD`, 코드 근거, 오해, 잔여 위험을 PR에 기록한다.

Codex가 Human 답변을 대신 작성하거나 이해도 PASS를 Merge 승인으로 사용하면 안 된다.

## 9. Troubleshooting Gate

다음 문제는 AI가 임의 해결하거나 범위를 넓히지 않고 즉시 중단해 Human에게 보고한다.

- 요구사항을 변경해야 테스트를 통과할 수 있음
- API 계약, DB 스키마, 트랜잭션 경계, 인증·권한 정책 재결정 필요
- 동시성 정합성, 데이터 손실, 중복 처리 가능성 발견
- 테스트 삭제·비활성화·약화가 필요함
- 성능 목표 미달 또는 운영 장애 가능성 발견
- ADR과 구현이 충돌함
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 변경 또는 보호 파일 변경 필요
- 문서의 가정과 실제 코드 구조가 충돌함
- 권위 있는 최종 계약 영역끼리 충돌함
- 미커밋 변경 또는 다른 작업 브랜치의 변경을 덮어쓸 위험이 있음

다음은 Troubleshooting Gate가 아니다.

- 깨끗한 `main` 또는 `develop`에서 Issue 전용 작업 브랜치를 생성하는 일
- `READY` Issue의 짧은 구현 계획을 보고하는 일
- Issue에 이미 승인된 구현을 시작하는 일
- `gh` CLI 없이 표준 `git`으로 Commit·Push하는 일

보고할 때는 발생한 문제, 확인된 사실, 추정 원인, 시도 결과, 영향 범위, 선택지, 권장 방향, 필요한 Human 결정을 구분한다.

## 10. 컨텍스트 라우팅

| 작업 종류 | 읽을 문서 |
|---|---|
| 프로젝트 목표와 개발 단계 | `docs/01_PROJECT_CONTEXT.md` |
| 기능 요구사항 | `docs/02_REQUIREMENTS.md` |
| API 생성 또는 변경 | `docs/03_API_SPEC.md` |
| Entity, 연관관계, DB 구조 | `docs/04_ERD.md` |
| Team OASIS AI 작업 흐름 | `docs/05_AI_WORKFLOW.md` |
| Codex 경계와 중단 조건 | `docs/06_CODEX_RULES.md` |
| 자동화 실험과 현재 결론 | `docs/11_AI_AUTOMATION_EXPERIMENT.md` |
| 테스트·검증 증거 기준 | `docs/12_EVIDENCE_GUIDE.md` |
| 계약 추적·PR 템플릿·이해도 Gate | `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md` |
| ADR 기준과 양식 | `docs/adr/README.md`, `docs/adr/ADR_TEMPLATE.md` |
| 의미 있는 트러블슈팅 기록 | `logs/README.md` |
| AI 리뷰 사례 | `docs/10_AI_REVIEW_LOG.md`, `docs/reviews/` |
| Issue 작성 | `.github/ISSUE_TEMPLATE/feature-refinement.md` |
| PR 작성 | `.github/PULL_REQUEST_TEMPLATE.md` |
| 코드 리뷰 작성 | `.github/CODE_REVIEW_TEMPLATE.md` |

계약 추적, PR 템플릿 보존, 실제 Diff 기반 이해도 질문에 관해서는 `docs/14_CODEX_CONTRACT_TRACEABILITY_GATE.md`가 세부 실행 기준이다.

## 11. 반드시 지킬 경계

- 한 번에 하나의 `READY` Issue만 처리한다.
- `main` 또는 `develop` 보호 브랜치에 직접 Commit·Push하지 않는다.
- 보호 브랜치의 작업 트리가 깨끗하면 Issue 전용 브랜치를 자동 생성하고 계속한다.
- Issue 범위 밖 기능과 불필요한 리팩터링을 추가하지 않는다.
- 최종 계약에 없는 편의 구현으로 계약된 구현 방식을 바꾸지 않는다.
- 검증하지 않은 결과를 완료로 보고하지 않는다.
- 실행하지 못한 검증은 미검증 항목으로 명시한다.
- PR 템플릿을 자체 요약 본문으로 대체하지 않는다.
- Human·ChatGPT·CI·이해도 검증을 선행 체크하지 않는다.
- AI가 작성한 코드는 작성자가 설명할 수 있어야 한다.
- 무제한 자동 수정 반복을 하지 않는다.
- ChatGPT, Codex, 자동화는 Merge하지 않는다.
- 최종 Merge는 최신 CI, 리뷰, 문서 정합성, 계약 추적, 이해도 검증을 마친 Human이 직접 수행한다.
