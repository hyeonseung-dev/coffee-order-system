# Codex Rules

Codex는 승인된 `READY` Issue를 기준으로 코드 구현, 테스트, 로컬 검증, 실제 Diff 자체 리뷰, Commit·Push·Draft PR 생성을 담당한다.

Markdown 문서는 기본적으로 ChatGPT와 Human이 관리한다. Codex는 Issue에서 명시적으로 허용한 문서만 수정한다.

## 1. 구현 시작 조건

다음 조건이 충족돼야 구현한다.

- 대상 GitHub Issue 존재
- Issue 상태 `READY`
- 담당 Human 착수 검토
- AI 재검증 결과와 Human 최종 결정
- 목적, 도메인 규칙, 완료 조건, 제외 범위
- 수정 허용·금지 범위
- 위험도 `LOW | MEDIUM | HIGH`
- 테스트 계획, 직접 검증 방법, 성공 기준
- 필요한 ADR과 Human 승인

조건이 부족하거나 현재 코드와 계약이 충돌하면 임의로 보충하거나 구현하지 않는다.

## 2. 작업 범위

- Issue 범위 안에서만 작업한다.
- Issue에 없는 기능을 추가하지 않는다.
- 불필요한 리팩터링을 하지 않는다.
- 패키지와 계층 구조를 임의로 변경하지 않는다.
- 기존 API, DTO, 예외, DB 계약을 임의로 변경하지 않는다.
- 승인된 코드·테스트 파일만 수정한다.
- Markdown 문서는 Issue에서 허용한 경우에만 수정한다.
- 구현 결과에 따라 필요한 문서 보완은 Draft PR에 기록한다.

## 3. 위험도별 행동

### LOW

예:

- 문서와 오타
- 기존 패턴의 단순 CRUD
- 국소 DTO·Mapper·Validation
- 테스트 보완
- 작은 버그 수정

Codex가 짧은 계획을 작성하고 구현, 테스트, Diff 리뷰, Commit, Push, Draft PR 생성까지 진행한다.

### MEDIUM

예:

- 새로운 API
- 서비스 비즈니스 로직
- Repository 조회 쿼리
- 인덱스
- 캐시
- 이벤트 처리
- 여러 계층 변경

구현 범위, 예상 파일, 영향 범위, 정상·실패·경계 테스트를 보고한다. `READY`라면 추가 승인 없이 진행하되 설계 재결정이 필요하면 중단한다.

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

구현 전 설계, 트레이드오프, 필요한 ADR, Human 승인을 확인한다. 구현 후 독립 AI 리뷰와 위험에 맞는 강화 테스트가 필요하다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 4. 기본 실행 흐름

```text
Read
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Link Contract Evidence
→ Fix
→ Re-verify
→ Generate Actual-Diff Questions
→ Report
→ Commit / Push / Draft PR
```

복잡한 Agent Teams와 상시 Subagent 분리는 기본값으로 사용하지 않는다.

## 5. 작업 전 확인

```bash
git branch --show-current
git status
git log --oneline -5
```

확인 내용:

- 보호 브랜치가 아닌지
- 현재 브랜치와 Issue가 일치하는지
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

보호 파일 변경이 필요하면 이유, 최소 변경안, 영향, 검증 방법을 보고하고 Human 승인을 기다린다.

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

## 9. Troubleshooting Gate

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

보고 시 구분:

- 발생한 문제와 재현 조건
- 확인된 사실
- 추정 원인
- 시도한 방법과 결과
- 영향 범위
- 가능한 선택지와 권장 방향
- 필요한 Human 결정

## 10. 구현 후 검증

실행 순서:

1. 변경과 직접 관련된 가장 작은 테스트
2. 필요한 통합·권한·Rollback·동시성·성능 테스트
3. 전체 테스트
4. 전체 빌드
5. Diff 검사
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

## 11. Contract Traceability와 Diff 리뷰

Commit 전에 실제 Diff를 다시 읽고 Issue 최종 계약을 원자 항목으로 대조한다.

