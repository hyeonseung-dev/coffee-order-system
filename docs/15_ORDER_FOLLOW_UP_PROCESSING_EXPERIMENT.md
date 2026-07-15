# 주문 후속 처리 방식 실험 기록

이 문서는 주문 완료 후 외부 데이터 플랫폼으로 전달하는 방식을 단계별로 비교하는 실험 기록이다. 각 단계는 별도 Human 승인 후에만 추가하며, 결과는 실제 실행한 테스트와 측정 로그만 기록한다.

## 실험 목적

주문 후속 처리를 주문 트랜잭션과 어떤 시점·스레드에서 분리할지 판단하기 위해, 직접 동기 호출부터 단계적으로 지연·예외·Rollback 영향을 확인한다.

현재 1단계부터 3단계까지 기록했다. 4단계 `AFTER_COMMIT + @Async`는 아직 구현하거나 측정하지 않았다.

## 공통 테스트 조건

- `@SpringBootTest`와 `test` 프로필에서 실제 JPA 저장소를 사용한다.
- 외부 플랫폼은 `OrderDataPlatformClient`의 Mockito 기반 테스트 Stub으로 제어한다.
- 주문 사용자 잔액은 10,000 포인트, 메뉴 가격은 3,000 포인트다.
- 관찰 대상은 요청 실행 스레드, 외부 Client 실행 스레드, `OrderService.order()` 호출 구간의 소요 시간, 주문 수, 지갑 잔액, USE 이력 수다.
- 소요 시간은 단일 실행값이며 절대 성능 지표가 아니라 같은 테스트 환경에서 단계별 영향을 비교하는 자료다.

## 1단계: 주문 트랜잭션 내부 직접 동기 호출

### 구현 흐름

`OrderService.order()`의 단일 트랜잭션에서 사용자·메뉴·지갑을 검증하고, 포인트 차감과 USE 이력 저장, 주문 저장을 수행한다. 이어서 Commit 전에 `OrderDataPlatformClient.sendOrderCompleted(...)`를 직접 호출한 뒤 응답을 반환한다.

따라서 외부 호출이 반환될 때까지 같은 요청 스레드와 주문 트랜잭션이 대기한다. 외부 호출이 `RuntimeException`을 던지면 예외가 호출자에게 전파되고 주문 트랜잭션이 Rollback된다.

### 실행 결과

| 조건 | 요청 / 외부 Client 스레드 | `order()` 측정 시간 | 주문·포인트·USE 이력 DB 결과 | 결과 |
| --- | --- | ---: | --- | --- |
| 정상 외부 호출 | `Test worker` / `Test worker` | 4ms | 주문 1건, 잔액 7,000, USE 이력 1건 | Commit |
| 외부 호출 2초 지연 | `Test worker` / `Test worker` | 2,014ms | 주문 1건, 잔액 7,000, USE 이력 1건 | Commit. 외부 지연만큼 주문 처리도 지연 |
| 외부 호출 예외 | `Test worker` / `Test worker` | 20ms | 주문 0건, 잔액 10,000, USE 이력 0건 | 예외 전파 및 Rollback |
| 주문 저장 예외 | `Test worker` / 외부 Client 호출 전 실패 | 23ms | 주문 0건, 잔액 10,000, USE 이력 0건 | 예외 전파 및 Rollback |

위 결과는 `OrderSynchronousExternalCallIntegrationTest`와 `OrderTransactionRollbackIntegrationTest`의 실제 테스트 로그에서 기록했다. 각 시나리오는 로그가 아니라 Assertion으로 주문 수, 잔액, USE 이력 수 및 예외·지연 조건을 판정한다.

### 해결되지 않은 문제

- 외부 플랫폼의 2초 지연이 주문 트랜잭션과 요청 응답을 2,014ms까지 함께 지연시킨다.
- 외부 플랫폼 예외가 주문·포인트·USE 이력을 모두 Rollback시킨다. 외부 전송이 주문의 핵심 성공 조건이 아닌 경우에도 주문이 실패하는 결합이 남는다.
- 외부 호출은 요청 스레드와 동일한 `Test worker`에서 실행되므로 요청 처리와 후속 처리의 실행 흐름이 분리되지 않는다.

