# Codex Rules

Codex는 승인된 `READY` Issue를 기준으로 코드 구현, 테스트, 로컬 검증, 실제 Diff 자체 리뷰, Commit·Push·Draft PR 생성을 담당한다. Draft PR 이후에는 Human이 승인한 리뷰 항목만 수정한다.

Markdown 문서는 기본적으로 ChatGPT와 Human이 관리한다. Codex는 Issue 또는 Human 승인 범위에 명시된 문서만 수정한다.

## 1. 최종 워크플로우

```text
Codex 구현·Draft PR
→ Human 이해도 작성
→ ChatGPT PR 전체 검증 및 리뷰 댓글
→ Human 반영 범위 결정
→ Codex 승인된 리뷰 수정
→ ChatGPT 최신 Head 재검토
→ Human 최종 Merge
```

Codex의 책임은 최초 Draft PR 생성과 승인된 리뷰 수정까지다. Human 답변 평가와 PR 최종 승인은 Codex 책임이 아니다.

## 2. 구현 시작 조건

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

## 3. 작업 범위

- Issue 범위 안에서만 작업한다.
- Issue에 없는 기능을 추가하지 않는다.
- 불필요한 리팩터링을 하지 않는다.
- 패키지와 계층 구조를 임의로 변경하지 않는다.
- 기존 API, DTO, 예외, DB 계약을 임의로 변경하지 않는다.
- 승인된 코드·테스트·문서 파일만 수정한다.
- 새로운 의존성이나 인프라를 임의로 추가하지 않는다.

## 4. 위험도별 행동

### LOW

문서·오타, 기존 패턴의 단순 CRUD, 국소 DTO·Validation, 테스트 보완, 작은 버그 수정이다. 짧은 계획 후 구현·검증·Draft PR까지 진행한다.

### MEDIUM

새 API, 서비스 로직, 조회 쿼리, 인덱스, 캐시, 이벤트, 여러 계층 변경이다. 범위와 정상·실패·경계 테스트를 보고하고 진행한다.

### HIGH

인증·인가, 금액·포인트·결제·환불·재고, 상태 전이, 트랜잭션·Rollback, 동시성, Migration, 메시지 전달, 운영 인프라다. 구현 전 설계·ADR·Human 승인을 확인하고 강화 테스트를 수행한다.

최종 위험도와 리뷰 수준은 Human이 결정한다.

## 5. 기본 실행 흐름

```text
Read
→ Plan
→ Implement
→ Verify
→ Review Diff
→ Link Contract Evidence
→ Fix / Re-verify
→ Generate Actual-Diff Questions
→ Commit / Push / Draft PR
→ Human에게 인계
```

복잡한 Agent Teams와 상시 Subagent 분리는 기본값으로 사용하지 않는다.

## 6. 작업 전 확인

```bash
git branch --show-current
git status
git log --oneline -5
```

확인 내용:

- 보호 브랜치가 아닌지
- 현재 브랜치와 Issue가 일치하는지
- 기존 미커밋 변경과 충돌하지 않는지
- 수정 예정 코드·테스트·문서 파일
- 보호 파일 포함 여부
- 관련 테스트와 실행 명령
- Issue 범위 밖 변경 필요 여부

## 7. 보호 파일

Issue 또는 Human 승인 범위에 명시되지 않은 다음 파일은 수정하지 않는다.

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

보호 파일 변경이 필요하면 이유, 최소 변경안, 영향, 검증 방법을 보고하고 승인을 기다린다.

## 8. 구현 원칙

- 가장 작은 변경 단위부터 구현한다.
- Controller, Service, Repository, Domain 책임을 유지한다.
- 비즈니스 규칙을 테스트로 표현한다.
- 정상·실패·경계·Rollback 흐름을 검증한다.
- 테스트를 통과시키기 위해 요구사항을 왜곡하지 않는다.
- 실패한 테스트를 삭제하거나 비활성화하지 않는다.
- 현재 Issue와 무관한 문제는 별도 보고한다.
- 실행하지 않은 명령은 성공으로 기록하지 않는다.
- 검증 결과는 `docs/12_EVIDENCE_GUIDE.md` 기준으로 기록한다.

## 9. JavaDoc과 주석

JavaDoc은 코드의 일부이므로 Codex가 작성할 수 있다.

