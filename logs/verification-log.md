# Verification Log

`coffee-order-issue-loop`에서 수행한 검증 결과를 Issue별로 누적 기록한다.

실행하지 않은 검증은 성공으로 기록하지 않는다.

## 기록 형식

### Issue #{issue-number} — {title}

- 검증 시각:
- 기준 브랜치:
- 작업 브랜치:
- Attempt:
- 위험도:
- 실행 주체:
- 최종 상태: PASS / FAIL / BLOCKED

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| 작은 관련 테스트 |  |  |  |
| 관련 테스트 |  |  |  |
| 전체 테스트 |  |  |  |
| 전체 빌드 |  |  |  |
| git status |  |  |  |
| git diff --stat |  |  |  |
| git diff |  |  |  |

#### 미검증 항목

- 항목:
- 실행하지 못한 이유:
- 완료 판정에 미치는 영향:
- 대체 근거:
- Human 판단 필요 여부:

#### Reviewer 결과

- 판정:
- 완료 기준 충족 여부:
- 범위 이탈 여부:
- 발견한 문제:
- 수정 지침:
- 재검증 결과:

#### 증거

- 테스트 리포트:
- 빌드 결과:
- Attempt Log:
- 관련 PR:
- 기타:

#### Human 확인

- 확인 필요 항목:
- 승인 또는 보류 결과:
- Merge 여부:

---

실제 Issue 결과를 이번 단계에서 추가하지 않는다.

---

### Issue #10 — [v1][test] 동시 주문 시 포인트 정합성 문제 재현

- 검증 시각: 2026-07-16 Asia/Seoul
- 기준 브랜치: develop
- 작업 브랜치: `codex/issue-10-concurrent-order-point-consistency`
- Attempt: 1/1
- 위험도: MEDIUM
- 실행 주체: coffee-order-issue-loop Implementer / Verify
- 최종 상태: PASS

#### 실행 조건

- Docker MySQL 8.4 컨테이너가 `healthy`, 호스트 포트 `3307`으로 실행 중인 상태에서 수행했다.
- 로컬 `.env`를 현재 셸에만 로드하고, Compose 변수 `MYSQL_USER`·`MYSQL_PASSWORD`를 애플리케이션 설정이 요구하는 `DB_USERNAME`·`DB_PASSWORD`로 현재 Gradle 프로세스에만 매핑했다.
- 자격 증명 값과 `.env` 파일 내용은 출력·기록·변경하지 않았다.
- `ConcurrentOrderMySqlIntegrationTest`는 실제 MySQL, 각 요청의 별도 Service 트랜잭션, 10개 스레드, 시작 `CountDownLatch`, 작업별 30초 제한을 사용한다.

#### 동시성 관찰 결과

| 구분 | 회차 | 성공 | 실패·예외 | 주문 | USE 이력 | 최종 잔액 | 정합성 위반 유형 |
| --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| Service | 1 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 2 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 3 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 4 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 5 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 6 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 7 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 8 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 9 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| Service | 10 | 10 | 0 / 없음 | 10 | 10 | 4,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |
| API 보조 | 1 | 10 | 0 / 없음 | 10 | 10 | 7,000 | `LOST_UPDATE_OR_BALANCE_MISMATCH`, `EXCESS_SUCCESS_ORDER` |

- 1차 10회 안에 모든 Service 회차에서 위반이 관찰됐으므로 30회 확대는 계약에 따라 수행하지 않았다.
- Service와 API 보조 시나리오 모두 10건의 요청·주문·USE 이력은 일치했다. 그러나 초기 10,000P에서 3,000P 주문이 10건 모두 성공해 성공 요청 수가 최대 3건을 초과했고, 최종 잔액도 성공 요청 수 기준 기대값과 불일치했다.
- API 보조 시나리오는 HTTP 요청·서버 요청 스레드·HTTP 클라이언트를 추가로 경유한다. 이번 실행에서는 Service 시나리오와 같은 위반 유형이 관찰됐으나, HTTP·서버·커넥션 풀 조건이 추가되므로 두 결과를 동일한 성능 측정값으로 해석하지 않는다.

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| 실제 MySQL 동시성 테스트 | `source .env` 후 현재 셸에 DB 변수 매핑, `./gradlew concurrencyTest --no-daemon` | PASS | Service 10회와 API 보조 1회가 실제 MySQL에서 완료되고 위 표의 정합성 위반을 기록 |
| 전체 테스트·빌드 | `./gradlew test build --no-daemon` | PASS | `concurrency` 태그는 기본 테스트에서 분리된 상태로 전체 H2 테스트와 빌드 통과 |
| DB 컨테이너 상태 | `docker compose ps` | PASS | MySQL 8.4 healthy |