### 2단계에서 개선할 대상

동기 `@EventListener`로 `OrderService`가 외부 플랫폼 구현을 직접 호출하는 결합을 줄이는지 확인한다. 단, 동일 스레드·동기 실행이므로 지연과 예외가 주문 트랜잭션에 미치는 영향은 남는지 같은 조건으로 검증한다.

### 실행한 테스트 명령

```bash
./gradlew test --tests 'com.example.coffeeordersystem.service.OrderSynchronousExternalCallIntegrationTest' --tests 'com.example.coffeeordersystem.service.OrderTransactionRollbackIntegrationTest'
./gradlew test
./gradlew build
git diff --check
```

모든 명령은 성공했다.

### 테스트와 측정의 한계

- 외부 플랫폼은 실제 HTTP 서버가 아니라 테스트용 Stub이므로 네트워크 지연, 연결 실패, 타임아웃, 재시도는 측정하지 않는다.
- 소요 시간은 `OrderService.order()` 호출 구간이며 HTTP 요청·응답 직렬화나 실제 외부 네트워크 왕복 시간은 포함하지 않는다.
- 단일 테스트 실행값(4ms, 2,014ms, 20ms, 23ms)은 환경 부하에 따라 달라질 수 있으므로 성능 목표나 SLA로 해석하지 않는다.
- 동시 요청, 프로세스 종료, 메시지 유실·재시도는 이번 1단계 검증 범위가 아니다.

### 체크포인트 Commit

- Commit SHA: `f00cacc` (`test: reproduce synchronous order follow-up coupling`)

## 2단계: 일반 동기 Spring Event

### 구현 흐름

`OrderService.order()`는 포인트 차감, USE 이력 저장, 주문 저장 후 최소 주문 데이터만 담은 `OrderCompletedEvent`를 `ApplicationEventPublisher`로 발행한다. 일반 `@EventListener`인 `OrderCompletedEventListener`가 Event를 받아 `OrderDataPlatformClient`를 호출한다.

`@TransactionalEventListener`, `AFTER_COMMIT`, `@Async`는 사용하지 않았다. 따라서 Event 발행, Listener, Client가 주문 트랜잭션 안에서 즉시 실행된다.

### 실행 결과

| 조건 | 요청 / 발행 / Listener / Client 스레드 | `order()` 측정 시간 | 주문·포인트·USE 이력 DB 결과 | 결과 |
| --- | --- | ---: | --- | --- |
| 정상 Event | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 5ms | 주문 1건, 잔액 7,000, USE 이력 1건 | Listener·Client 각 1회 호출 후 Commit |
| Listener 내부 Client 2초 지연 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 2,008ms | 주문 1건, 잔액 7,000, USE 이력 1건 | Commit. Client 지연만큼 주문 처리도 지연 |
| Listener 내부 Client 예외 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 9ms | 주문 0건, 잔액 10,000, USE 이력 0건 | 예외 전파 및 Rollback |
| 주문 저장 예외 | `Test worker` / Event 발행 전 실패 / Event 발행 전 실패 / 호출 전 실패 | 6ms | 주문 0건, 잔액 10,000, USE 이력 0건 | Event·Listener·Client 미호출, Rollback |

### Event 처리 후 트랜잭션 Rollback

테스트 전용 외부 트랜잭션 Service가 실제 `OrderService.order()`를 호출한 뒤 강제 예외를 던져, production 코드 변경 없이 Event 전달 이후 Rollback을 재현했다.

| 조건 | 요청 / Event 발행 / Listener / Client 스레드 | 측정 시간 | 호출자·DB 결과 |
| --- | --- | ---: | --- |
| Event·Listener·Client 성공 후 강제 예외 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 21ms | `IllegalStateException` 전파, 주문 0건, 잔액 10,000, USE 이력 0건, Listener 1회·Client 1회 호출 |