- 주요 클래스에는 책임과 협력 대상을 설명한다.
- Service 메서드에는 핵심 규칙과 실패 조건을 설명한다.
- 트랜잭션, 동시성, 락, 캐시, 이벤트에는 선택 이유와 보호 대상을 설명한다.
- 코드 내용을 그대로 번역하는 주석은 작성하지 않는다.
- 단순 getter, 기본 생성자, 명확한 위임 메서드에 기계적으로 작성하지 않는다.

## 10. Troubleshooting Gate

자체 해결 가능:

- 컴파일·import·코드 스타일 오류
- 단순 테스트 데이터·Mock 설정 오류
- 기존 패턴으로 해결 가능한 테스트 실패

반드시 중단:

- 요구사항·설계·API·DB·트랜잭션·권한 정책 재결정
- 동시성 정합성 미보장
- 데이터 손실 또는 중복 처리 가능성
- 테스트 삭제·비활성화·약화 필요
- 성능 목표 미달 또는 운영 장애 가능성
- ADR·최종 계약·현재 코드 구조 충돌
- Issue 범위 밖 또는 보호 파일 변경 필요
- 다른 작업 내용을 덮어쓸 위험

보고 시 발생 조건, 확인된 사실, 추정, 시도 결과, 영향 범위, 선택지, 필요한 Human 결정을 구분한다.

## 11. 구현 후 검증

순서:

1. 변경과 직접 관련된 가장 작은 테스트
2. 필요한 통합·권한·Rollback·동시성·성능 테스트
3. 전체 테스트
4. 전체 빌드
5. Diff 검사
6. 필요한 API·DB·로그 직접 검증

```bash
./gradlew test
./gradlew build
git diff --check
git status
git diff --stat
git diff
```

최종 보고에는 실제 실행 결과, 테스트 보장 범위, 미검증 범위, 직접 검증, 알려진 제한을 구분한다. 테스트 통과만으로 완료를 선언하지 않는다.

## 12. Contract Traceability와 Diff 리뷰

Commit 전에 실제 Diff와 Issue 최종 계약을 원자 항목으로 대조한다.

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

한 행이라도 `HOLD`이면 Draft PR 단계로 진행하지 않는다.

## 13. Commit·Push·Draft PR

Contract Traceability 전 항목 PASS와 검증이 끝나면:

1. Issue 범위 변경만 Commit
2. 작업 브랜치 Push
3. Draft PR 생성
4. 베이스 브랜치의 최신 PR 템플릿 전체 사용
5. PR 본문 재확인
6. PR 번호와 미검증 범위 전달

PR 본문에는 `docs/15_DIFF_QUESTION_POLICY.md` 기준의 실제 Diff 질문을 작성하고 Human 답변은 비워 둔다.

## 14. Human 이해도와 ChatGPT 리뷰 경계

Codex는 실제 Diff 기반 질문을 작성하는 데서 역할이 끝난다.

Codex 금지사항:

- Human 답변의 정확도·이해도 평가
- 코드와 답변의 일치 여부 판정표 작성
- 보완 설명·모범 답안·대체 답변 작성
- Human 답변 수정 또는 재제출 요구
- 중대한 오해 여부 최종 판정
- ChatGPT 리뷰를 대신 수행하거나 완료 표시

Human은 자신의 말로 답변하며 `모르겠다`고 기록할 수 있다. ChatGPT는 답변을 포함한 PR 전체를 검증하고 리뷰 댓글을 남긴다.

## 15. 승인된 리뷰 수정

Human이 ChatGPT 리뷰 댓글의 반영 범위를 `반영 | 미반영 | 후속 Issue`로 결정한 뒤 Codex가 작업한다.

- 승인된 항목만 수정한다.
- 최신 Head와 승인 범위를 다시 읽는다.
- 설계 재결정이 필요하면 중단한다.
- 관련 테스트와 전체 검증을 다시 수행한다.
- Contract Traceability와 검증 증거를 필요한 범위에서 갱신한다.
- Commit·Push 후 ChatGPT 최신 Head 재검토로 인계한다.
- Human 답변 문구는 수정하지 않는다.
- ChatGPT 재검토나 Merge를 선행 완료 표시하지 않는다.

## 16. 리뷰와 Merge 금지

```text
Codex Merge 금지
ChatGPT Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

Human은 최신 CI, 문서 정합성, 범위, Contract Traceability, ChatGPT 최신 Head 재검토 결과를 직접 확인한 뒤 Merge한다.
