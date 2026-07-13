# PR #32 AI Review Log

## 작업 내용

Issue #9의 최근 7일 인기 메뉴 TOP 3 조회 기능을 Codex가 구현하고 Draft PR #32를 생성했다.

핵심 JPQL 집계 구조는 최종 설계와 대체로 일치했으나, PR 리뷰에서 최종 계약 일부 누락과 PR 템플릿 축약이 확인됐다. 이번 기록은 개별 코드 수정뿐 아니라 Codex 실행 루프의 구조적 원인을 분석하고 재발 방지 규칙으로 반영하기 위한 것이다.

## ChatGPT 리뷰

| 항목 | 리뷰 내용 | 분류 | 반영 방향 |
|---|---|---|---|
| KST Clock 계약 누락 | Issue는 `Asia/Seoul` 고정 Clock을 확정했지만 `Clock.systemDefaultZone()`이 구현됐다. | 확정 계약 미준수 | `Clock.system(ZoneId.of("Asia/Seoul"))` 적용 및 설정 Bean 검증 |
| 시간 Fixture 방식 불일치 | Issue는 SQL 또는 `JdbcTemplate`을 확정했지만 `ReflectionTestUtils`로 `orderedAt`을 변경했다. | 구현 방식 계약 미준수 | 합의한 Fixture 방식으로 교체 |
| 경계 테스트 일부 누락 | 4개 이상 TOP 3, 1~2개, ACTIVE 0개, Controller 빈 배열을 직접 증명하지 못했다. | 계약 증거 누락 | 요구사항별 테스트 증거 추가 |
| PR 템플릿 축약 | Codex가 저장소 PR 템플릿 전체 대신 자체 요약 본문을 작성해 이해도 질문 영역과 체크리스트 일부를 제거했다. | 실행 루프 설계 누락 + 템플릿 미준수 | 베이스 브랜치 템플릿 보존 Gate 추가 |
| 리뷰 선행 체크 | Human·ChatGPT 리뷰가 실제 수행 전에 완료 처리됐다. | 증거 기록 오류 | 해당 주체가 실제 완료한 뒤에만 체크 |
| 이해도 질문 미생성 | MEDIUM PR인데 실제 클래스·Query·테스트에 기반한 질문이 없었다. | Gate 설계 미흡 | Codex가 최신 Diff를 읽고 5개 질문 생성 |

## 원인 분석

### 1. PR 템플릿을 준수하지 않은 이유

기존 Skill은 Draft PR 본문에 위험도와 검증 증거를 기록하도록만 규정했다. 다음 강제 조건이 없었다.

- 베이스 브랜치의 최신 `.github/PULL_REQUEST_TEMPLATE.md`를 PR 생성 직전에 다시 읽기
- 모든 `##`·`###` 제목을 보존하기
- 빈 섹션을 자체 요약으로 대체하지 않기
- PR 생성 후 템플릿 누락을 재검사하기
- Human·ChatGPT·CI 항목을 선행 체크하지 않기

또한 기존 이해도 Gate는 MEDIUM에서 일반 5문항을 사용하고 실제 Diff 기반 추가 질문을 HIGH에만 요구했다. 이 구조는 Codex가 MEDIUM PR의 질문 영역을 일반 안내 문장으로 축약해도 명시적인 위반으로 판정하기 어렵게 만들었다.

따라서 이번 실패는 Codex의 실행 충실도 문제이면서 동시에 **템플릿 보존을 하드 Gate로 만들지 않은 워크플로우 설계 문제**다.

### 2. 확정 계약을 완전히 지키지 않은 이유

Issue #9에는 최초 AI안, Human 최초 이해, AI 재검증, 협의 결과, 최종 계약이 함께 보존돼 있다. 이 기록 방식은 학습 증거에는 유용하지만 Codex가 권위 있는 최종 영역을 구조적으로 추출하지 않으면 세부 계약을 놓칠 수 있다.

기존 자체 리뷰는 `Issue 완료 조건 충족`이라는 단일 체크였고 다음이 없었다.

- 최종 계약을 원자 항목으로 분해한 목록
- 각 계약과 실제 코드 위치의 연결
- 각 계약과 테스트·직접 검증 증거의 연결
- 구현 방식 제약과 결과 요구사항의 분리
- 하나라도 증거가 없으면 Commit·PR을 중단하는 판정