동기 Listener와 외부 Client는 Event 발행 즉시 성공적으로 실행됐지만, 그 뒤 바깥 주문 트랜잭션이 Rollback되어 내부 주문·포인트·USE 이력은 남지 않았다. 외부 전송은 이미 실행됐으므로 내부 DB와 외부 시스템의 상태 불일치 가능성이 확인됐다. 이것이 3단계 AFTER_COMMIT이 필요한 핵심 이유다.

이 시나리오는 기존 주문 저장 실패와 다르다.

```text
Event 발행 전 주문 저장 실패
→ Listener 미실행
→ 전체 Rollback

Event 발행 및 Listener 성공 후 실패
→ 외부 Client 호출 완료
→ 내부 DB만 Rollback
→ 외부 시스템과 불일치 가능
```

### 1단계 대비

- 해결됨: `OrderService`는 `OrderDataPlatformClient` 구현을 직접 의존하거나 호출하지 않는다. 외부 전송 책임은 `OrderCompletedEventListener`로 이동했다.
- 남음: 네 실행 지점이 모두 같은 `Test worker` 스레드다. Client 지연은 주문 처리 시간을 2,008ms로 늘리고, Listener를 통해 전파된 Client 예외는 주문·포인트·USE 이력을 Rollback한다.

### 실행한 테스트 명령

```bash
./gradlew test --tests 'com.example.coffeeordersystem.service.OrderSynchronousEventIntegrationTest' --tests 'com.example.coffeeordersystem.service.OrderServiceTest' --tests 'com.example.coffeeordersystem.service.OrderTransactionRollbackIntegrationTest'
```

### 테스트와 측정의 한계

- 1단계와 마찬가지로 외부 플랫폼은 Mockito 기반 Stub이며 실제 HTTP 네트워크 조건은 검증하지 않는다.
- 소요 시간은 `OrderService.order()` 호출 구간의 단일 테스트 실행값이며 성능 목표나 SLA가 아니다.
- `AFTER_COMMIT`, 비동기 실행, 재시도·내구성·유실, 동시 요청은 아직 검증하지 않았다.

### 체크포인트 Commit

- Commit SHA: `c0af605` (`test: verify synchronous order event coupling`)
- 보강 Commit SHA: `ceeff7a` (`test: verify rollback after synchronous event delivery`)

## 3단계: `@TransactionalEventListener(AFTER_COMMIT)`

### 구현 흐름과 Commit 시점

