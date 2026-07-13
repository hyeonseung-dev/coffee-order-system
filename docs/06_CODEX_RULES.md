# Codex Rules

Codex는 승인된 `READY` Issue를 기준으로 코드 구현, 테스트, 로컬 검증, 실제 diff 자체 리뷰, Commit·Push·Draft PR 생성을 담당한다.

요구사항, README, API 명세, ERD, ADR, 워크플로우 등 Markdown 문서는 기본적으로 ChatGPT와 Human이 관리한다. Codex는 Issue에서 허용한 문서만 수정한다.

## 1. 구현 시작 조건

다음 조건이 충족돼야 구현한다.

- 대상 GitHub Issue가 존재함
- Issue 상태가 `READY`임
- 담당 Human 착수 검토가 있음
- AI 재검증 결과와 Human 최종 결정이 있음
- 목적, 도메인 규칙, 완료 조건, 제외 범위가 명확함
- 수정 허용·금지 범위가 명확함
- 위험도가 `LOW`, `MEDIUM`, `HIGH` 중 하나로 확정됨
- 테스트 계획, 직접 검증 방법, 성공 기준이 있음
- 예상 트러블슈팅과 Human 공유 조건이 있음
- 필요한 ADR과 Human 승인이 완료됨

조건이 부족하면 임의로 보충하거나 구현하지 않는다.

## 2. 작업 범위

- Issue 범위 안에서만 작업한다.
- Issue에 없는 기능을 추가하지 않는다.
- 불필요한 리팩터링을 하지 않는다.
- 패키지와 계층 구조를 임의로 변경하지 않는다.
- 기존 API, DTO, 예외, DB 계약을 임의로 변경하지 않는다.
- 승인된 코드·테스트 파일만 수정한다.
- Markdown 문서는 Issue에서 Codex 수정 대상으로 명시한 경우에만 수정한다.
- 구현 결과에 따라 문서 보완이 필요하면 Draft PR 본문에 기록한다.

## 3. 위험도별 행동

### LOW

예:

- 문서와 오타
- 기존 패턴의 단순 CRUD
- 국소 DTO·Mapper·Validation
- 테스트 보완
- 작은 버그 수정

주 Codex가 짧은 계획을 작성하고 구현, 테스트, 자체 diff 리뷰, Commit, Push, Draft PR 생성까지 진행한다.

### MEDIUM

예:

- 새로운 API
- 서비스 비즈니스 로직
- Repository 조회 쿼리
- 인덱스
- 캐시
- 이벤트 처리
- 여러 계층에 영향을 주는 변경

파일 수정 전에 구현 범위, 예상 파일, 영향 범위, 정상·실패·경계 테스트를 보고하고 Human 확인 후 진행한다. 독립 AI 리뷰는 필요 시 별도 단계에서 수행한다.

### HIGH

예:

- 인증·인가
- 금액·포인트·결제·환불·재고
- 상태 전이
- 트랜잭션 경계와 Rollback
- 동시성 제어
- DB Migration과 구조 변경
- 메시지 전달 보장
- 운영 인프라와 배포 설정

구현 전 설계와 트레이드오프, 필요한 ADR, 명시적 Human 승인을 확인한다. 새로운 설계 선택이 발견되면 즉시 중단한다. 구현 후 독립 AI 리뷰와 위험에 맞는 강화 테스트가 필요하다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 4. 기본 실행 흐름

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

복잡한 Agent Teams와 상시 Subagent 분리는 기본값으로 사용하지 않는다. 주 Codex가 구현 루프를 수행하고, 독립 리뷰는 위험도에 따라 별도로 실행한다.

## 5. 작업 전 확인

```bash
git branch --show-current
git status
git log --oneline -5
```

확인 내용:

- `main` 또는 `develop` 보호 브랜치가 아닌지
- 현재 브랜치와 대상 Issue가 일치하는지
- 기존 미커밋 변경과 충돌하지 않는지
- 수정 예정 코드·테스트 파일
- 보호 파일 포함 여부
- 관련 테스트와 실행 명령
- Issue 범위 밖 변경 필요 여부

## 6. 보호 파일

Issue에서 명시적으로 허용하지 않은 다음 파일은 수정하지 않는다.

- `build.gradle`
- `settings.gradle`
- `application.yml`
- `application.properties`
- `README.md`
- `AGENTS.md`
- `docs/*.md`
- `.github/**/*.md`
- `.codex/**/*.md`
- `.github/workflows/*`
- `.githooks/*`

보호 파일 변경이 필수라면 수정 이유, 최소 변경안, 영향, 검증 방법을 보고하고 Human 승인을 기다린다.

## 7. 구현 원칙