#### 미검증 항목

- 실제 다중 애플리케이션 인스턴스, 운영 트래픽, 네트워크 장애, 잠금 대기·데드락은 검증하지 않았다.
- 비관적 락 적용 후의 정합성 회복은 Issue #11 범위다.

#### 증거

- 테스트 리포트: `build/test-results/concurrencyTest/TEST-com.example.coffeeordersystem.service.ConcurrentOrderMySqlIntegrationTest.xml`
- 빌드 결과: `./gradlew test build --no-daemon` 성공
- 기타: `CONCURRENCY_OBSERVATION` 로그 11건

---

### Issue #8 — [v0][feat] 커피 주문·결제 API 구현

- 검증 시각: 2026-07-12 Asia/Seoul
- 기준 브랜치: develop
- 작업 브랜치: develop
- Attempt: 1/2
- 위험도: MEDIUM
- 실행 주체: coffee-order-issue-loop Preflight / Record
- 최종 상태: BLOCKED

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| 작은 관련 테스트 | 미실행 | BLOCKED | 요구사항 충돌 및 기존 작업 트리 변경으로 구현 전 중단 |
| 관련 테스트 | 미실행 | BLOCKED | 요구사항 충돌 및 기존 작업 트리 변경으로 구현 전 중단 |
| 전체 테스트 | `./gradlew test` 미실행 | BLOCKED | 요구사항 충돌 및 기존 작업 트리 변경으로 구현 전 중단 |
| 전체 빌드 | `./gradlew build` 미실행 | BLOCKED | 요구사항 충돌 및 기존 작업 트리 변경으로 구현 전 중단 |
| git status | `git status` | BLOCKED | 미추적 `.DS_Store` 존재 |
| git diff --stat | 미실행 | BLOCKED | 구현 diff가 없음; 로그 기록 전 Preflight 중단 |
| git diff | 미실행 | BLOCKED | 구현 diff가 없음; 로그 기록 전 Preflight 중단 |
| git diff --check | 미실행 | BLOCKED | 요구사항 충돌 및 기존 작업 트리 변경으로 구현 전 중단 |

#### 미검증 항목

- 항목: 관련 테스트, 전체 테스트, 전체 빌드, `git diff --check`, 구현 diff, 독립 Reviewer 검토
- 실행하지 못한 이유: Issue #8의 Entity/Repository 요구와 `docs/04_ERD.md`의 v0 제외 범위가 충돌하며, 기존 미추적 `.DS_Store`가 작업 트리에 존재한다.
- 완료 판정에 미치는 영향: 핵심 완료 기준을 검증할 수 없으므로 PASS 판정 불가
- 대체 근거: GitHub Issue #8 원문, `docs/02_REQUIREMENTS.md`, `docs/03_API_SPEC.md`, `docs/04_ERD.md`, `docs/06_CODEX_RULES.md`, `docs/11_AI_AUTOMATION_EXPERIMENT.md`
- Human 판단 필요 여부: 예

#### Reviewer 결과

- 판정: BLOCKED
- 완료 기준 충족 여부: 미검증
- 범위 이탈 여부: 없음; 구현 파일을 변경하지 않음
- 발견한 문제: `docs/04_ERD.md`의 v0 Entity/Repository 제외와 Issue #8 구현 범위의 요구사항 충돌, 기존 미추적 `.DS_Store`
- 수정 지침: 해당 없음; Human 설계 및 작업 트리 결정이 선행되어야 함
- 재검증 결과: 미실행

#### 증거