| ID | 최종 계약 | 실제 코드 증거 | 테스트·직접 검증 증거 | 상태 |
|---|---|---|---|---|
| C-01 | 구체적인 계약 | `path#symbol` | `TestClass#method` 또는 미검증 사유 | `PASS | HOLD | N/A` |

확인 항목:

- Issue 완료 조건 충족
- 범위 이탈과 불필요한 리팩터링
- API·ERD·ADR·예외 계약 불일치
- 핵심 버그와 실패 흐름
- 테스트 누락과 의미 없는 테스트
- 실행하지 않은 검증의 허위 기록
- 필요한 문서 보완

한 행이라도 `HOLD`이면 Commit·Draft PR 단계로 진행하지 않는다.

## 12. Commit·Push·Draft PR

Contract Traceability 전 항목 PASS, 검증, 자체 Diff 리뷰가 끝나면:

1. Issue 범위 변경만 Commit
2. 작업 브랜치 Push
3. Draft PR 생성
4. 베이스 브랜치의 최신 PR 템플릿 전체 사용
5. 생성된 PR 본문 재확인
6. PR 번호 전달

PR 본문에는 실제 Diff 기반 질문을 작성하고 Human·팀원 답변은 비워 둔다.

## 13. 팀 이해·오해 점검

이 단계는 정답 시험이 아니다.

- 작성자와 팀원이 실제 Diff를 읽는다.
- 이해한 내용과 모르는 부분을 자기 언어로 기록한다.
- `잘 모르겠다`고 답할 수 있다.
- AI는 정답 재제출을 요구하지 않고 실제 코드 근거와 보완 설명을 제공한다.

### 참여 기준

- LOW: 작성자 답변 + 비담당 리뷰어 1명 확인
- MEDIUM: 작성자 답변 + 비담당 리뷰어 최소 1명 이해 요약
- HIGH: 작성자 답변 + 비담당 리뷰어 최소 2명 이해 요약 + 필요 시 팀원 전원 확인

### 상태

이해 점검에는 `PASS | HOLD`를 사용하지 않는다.

- `확인 완료`
- `보완 설명 필요`
- `보완 설명 완료`
- `차단 필요`

Codex는 다음만 기록한다.

- 실제 코드와 일치하는 부분
- 보완 설명이 필요한 부분
- 미검증·잔여 위험
- 중대한 오해 후보

중대한 오해 여부의 최종 판단은 Human 리뷰어가 한다.

중대한 오해 예시:

- 금액·포인트·재고 정합성
- 인증·인가
- 트랜잭션·Rollback
- 동시성 보호 대상
- 캐시와 원본 데이터
- 데이터 삭제·Migration
- 메시지 중복·유실
- 테스트 범위 과대평가

단순 용어 부족, 표현 미숙, 세부 구현 기억 부족은 차단 사유가 아니다.

### Merge 차단

- LOW·MEDIUM: 중대한 오해가 남아 있을 때만 Merge 보류
- HIGH: 필수 참여자의 이해 점검과 보완 설명 확인이 끝날 때까지 Merge 보류
- 모든 위험도: 중대한 오해가 남아 있으면 Merge 보류

## 14. 리뷰와 Merge 금지

- LOW: 자체 Diff 리뷰 + 작성자 Human 리뷰 + 비담당 리뷰어 확인 + CI
- MEDIUM: 자체 Diff 리뷰 + 작성자 Human 리뷰 + 비담당 리뷰어 최소 1명 + 필요 시 독립 리뷰 + Human/ChatGPT 리뷰 + CI
- HIGH: 구현 전 설계 확인 + 작성자 답변 + 비담당 리뷰어 최소 2명 + 독립 AI 리뷰 + 강화된 Human 리뷰 + 위험 테스트 + CI

```text
Codex Merge 금지
ChatGPT Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

최신 Commit 기준 CI, 문서 정합성, 범위, Contract Traceability, 위험도별 팀 이해·오해 점검을 Human이 직접 확인한 뒤 Merge한다.
