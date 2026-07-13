# Codex 작업 진입점

이 파일은 AI가 저장소 작업을 시작할 때 확인하는 최상위 규칙이다. 상세 기준은 연결된 문서를 따른다.

## 1. 기본 원칙

- AI 활용의 목적은 코드 생성량이 아니라 백엔드 결과물의 안전성, 일관성, 검증 가능성을 높이는 것이다.
- Human은 문제 정의, 도메인 규칙, 설계와 트레이드오프, 위험도, 테스트 기준, 최종 선택과 결과 책임을 맡는다.
- ChatGPT는 프로젝트 문서를 근거로 Draft Issue 초안 작성, Human 착수 검토 재검증, ADR·Markdown 정리, PR diff·Actions 리뷰를 지원한다.
- Codex는 Human이 승인한 `READY` Issue를 기준으로 코드와 테스트를 구현하고 로컬 검증, Commit, Push, Draft PR 생성을 수행한다.
- GitHub Issue는 작업 계약이고 Pull Request는 Codex에서 ChatGPT와 Human으로 넘어가는 공식 인계 지점이다.
- 테스트 통과와 실제 문제 해결 검증을 구분한다.
- Merge는 Human만 수행한다.

Markdown 문서는 Issue에서 수정 대상으로 명시한 경우에만 Codex가 변경한다. 구현 결과에 따라 문서 보완이 필요하면 Draft PR 본문에 남긴다.

## 2. Issue 상태와 구현 시작 조건

- `DRAFT`: 전체 개발 지도를 위한 변경 가능한 초안이다. Codex가 구현하지 않는다.
- `READY`: 담당자 착수 검토, AI 재검증, Ready Check와 Human 승인을 통과한 작업 계약이다.
- `IN_PROGRESS`: Codex가 구현 중이다.
- `REVIEW`: Draft PR이 생성되어 검토 중이다.
- `BLOCKED`: Human 판단이나 환경 조치가 필요하다.
- `DONE`: Human이 Merge하고 필요한 후속 확인을 마친 상태다.

Codex는 `READY` Issue만 구현한다. Human 착수 검토, 완료 조건, 테스트·검증 계획, 수정 범위, 위험도가 부족하면 임의로 보충하지 않고 중단해 보고한다.

## 3. 위험도와 검증 수준

### LOW

예: 문서, 오타, 단순 CRUD, 국소 DTO·Validation·테스트 보완, 작은 버그 수정.

필수 수준:

- Codex 자체 diff 리뷰
- 관련 테스트와 전체 CI
- 작성자 Human 리뷰

별도 계획 승인은 생략할 수 있다.

### MEDIUM

예: 새로운 API, 서비스 비즈니스 로직, 조회 쿼리, 인덱스, 캐시, 이벤트, 여러 계층에 영향을 주는 변경.

필수 수준:

- 짧은 구현 계획과 영향 범위 Human 확인
- 정상·실패·경계 테스트
- Codex 자체 diff 리뷰
- 필요 시 독립 AI 또는 다른 팀원 리뷰
- 작성자 Human 리뷰와 CI

### HIGH

예: 인증·인가, 금액·포인트·재고, 상태 전이, 트랜잭션, 동시성, DB 구조 변경, 메시지 전달 보장, 운영 설정.

필수 수준:

- 구현 전 설계와 트레이드오프 확인
- 필요한 ADR Accepted와 명시적 Human 승인
- 권한·Rollback·동시성·정합성 등 위험에 맞는 강화 테스트
- 독립 AI 리뷰
- 강화된 Human 리뷰와 CI

AI가 잠정 위험도를 제안할 수 있지만 최종 위험도와 리뷰 수준은 Human이 확정한다.

## 4. Codex 기본 실행 흐름

Codex는 복잡한 멀티에이전트 팀을 기본으로 사용하지 않고 주 에이전트가 다음 순서로 작업한다.

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

1. 대상 Issue가 `READY`인지 확인한다.
2. Human 착수 검토와 AI 재검증 결과를 읽는다.
3. `AGENTS.md` 라우팅에 따라 필요한 문서와 관련 코드만 읽는다.
4. 위험도와 Human Gate 충족 여부를 확인한다.
5. Issue 범위, 예상 변경 파일, 테스트, 제외 범위를 짧게 계획한다.
6. Issue 범위 안에서 최소 변경으로 코드와 테스트를 구현한다.
7. 가장 작은 관련 테스트부터 전체 테스트·빌드·diff 검증까지 실행한다.
8. 실제 diff를 다시 읽어 범위 이탈, 핵심 버그, 테스트 누락을 자체 리뷰한다.
9. Issue 범위 안의 수정 가능한 문제는 고치고 재검증한다.
10. 실행한 명령과 결과, 테스트 보장·비보장 범위, 직접 검증, 미검증 항목을 보고한다.
11. 검증이 끝나면 작업 브랜치에 Commit·Push하고 Draft PR을 생성한다.
12. PR 번호를 Human에게 전달하고 위험도에 맞는 리뷰를 기다린다.
13. 승인된 리뷰 수정은 같은 브랜치와 같은 PR에 반영한다.

독립 AI 리뷰는 MEDIUM에서 필요할 때, HIGH에서는 필수로 수행하며 기본 구현 루프의 상시 Subagent로 강제하지 않는다.

## 5. Troubleshooting Gate

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

보고할 때는 다음을 구분한다.

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 영향 범위
- 가능한 선택지와 권장 방향
- 필요한 Human 결정

## 6. 컨텍스트 라우팅

| 작업 종류 | 읽을 문서 |
| --- | --- |
| 프로젝트 목표와 개발 단계 | `docs/01_PROJECT_CONTEXT.md` |
| 기능 요구사항 | `docs/02_REQUIREMENTS.md` |
| API 생성 또는 변경 | `docs/03_API_SPEC.md` |
| Entity, 연관관계, DB 구조 | `docs/04_ERD.md` |
| Team OASIS AI 작업 흐름 | `docs/05_AI_WORKFLOW.md` |
| Codex 경계와 중단 조건 | `docs/06_CODEX_RULES.md` |
| 자동화 실험과 현재 결론 | `docs/11_AI_AUTOMATION_EXPERIMENT.md` |
| 테스트·검증 증거 기준 | `docs/12_EVIDENCE_GUIDE.md` |
| ADR 기준과 양식 | `docs/adr/README.md`, `docs/adr/ADR_TEMPLATE.md` |
| 의미 있는 트러블슈팅 기록 | `logs/README.md` |
| Issue 작성 | `.github/ISSUE_TEMPLATE/feature.md` |
| PR 작성 | `.github/PULL_REQUEST_TEMPLATE.md` |
| 코드 리뷰 작성 | `.github/CODE_REVIEW_TEMPLATE.md` |

## 7. 반드시 지킬 경계

- 한 번에 하나의 `READY` Issue만 처리한다.
- `main` 또는 `develop` 보호 브랜치에 직접 Commit·Push하지 않는다.
- Issue 범위 밖 기능과 불필요한 리팩터링을 추가하지 않는다.
- 검증하지 않은 결과를 완료로 보고하지 않는다.
- 실행하지 못한 검증은 미검증 항목으로 명시한다.
- AI가 작성한 코드는 작성자가 설명할 수 있어야 한다.
- 무제한 자동 수정 반복을 하지 않는다.
- ChatGPT, Codex, 자동화는 Merge하지 않는다.
- 최종 Merge는 최신 CI, 리뷰, 문서 정합성, 범위 확인을 마친 Human이 직접 수행한다.
