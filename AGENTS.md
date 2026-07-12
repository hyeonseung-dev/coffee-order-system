# Codex 작업 진입점

이 파일은 Codex가 코드 구현을 시작할 때 확인하는 저장소 진입 규칙이다. 상세 기준은 연결된 문서를 따른다.

## 1. 역할 경계

- Human은 목적, 요구사항, 설계, 위험 판단, 코드 이해, 최종 선택과 결과 책임을 맡는다.
- ChatGPT는 요구사항 구조화, Issue와 Markdown 문서 관리, ADR 작성 지원, PR diff·Actions 검토와 리뷰를 맡는다.
- Codex는 승인된 Issue를 기준으로 Java/Spring 코드와 테스트 구현, 로컬 실행·디버깅, Commit·Push·PR 생성을 맡는다.
- GitHub Issue는 작업 계약이고 Pull Request는 Codex에서 ChatGPT와 Human으로 넘어가는 공식 인계 지점이다.

Markdown 문서는 Issue에서 Codex 수정 대상으로 명시한 경우에만 Codex가 수정한다. 구현 결과에 따라 문서 보완이 필요하면 PR 본문에 `ChatGPT 문서 보완 필요`로 남긴다.

## 2. Issue 상태와 구현 시작 조건

- `DRAFT`: 변경 가능한 백로그다. Codex가 구현하지 않는다.
- `READY`: Refinement와 Ready Check를 통과하고 Human이 구현 시작을 승인한 작업 계약이다.
- `IN_PROGRESS`: Codex가 구현 중이다.
- `REVIEW`: PR이 생성되어 검토 중이다.
- `BLOCKED`: Human 판단이나 환경 조치가 필요하다.
- `DONE`: Human이 Merge한 뒤 종료된 상태다.

Codex는 `READY` Issue만 구현한다. Issue 상태가 불명확하거나 완료 조건, 테스트 계획, 검증 방법, 수정 범위가 부족하면 구현하지 않고 질문한다.

## 3. 위험도 기준

- `LOW`: 기존 패턴을 따르는 단순 조회, DTO, Validation, 테스트 보완, 작은 버그 수정. 별도 계획 승인 없이 구현부터 PR 생성까지 진행한다.
- `MEDIUM`: API 계약, Entity 관계, 트랜잭션 범위, 인덱스, 캐시, 이벤트처럼 설계 영향이 있는 작업. 짧은 계획과 영향 범위를 Human에게 보고하고 승인 후 구현한다.
- `HIGH`: 인증·인가, 금액·포인트, 동시성, DB Migration, 메시지 전달 보장, 운영 설정처럼 데이터 손실·보안·운영 장애 가능성이 있는 작업. 필요한 ADR과 Human 승인 없이는 구현하지 않는다.

위험도는 별도 실행 모드와 중복해서 사용하지 않는다.

## 4. 기본 작업 흐름

1. 대상 GitHub Issue와 상태가 `READY`인지 확인한다.
2. `AGENTS.md` 라우팅에 따라 필요한 문서와 관련 코드만 읽는다.
3. 위험도와 Human Gate 충족 여부를 확인한다.
4. Issue 범위 안에서 코드와 테스트를 구현한다.
5. 가장 작은 관련 테스트부터 실행하고 전체 테스트·빌드·diff 검증을 수행한다.
6. 일반적인 컴파일·테스트 오류는 Issue 범위 안에서 스스로 해결한다.
7. 중요한 설계·정합성·보안·성능·테스트 문제가 발견되면 `Troubleshooting Gate`에서 멈춘다.
8. 검증이 끝나면 Commit하고 작업 브랜치를 Push한다.
9. PR을 생성하고 테스트, 검증, 트러블슈팅, 미검증 항목, ChatGPT 인계 요청을 기록한다.
10. PR 번호를 Human에게 전달하고 ChatGPT·Human 리뷰를 기다린다.
11. 리뷰 수정 사항은 같은 브랜치와 같은 PR에 반영한다.

PR이 생성된 뒤에는 구현 결과 전체를 대화에 복사하지 않는다. Human이 ChatGPT에 PR 번호만 전달하면 ChatGPT가 GitHub에서 직접 확인한다.

## 5. Troubleshooting Gate

다음 문제는 임의 해결하지 않고 즉시 중단해 Human에게 보고한다.

- 요구사항을 변경해야 테스트를 통과할 수 있음
- API 계약, DB 스키마, 트랜잭션 경계, 권한 정책 변경 필요
- 동시성 정합성, 데이터 손실, 중복 처리 가능성 발견
- 테스트 삭제·비활성화·약화가 필요함
- 성능 목표 미달 또는 운영 장애 가능성 발견
- ADR과 구현이 충돌함
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 변경 또는 보호 파일 변경 필요

보고할 때는 `확인된 사실`, `추정 원인`, `시도한 방법`, `영향 범위`, `선택지`, `필요한 Human 결정`을 구분한다.

## 6. 컨텍스트 라우팅

| 작업 종류 | 읽을 문서 |
| --- | --- |
| 프로젝트 목표와 개발 단계 | `docs/01_PROJECT_CONTEXT.md` |
| 기능 요구사항 | `docs/02_REQUIREMENTS.md` |
| API 생성 또는 변경 | `docs/03_API_SPEC.md` |
| Entity, 연관관계, DB 구조 | `docs/04_ERD.md` |
| 반자동 AI 작업 흐름 | `docs/05_AI_WORKFLOW.md` |
| Codex 경계와 중단 조건 | `docs/06_CODEX_RULES.md` |
| 자동화 실험과 현재 결론 | `docs/11_AI_AUTOMATION_EXPERIMENT.md` |
| ADR 기준과 양식 | `docs/adr/README.md`, `docs/adr/ADR_TEMPLATE.md` |
| 테스트·검증·트러블슈팅 기록 | `logs/README.md` |
| Issue 작성 | `.github/ISSUE_TEMPLATE/feature.md` |
| PR 작성 | `.github/PULL_REQUEST_TEMPLATE.md` |
| 코드 리뷰 작성 | `.github/CODE_REVIEW_TEMPLATE.md` |

## 7. 반드시 지킬 경계

- 한 번에 하나의 READY Issue만 처리한다.
- Issue 범위 밖 기능과 불필요한 리팩터링을 추가하지 않는다.
- 검증하지 않은 결과를 완료로 보고하지 않는다.
- 실행하지 못한 검증은 미검증 항목으로 명시한다.
- AI가 작성한 코드는 Human이 이해하고 검토할 수 있어야 한다.
- ChatGPT는 Merge하지 않는다.
- Codex는 Merge하지 않는다.
- 자동 Merge를 사용하지 않는다.
- 최종 Merge는 Human이 직접 수행한다.