`OrderService.order()`는 2단계와 같이 주문 완료 Event를 트랜잭션 안에서 발행한다. `OrderCompletedEventListener`는 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`로 등록되어 주문 Commit 이후에만 `OrderDataPlatformClient`를 호출한다.

테스트 관찰자는 AFTER_COMMIT Listener 실행 직전에 `orders` 테이블에서 해당 주문을 조회했다. 모든 성공 시나리오에서 `orderCommittedBeforeListener=true`였으므로 Listener 실행 전에 주문 DB Commit이 완료된 것을 확인했다.

관찰 시 `TransactionSynchronizationManager.isActualTransactionActive()`는 `true`였다. 이는 Spring의 트랜잭션 동기화 정리가 아직 끝나지 않은 상태를 나타내는 관찰값이며, 주문 DB Commit 이전에 Listener가 실행됐다는 뜻은 아니다. 이 단계의 Listener는 주문 Entity를 수정하거나 추가 DB 저장을 수행하지 않는다.

### 실행 결과

| 조건 | 요청 / 발행 / AFTER_COMMIT Listener / Client 스레드 | `order()` 측정 시간 | 호출자 결과 | 주문·포인트·USE 이력 DB 결과 |
| --- | --- | ---: | --- | --- |
| 정상 처리 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 2ms | 성공 응답 | 주문 1건, 잔액 7,000, USE 이력 1건 |
| Listener 내부 Client 2초 지연 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 2,016ms | 성공 응답 | Listener 이전 Commit 확인 후 주문 1건, 잔액 7,000, USE 이력 1건 |
| AFTER_COMMIT Client 예외 | `Test worker` / `Test worker` / `Test worker` / `Test worker` | 11ms | 성공 응답. `TransactionSynchronizationUtils`가 Listener 예외를 ERROR 로그로 기록 | 주문 1건, 잔액 7,000, USE 이력 1건 유지 |
| 주문 저장 예외 | `Test worker` / Event 발행 전 실패 / Event 발행 전 실패 / 호출 전 실패 | 3ms | `IllegalStateException` 전파 | 주문 0건, 잔액 10,000, USE 이력 0건. Listener·Client 미호출 |

### Event 발행 후 바깥 트랜잭션 Rollback

테스트 전용 외부 트랜잭션 Service가 실제 `OrderService.order()`를 호출해 주문 완료 Event가 발행된 뒤, 주문 Service가 정상 반환한 다음 강제 예외를 던졌다. 실행 로그에서 Event 발행은 같은 `Test worker` 스레드에서 관찰됐고, 호출자에게 `IllegalStateException`이 전파됐다.

그러나 최종 바깥 트랜잭션이 Rollback되어 Commit은 발생하지 않았다. 따라서 AFTER_COMMIT Listener와 외부 Client는 모두 0회 호출됐고, 주문은 0건, 잔액은 10,000, USE 이력은 0건이었다. 2단계에서 발생한 “외부 전송은 완료됐지만 내부 DB만 Rollback”되는 불일치는 이 조건에서 방지됐다.

| 조건 | Event 발행 | Listener / Client 호출 | 주문·포인트·USE 이력 DB 결과 | 결과 |
| --- | --- | --- | --- | --- |
| Event 발행 후 바깥 트랜잭션 강제 예외 | `Test worker`에서 발행 관찰 | 0회 / 0회 | 주문 0건, 잔액 10,000, USE 이력 0건 | `IllegalStateException` 전파 및 전체 Rollback |

### 2단계 대비

- 해결됨: 주문 저장 실패로 트랜잭션이 Rollback되면 AFTER_COMMIT Listener와 Client가 호출되지 않는다. 외부 Client 예외는 이미 Commit된 주문·포인트·USE 이력을 Rollback하지 못한다.
- 남음: `@Async`가 없으므로 네 실행 지점은 같은 `Test worker` 스레드이며, 2초 Client 지연은 `order()` 호출 흐름을 2,016ms까지 지연시킨다. Listener 실패에 대한 재시도·복구·내구성은 없다.
- 호출 결과와 DB 결과의 차이: 이번 환경에서는 AFTER_COMMIT Client 예외가 호출자에게 전파되지 않아 Service 호출은 성공했지만, 외부 전송은 실패했고 ERROR 로그만 남았다. DB 주문은 성공 상태로 유지됐다.

| 조건 | 2단계 동기 Event | 3단계 AFTER_COMMIT |
| --- | --- | --- |
| Event 발행 후 최종 트랜잭션 Rollback | Listener·Client 이미 실행, 내부 DB만 Rollback | Listener·Client 미실행, 내부 DB Rollback |

### 4단계에서 개선할 대상

`@Async`를 적용했을 때 요청 스레드와 AFTER_COMMIT Listener 스레드를 분리해 호출 흐름의 완료 시점을 외부 Client 지연과 분리할 수 있는지 확인한다. 단, 메모리 기반 비동기의 예외 관찰, 작업 거절, 재시도·내구성·유실 한계를 별도로 검증해야 한다.

### 실행한 테스트 명령

```bash
./gradlew test --tests 'com.example.coffeeordersystem.service.OrderAfterCommitEventIntegrationTest' --tests 'com.example.coffeeordersystem.service.OrderServiceTest' --tests 'com.example.coffeeordersystem.service.OrderTransactionRollbackIntegrationTest'
```

### 테스트와 측정의 한계

- 외부 플랫폼은 Mockito 기반 Stub이므로 실제 HTTP 연결·타임아웃·재시도를 검증하지 않는다.
- 소요 시간은 `OrderService.order()` 호출 구간의 단일 실행값이다. AFTER_COMMIT callback은 트랜잭션 interceptor의 Commit 처리 과정에서 같은 호출 흐름으로 실행되므로 이 측정에 포함된다.
- `isActualTransactionActive()` 관찰값만으로 Listener가 새 트랜잭션인지 여부를 판단할 수 없다. 이 실험에서는 Commit 가시성(`orderCommittedBeforeListener=true`)을 함께 확인했다.
- 서버 종료, 프로세스 장애, 후속 처리 유실, 동시 요청은 검증하지 않았다.

### 체크포인트 Commit

- Commit SHA: _3단계 체크포인트 Commit 후 기록_

## 4단계: `AFTER_COMMIT + @Async`

### 구현 흐름과 Executor

3단계의 `@TransactionalEventListener(phase = AFTER_COMMIT)`는 유지하고 Listener에 `@Async("orderFollowUpExecutor")`를 추가했다. 전용 `ThreadPoolTaskExecutor`는 운영 기본값으로 core 2, max 4, queue 100, `order-follow-up-` prefix를 사용한다. 포화 시 호출자 스레드를 대신 점유하지 않고 후속 작업 유실 가능성을 드러내기 위해 `AbortPolicy` 기반 즉시 거절을 명시했다. 포화 검증은 production 기본값을 줄이지 않고 테스트 속성 `1 / 1 / 1`로 분리했다.

### 실제 결과

| 조건 | 요청 / 발행 / Listener / Client 스레드 | 측정·DB 결과 |
| --- | --- | --- |
| 정상 | `Test worker` / `Test worker` / `order-follow-up-2` / `order-follow-up-2` | `order()` 3ms, 주문 1건·잔액 7,000·USE 1건 |
| Client 2초 지연 | `Test worker` / `Test worker` / `order-follow-up-1` / `order-follow-up-1` | `order()` 6ms, Client 실행 2,005ms. Client 완료 전에도 DB Commit 확인 |
| Client 예외 | `Test worker` / `Test worker` / `order-follow-up-1` / `order-follow-up-1` | 호출자 예외 없음, 테스트용 Client 예외 기록 장치가 `IllegalStateException` 확인, DB Commit 유지 |
| Event 후 바깥 Rollback | Event는 `Test worker`에서 관찰 | Listener·Client 0회, 주문 0건·잔액 10,000·USE 0건 |
| Executor 포화 | 테스트 전용 `1 / 1 / 1` | 첫 작업 실행·둘째 큐 대기 후 셋째 Event 거절. 주문 3건은 Commit, Client는 2회만 실행 |

비동기 예외는 반환값 없는 `@Async` 메서드의 예외 처리기가 `[ASYNC]` 로그로 기록하며, 테스트는 Client Stub이 실제 비동기 스레드에서 던진 예외 객체를 기록하고 Assertion으로 확인했다. 자동 재시도는 없다.

### 3단계 대비와 한계

| 항목 | 3단계 AFTER_COMMIT | 4단계 AFTER_COMMIT + Async |
| --- | --- | --- |
| 주문 Commit 이후 실행 | O | O |
| 요청·Listener 스레드 | 동일 | 분리 |
| 2초 Client 지연 | 요청 흐름 지연 | 요청 흐름과 분리 |
| Client 예외 | 주문 Commit 유지 | 주문 Commit 유지 |
| 호출자 예외 영향 | 일반적으로 직접 전파되지 않음 | 전파되지 않음 |
| 작업 유실 가능성 | 존재 | 존재 |
| Executor 포화 | 해당 없음 | 작업 거절 가능 |
| 재시도·복구 | 없음 | 없음 |

`@Async`는 Commit 이후 실행과 요청 지연 분리는 제공하지만 전달 보장·재시도·복구를 제공하지 않는다. 포화 시 세 번째 주문 DB는 Commit됐지만 외부 전송은 거절되어 유실될 수 있었다. 이 내구성·재처리 문제는 후속 Issue #46 Transactional Outbox에서 다룰 대상이다.

### 단계별 체크포인트와 최종 비교

- 1단계: `f00cacc`
- 2단계: `c0af605`
- 2단계 보강: `ceeff7a`
- 3단계: `d31d73a`
- 4단계: `cbdec83`

| 단계 | 실행 방식 | Commit 경계 | 요청 스레드 영향 | 외부 실패 영향 | 남은 한계 |
| --- | --- | --- | --- | --- | --- |
| 1단계 | Client 직접 동기 호출 | 주문 트랜잭션 내부 | 지연됨 | 주문 Rollback | 코드·실행 결합 |
| 2단계 | 동기 Event | 주문 트랜잭션 내부 | 지연됨 | 주문 Rollback | 발행 후 Rollback 시 외부 불일치 |
| 3단계 | AFTER_COMMIT | Commit 이후, 같은 스레드 | 지연됨 | 주문 Commit 유지 | 재시도·복구 없음 |
| 4단계 | AFTER_COMMIT + Async | Commit 이후, 별도 스레드 | 분리됨 | 주문 Commit 유지 | 포화·종료·실패 시 유실 가능 |

최종 유지 구조는 다음과 같다.

```text
ApplicationEventPublisher
→ OrderCompletedEvent
→ @TransactionalEventListener(AFTER_COMMIT)
→ @Async("orderFollowUpExecutor")
→ OrderDataPlatformClient
```

`AFTER_COMMIT`은 주문 Commit 성공 후에만 Listener를 실행하도록 보장한다. `@Async`는 Listener 실행 스레드를 분리하지만 전달 보장이나 재시도를 제공하지 않는다. `REQUIRES_NEW`는 별도 DB 트랜잭션 전파 방식일 뿐 외부 호출 전달 보장이나 요청 스레드 분리를 제공하지 않으므로 이 실험에는 적용하지 않았다.

## Issue #46 Transactional Outbox 전환

4단계 Async는 Executor 포화·서버 종료·외부 실패에서 메모리 작업이 유실될 수 있었다. 최종 production 경로는 Async Listener 대신 주문·포인트·USE 이력·Outbox `PENDING`을 같은 주문 트랜잭션으로 저장하고, 단일 인스턴스 `@Scheduled` Publisher가 DB의 PENDING을 조회해 외부 Client를 호출하도록 전환했다.

성공 시 `SENT`와 `processedAt`, 실패 시 `retryCount`와 `lastError`를 기록하며 최대 재시도 도달 시 `FAILED`가 된다. 서버 종료 뒤에도 DB PENDING은 다음 Publisher 실행에서 다시 조회할 수 있다. 다만 외부 전송 성공 후 SENT 갱신 전 장애에서는 중복 전송될 수 있어 Exactly Once는 보장하지 않는다. Kafka·RabbitMQ, 멀티 인스턴스 선점, PROCESSING/SKIP LOCKED, 재시도 백오프는 제외한다.

기존 Async 단계별 실험 기록은 학습 근거로 유지한다. Async 테스트 삭제는 테스트 통과 목적이 아니라 production 전송 경로를 Outbox 하나로 통일해 동일 주문의 중복 전송을 제거한 데 따른 정상적인 정리다.

Outbox `eventId`는 주문 API 중복 요청을 막는 키가 아니라, 같은 Outbox 이벤트의 재시도 전달을 외부 Consumer가 식별하기 위한 키다. Publisher는 Entity와 JSON Payload의 동일 `eventId`를 Payload 전체와 함께 Client에 전달하며 재시도에서도 새 UUID를 만들지 않는다. Consumer는 이를 멱등성 키로 저장·검사해야 하지만, Consumer 저장소 구현은 이번 범위에서 제외한다. 따라서 현재 Producer 보장은 동일 이벤트를 같은 `eventId`로 전달하는 계약까지이며 Exactly Once는 보장하지 않는다.