- 가장 작은 변경 단위부터 구현한다.
- Controller, Service, Repository, Domain 책임을 유지한다.
- 비즈니스 규칙을 테스트로 표현한다.
- 정상 흐름뿐 아니라 실패·경계·Rollback 흐름을 검증한다.
- 테스트를 통과시키기 위해 요구사항을 왜곡하지 않는다.
- 실패한 테스트를 삭제하거나 비활성화하지 않는다.
- 새로운 의존성이나 인프라를 임의로 추가하지 않는다.
- 현재 Issue와 무관한 문제는 별도 보고한다.
- 실행하지 않은 명령은 성공으로 기록하지 않는다.
- 검증 결과는 `docs/12_EVIDENCE_GUIDE.md` 기준으로 기록한다.

## 8. JavaDoc과 주석

JavaDoc은 코드의 일부이므로 Codex가 작성할 수 있다.

- 주요 클래스에는 책임과 협력 대상을 설명한다.
- Service 비즈니스 메서드에는 핵심 규칙과 실패 조건을 설명한다.
- 트랜잭션, 동시성, 락, 캐시, 이벤트에는 선택 이유와 보호 대상을 설명한다.
- 코드 내용을 그대로 번역하는 주석은 작성하지 않는다.
- 단순 getter, 기본 생성자, 명확한 위임 메서드에 기계적으로 작성하지 않는다.

## 9. 일반 디버깅과 Troubleshooting Gate

### 자체 해결 가능한 일반 오류

- 컴파일 오류
- import 누락
- 단순 테스트 데이터 오류
- 명확한 Mock 설정 오류
- 코드 스타일과 정적 검사 오류
- 기존 패턴으로 해결 가능한 테스트 실패

### 반드시 중단할 중요 문제

- 요구사항을 변경해야 테스트 통과 가능
- API 계약, DB 스키마, 트랜잭션 경계 변경 필요
- 인증·권한 정책 재결정 필요
- 동시성 정합성 미보장
- 데이터 손실 또는 중복 처리 가능성
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR과 실제 구현 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 또는 보호 파일 변경 필요
- Human 착수 검토의 가정과 실제 코드 구조 충돌

중요 문제를 단순 수정으로 숨기지 않는다.

보고 시 다음을 구분한다.

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 영향 범위
- 가능한 선택지와 권장 방향
- 필요한 Human 결정

## 10. 구현 후 검증

다음 순서로 실행한다.

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

최종 보고에는 다음을 구분한다.

- 실제 실행한 명령과 결과
- 테스트가 보장하는 것
- 테스트가 보장하지 않는 것
- 직접 검증 결과
- 수행하지 못한 검증
- 미검증 항목과 알려진 제한

테스트 통과만으로 완료를 선언하지 않는다.

## 11. Review Diff와 재검증

주 Codex는 Commit 전에 실제 diff를 다시 읽는다.

- Issue 완료 조건 충족
- 범위 이탈과 불필요한 리팩터링
- API·ERD·ADR·예외 계약 불일치
- 핵심 버그와 실패 흐름
- 테스트 누락과 의미 없는 테스트
- 실행하지 않은 검증의 허위 기록
- 필요한 문서 보완

Issue 범위 안에서 수정 가능한 문제는 고치고 재검증한다. 같은 실패가 반복되거나 설계 변경이 필요하면 Troubleshooting Gate로 전환한다. 무제한 자동 수정 반복을 하지 않는다.

## 12. Commit·Push·Draft PR

READY Issue와 필요한 Human Gate를 통과하고 검증이 완료되면 다음을 수행한다.

1. Issue 범위의 코드와 테스트만 Commit한다.
2. 작업 브랜치를 Push한다.
3. Draft PR을 생성한다.
4. PR 본문에 다음을 기록한다.
   - `Closes #<Issue 번호>`
   - 위험도와 요구 리뷰 수준
   - 구현 내용과 주요 변경 파일
   - 실제 테스트와 빌드 결과
   - 테스트가 보장하는 것과 보장하지 않는 것
   - 직접 검증 결과
   - 의미 있는 실제 트러블슈팅
   - 미검증 항목과 알려진 제한
   - Issue 범위 이탈 여부
   - 독립 리뷰 필요 여부
   - ChatGPT 집중 리뷰 대상
   - 필요한 문서 보완
5. PR 번호를 Human에게 전달한다.

Draft PR 생성 이후에는 전체 결과를 대화에 복사하지 않는다.

## 13. 리뷰와 Merge 금지

- LOW: 자체 diff 리뷰 + Human 리뷰 + CI
- MEDIUM: 자체 diff 리뷰 + 필요 시 독립 리뷰 + Human 리뷰 + CI
- HIGH: 구현 전 설계 확인 + 독립 리뷰 + 강화된 Human 리뷰 + 위험에 맞는 테스트 + CI

Codex의 작업은 Draft PR 생성과 승인된 리뷰 반영까지다.

```text
Codex Merge 금지
ChatGPT Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

최신 Commit 기준 CI, 문서 정합성, 범위 확인, 작성자 이해를 Human이 직접 확인한 뒤 Merge한다.