- 테스트 리포트: 없음 (Preflight 중단)
- 빌드 결과: 없음 (Preflight 중단)
- Attempt Log: `logs/issues/issue-8/attempt-log.md`
- 관련 PR: 없음
- 기타: GitHub Issue #8 읽기 전용 조회, `git status`, `git branch --show-current`, `git log --oneline -5`

#### Human 확인

- 확인 필요 항목: Issue #8과 ERD v0 범위 중 우선할 원본 요구사항, 기존 `.DS_Store` 처리
- 승인 또는 보류 결과: 보류
- Merge 여부: 수행하지 않음

---

### Issue #8 — Attempt 1 Verify 재개 (Human 검증 환경)

- 검증 시각: 2026-07-12 Asia/Seoul
- 기준 브랜치: develop
- 작업 브랜치: develop
- Attempt: 1/2
- 위험도: HIGH (Human 설계 승인 완료)
- 실행 주체: Human 검증 환경 / coffee-order-issue-loop Verify 기록
- 최종 상태: REVIEW PENDING

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| Rollback 통합 테스트 | `OrderTransactionRollbackIntegrationTest` | PASS | BUILD SUCCESSFUL |
| 전체 테스트 | `./gradlew test` | PASS | BUILD SUCCESSFUL |
| 전체 빌드 | `./gradlew build` | PASS | BUILD SUCCESSFUL |
| git diff --check | `git diff --check` | PASS | 통과 |

#### 환경 복구 근거

- 이전 차단 원인: Gradle 실행 프로세스에 DB 환경변수가 전달되지 않아 `${DB_USERNAME}`·`${DB_PASSWORD}`가 미치환된 상태로 MySQL 인증이 실패함
- 복구 환경: `DB_URL=jdbc:mysql://localhost:3307/coffee_order?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`, `DB_USERNAME=coffee`, `DB_PASSWORD` 제공
- 구현 코드 변경: 없음

#### Reviewer 결과

- 판정: PASS
- 테스트 재실행: 하지 않음 (Human 제공 실제 결과 사용)

#### 증거

- Attempt Log: `logs/issues/issue-8/attempt-log.md`
- 기타: Human 제공 검증 결과

#### 최종 결과

- 최종 상태: PASS
- 완료 기준 충족 여부: 주문 API·단일 트랜잭션·Rollback·AFTER_COMMIT·테스트·빌드·diff check 충족
- 범위 이탈 여부: 없음
- 미검증 항목: 없음
- Human 확인: Commit, Push, PR 생성, Merge는 미수행

---

### Issue #8 — Attempt 1 구현 및 검증

- 검증 시각: 2026-07-12 Asia/Seoul
- 기준 브랜치: develop
- 작업 브랜치: develop
- Attempt: 1/2
- 위험도: HIGH (Human 설계 승인 완료)
- 실행 주체: coffee-order-issue-loop Implementer / Verify / Reviewer
- 최종 상태: BLOCKED

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| 관련 테스트 | `./gradlew test --tests OrderServiceTest --tests OrderControllerTest` | PASS | 주문 Service 및 Controller 테스트 통과 |
| 전체 테스트 | `./gradlew test` | FAIL | `OrderTransactionRollbackIntegrationTest` DB 인증 환경 오류 |
| 전체 빌드 | `./gradlew build` | FAIL | 전체 테스트의 동일한 DB 인증 환경 오류 |
| git diff --check | `git diff --check` | PASS | 종료 코드 0 |
| git status | `git status --short` | PASS | Issue #8 구현·테스트·로그 파일만 변경 |

#### 미검증 항목

- 항목: 실제 DB 기반 Rollback 및 AFTER_COMMIT Listener 미실행, 전체 테스트·빌드 통과
- 실행하지 못한 이유: `${DB_USERNAME}`·`${DB_PASSWORD}` 미주입으로 MySQL 1045 인증 실패가 발생해 Spring ApplicationContext와 JPA JDBC metadata를 초기화하지 못함
- 완료 판정에 미치는 영향: Issue의 핵심 원자성·Rollback·Commit 이후 이벤트 완료 기준을 실제로 검증하지 못해 PASS 불가
- 대체 근거: `OrderTransactionRollbackIntegrationTest` 작성, 관련 Service/Controller 테스트 PASS
- Human 판단 필요 여부: 예