테스트 통과도 누락을 잡지 못했다.

- Service 테스트는 KST Clock Mock을 사용해 실제 `ClockConfig`의 기본 시간대 의존성을 검증하지 않았다.
- Reflection Fixture는 원하는 조회 결과를 만들었으므로 행동 테스트는 통과했지만 Human이 확정한 구현 방식 제약은 위반했다.
- 존재하지 않는 경계 테스트는 실패할 수 없으므로 전체 테스트 성공이 계약 전체 충족을 의미하지 않았다.

따라서 원인은 **Issue가 부족해서가 아니라 계약별 증거 추적 단계가 없었던 것**이다.

## 개선된 루프

```text
READY Issue
→ 최종 합의·최종 구현 계약·완료 조건만 권위 있는 영역으로 추출
→ Contract Traceability 표 작성
→ 구현·테스트
→ 실제 Diff 자체 리뷰
→ 계약별 코드·테스트 증거 연결
→ 전 항목 PASS
→ 베이스 브랜치 PR 템플릿 재확인·전체 제목 보존
→ Codex가 실제 Diff 기반 질문 생성
→ Commit·Push·Draft PR
→ 리뷰·수정·CI
→ Human 직접 답변
→ Codex가 최신 Diff와 답변 대조 PASS/HOLD
→ Human Merge
```

## 실제 Diff 기반 질문 생성 기준

- LOW: 3개
- MEDIUM: 5개
- HIGH: 5~8개
- 질문마다 실제 파일·클래스·메서드·Query·테스트를 포함
- Codex는 질문만 작성하고 Human 답변은 대신 작성하지 않음
- Human 답변 후 Codex가 문항별 근거와 `PASS | HOLD` 기록

PR #32라면 다음과 같은 구체성을 가져야 한다.

- `ClockConfig.clock()`과 `MenuService.findPopularMenus()`의 시간대 경계
- `MenuRepository.findPopularMenus()`의 `LEFT JOIN ... ON`과 `COUNT(o.id)` 선택 이유
- `PageRequest.of(0, 3)`과 결정적 정렬을 증명하는 테스트
- H2 테스트와 실제 MySQL 환경 사이의 미검증 범위
- 조회 전용 트랜잭션에서 정상·실패 시 데이터 상태

## 최종 반영 결과

- 반영:
  - Contract Traceability Gate 도입
  - PR Template Fidelity Gate 도입
  - 모든 위험도에서 실제 Diff 기반 질문 생성
  - Codex의 Human 답변 검증 절차 도입
  - `gh` 부재와 Commit·Push·PR 생성 책임 분리
- 현재 PR #32 코드 수정:
  - 기존 ChatGPT HOLD 리뷰에 따라 Codex가 같은 브랜치에서 반영해야 함
- Merge:
  - ChatGPT·Codex는 수행하지 않음
  - 코드 수정, 재검증, 실제 Diff 기반 질문, Human 답변 검증이 끝난 뒤 Human이 수행

## 회고

### AI가 도움이 된 부분

- 핵심 `LEFT JOIN ... ON`, `COUNT(o.id)`, 정렬, Pageable 구조는 상세 Issue를 통해 올바른 방향으로 구현됐다.
- PR Diff 리뷰가 설정·Fixture·테스트 증거·PR 본문 오류를 Merge 전에 발견했다.

### AI가 위험했던 부분

- 큰 흐름이 맞고 테스트가 통과했다는 이유로 세부 계약까지 충족했다고 판단했다.
- PR 템플릿을 정보 입력 기준이 아니라 참고 자료처럼 취급했다.
- 자신이 수행하지 않은 Human·ChatGPT 리뷰를 완료로 기록했다.

### 다음 Issue에서 개선할 점

- Issue를 더 길게 만들지 않는다.
- 최종 계약을 요구사항별로 추적하고 코드·테스트 증거가 없는 항목을 HOLD한다.
- PR 템플릿을 그대로 보존한다.
- 이해도 질문은 실제 구현 이후 Codex가 최신 Diff에 맞춰 생성한다.
- Human 답변도 Codex가 최신 Diff와 대조해 검증한다.
