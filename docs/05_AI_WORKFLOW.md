# AI-assisted Development Workflow

## 목적

본 프로젝트는 ChatGPT와 Codex를 같은 역할로 사용하지 않는다.

ChatGPT는 요구사항·설계·문서·GitHub 운영을 담당하고, Codex는 승인된 범위 안에서 코드 구현과 로컬 검증에 집중한다. 두 도구 사이의 기본 인계 수단은 대화 복사가 아니라 GitHub Issue와 Pull Request다.

최종 설계 결정, 위험 작업 승인, 코드 반영 여부, Merge 여부는 Human이 판단한다.

## 기본 Workflow

1. Human이 구현할 기능과 우선순위를 제시한다.
2. ChatGPT가 저장소 문서, 기존 코드, Issue와 PR 상태를 확인하고 구현 범위를 설계한다.
3. ChatGPT가 GitHub Issue와 구현 전 Markdown 문서를 직접 작성·수정한다.
4. Human이 Issue의 범위와 설계를 승인한다.
5. Codex가 승인된 Issue를 기준으로 Java/Spring 코드와 테스트를 구현한다.
6. Codex가 로컬 테스트·빌드·diff 검증을 수행한다.
7. Codex가 Commit, Push, PR 생성을 완료하고 PR 번호를 Human에게 전달한다.
8. Human은 ChatGPT에 PR 번호만 전달한다.
9. ChatGPT가 GitHub에서 PR diff, 커밋, Actions, Issue 완료 조건을 직접 확인하고 리뷰한다.
10. 구현 결과에 종속되는 README, API 설명, 리뷰 로그, 트러블슈팅 문서는 ChatGPT가 PR 확인 후 보완한다.
11. 수정이 필요하면 Codex가 코드만 보완하고 같은 PR에 Push한다.
12. ChatGPT가 재검토하고 Human이 최종 확인 후 Merge한다.

## 역할 분리

### Human

- 기능 방향과 우선순위 결정
- 설계와 Issue 범위 승인
- 데이터 삭제, 마이그레이션, 외부 비용, 보안 정책 변경 승인
- 최종 기능 확인과 Merge 판단

### ChatGPT

- 요구사항 정리와 설계
- 작업 범위 분리
- GitHub Issue 작성
- README, API 명세, 작업 규칙, 워크플로우, 리뷰 로그 등 Markdown 문서 작성·수정
- 소규모 GitHub 설정과 문서 PR 처리
- Codex 구현 PR의 diff, 커밋, Actions, 테스트 결과 검토
- 구현 후 문서 보완과 PR 리뷰

### Codex

- 승인된 Issue 범위의 Java/Spring 코드 구현
- 테스트 코드 작성
- 로컬 실행, 컴파일, 테스트, 빌드, 디버깅
- Commit, 작업 브랜치 Push, PR 생성
- 리뷰에서 지적된 코드 문제 수정

Codex는 별도 요청이 없는 한 설계 문서나 운영 규칙을 새로 작성하지 않는다. 문서 변경이 필요하면 PR 본문에 ChatGPT 보완 항목으로 남긴다.

### GitHub

- Issue로 요구사항과 완료 조건 전달
- Branch와 Commit으로 구현 이력 관리
- PR로 Codex 결과를 ChatGPT에 인계
- Actions로 테스트와 빌드 결과 제공
- 리뷰와 Merge 이력 보존

## PR 기반 인계 원칙

Codex가 PR을 생성한 뒤에는 실행 결과 전체를 대화에 복사하지 않는다.

Human은 다음과 같이 PR 번호만 전달한다.

```text
PR #<번호> 구현 끝났어. 검토해줘.
```

ChatGPT는 해당 PR에서 다음을 직접 확인한다.

- Issue 목적과 완료 조건
- 변경 파일과 실제 diff
- 커밋 범위
- 테스트·빌드 결과
- GitHub Actions 상태와 실패 로그
- 범위 이탈, 설계 불일치, 보안·정합성·성능 위험

## 로컬 BLOCKED 예외

Codex가 Push 또는 PR 생성 전에 막히면 GitHub에 확인할 결과가 없으므로 다음만 Human에게 전달한다.

- 실패한 명령
- 핵심 오류 메시지
- 현재 브랜치와 작업 트리 상태
- 수행하지 못한 검증
- 필요한 Human 승인 또는 환경 조치

PR이 생성된 이후의 Actions 실패는 ChatGPT가 GitHub에서 직접 확인한다.

## 문서 작성 시점

### 구현 전에 ChatGPT가 작성

- 요구사항
- API 명세
- 완료 조건
- 설계 결정
- 수정 가능·금지 범위
- 테스트 시나리오

### 구현 후 PR을 확인하고 ChatGPT가 작성

- 실제 변경 파일과 구현 현황
- 실제 테스트·빌드 결과
- 트러블슈팅
- 성능 수치
- README 진행 현황
- AI Review Log와 PR 설명 보완

실제 구현과 다른 문서를 미리 확정하지 않는다.

## 리뷰 원칙

- ChatGPT 리뷰: 설계 의도와 코드 일치, 트랜잭션·정합성·권한·테스트·문서 불일치 확인
- Codex 문맥 리뷰: Issue 요구사항 충족과 기존 코드 흐름 충돌 확인
- Codex 독립 리뷰: 잠재 버그, 과도한 변경, 보안·성능 위험 확인

리뷰 횟수보다 실제 위험과 수정 근거를 우선한다.

## 세부 규칙 위임

Codex의 작업 범위, 보호 파일, 중단 조건, 구현 후 보고는 [06_CODEX_RULES.md](06_CODEX_RULES.md)를 따른다.

## 기록 원칙

AI 리뷰 결과는 PR 또는 [10_AI_REVIEW_LOG.md](10_AI_REVIEW_LOG.md)에 기록한다.