#### Reviewer 결과

- 판정: `BLOCKED: VERIFICATION ENVIRONMENT REQUIRED`
- 완료 기준 충족 여부: 관련 단위/MVC 및 diff check는 확인, 실제 트랜잭션 완료 기준은 미검증
- 범위 이탈 여부: 없음
- 발견한 문제: 검증 DB 환경 변수·인증 정보 부재
- 수정 지침: 자동 수정 없음; 검증용 DB 환경 제공 또는 최소 테스트 DB 설정 허용 필요
- 재검증 결과: 미실행

#### 증거

- 테스트 리포트: `build/test-results/test/TEST-com.example.coffeeordersystem.service.OrderTransactionRollbackIntegrationTest.xml`
- 빌드 결과: `./gradlew build` 종료 코드 1
- Attempt Log: `logs/issues/issue-8/attempt-log.md`
- 관련 PR: 없음
- 기타: MySQL 1045, Hibernate JDBC metadata/Dialect 초기화 실패

#### Human 확인

- 확인 필요 항목: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 제공 또는 테스트 DB 설정 허용
- 승인 또는 보류 결과: 보류
- Merge 여부: 수행하지 않음

---

### Issue #8 — 재실행 Preflight / Planner

- 검증 시각: 2026-07-12 Asia/Seoul
- 기준 브랜치: develop
- 작업 브랜치: develop
- Attempt: 1/2 (구현 전 BLOCKED Attempt 지속)
- 위험도: HIGH (Issue 본문의 MEDIUM 표기와 별개로 트랜잭션·상태 전이·금액 범위를 `docs/11_AI_AUTOMATION_EXPERIMENT.md` 기준으로 재분류)
- 실행 주체: coffee-order-issue-loop Preflight / Planner / Record
- 최종 상태: BLOCKED

#### 실행한 검증

| 구분 | 명령 | 결과 | 핵심 근거 |
| --- | --- | --- | --- |
| Preflight | `git branch --show-current`, `git status --short`, `git log --oneline -5` | PASS | `develop`, clean worktree, 문서 정합성 커밋 `c5cdce3` 확인 |
| Issue 확인 | GitHub Issue #8 읽기 전용 조회 | PASS | 목적·범위·완료 기준·위험도·검증 명령 확인 |
| Planner 검토 | Issue·문서·현재 코드 읽기 전용 대조 | BLOCKED | HIGH 설계 승인 부재 |

#### 미검증 항목

- 항목: 관련 테스트, `./gradlew test`, `./gradlew build`, `git diff --check`, 구현 diff, 독립 Reviewer 검토
- 실행하지 못한 이유: Planner가 HIGH 위험 설계의 Human 승인 부재로 구현 전 BLOCKED를 반환했다.
- 완료 판정에 미치는 영향: 구현과 핵심 검증 근거가 없으므로 PASS 판정 불가
- 대체 근거: GitHub Issue #8, `docs/11_AI_AUTOMATION_EXPERIMENT.md`, `logs/issues/issue-8/attempt-log.md`
- Human 판단 필요 여부: 예

#### Reviewer 결과

- 판정: 미실행
- 완료 기준 충족 여부: 미검증
- 범위 이탈 여부: 없음; 구현 파일을 변경하지 않음
- 발견한 문제: 단일 요청 트랜잭션·Rollback 검증 및 AFTER_COMMIT Listener 책임에 대한 HIGH 설계 승인 부재
- 수정 지침: 해당 없음; Human 설계 승인이 선행되어야 함
- 재검증 결과: 미실행

#### 증거

- 테스트 리포트: 없음 (Planner BLOCKED)
- 빌드 결과: 없음 (Planner BLOCKED)
- Attempt Log: `logs/issues/issue-8/attempt-log.md`
- 관련 PR: 없음
- 기타: `c5cdce3`, GitHub Issue #8 읽기 전용 조회

#### Human 확인

- 확인 필요 항목: 주문 트랜잭션·Rollback 검증 방식 및 `OrderCompletedEvent` AFTER_COMMIT Listener Mock 처리 책임 승인
- 승인 또는 보류 결과: 보류
- Merge 여부: 수행하지 않음
