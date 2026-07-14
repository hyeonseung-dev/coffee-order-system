# 주문 후속 처리 방식 실험 기록

이 문서는 주문 완료 후 외부 데이터 플랫폼으로 전달하는 방식을 단계별로 비교하는 실험 기록이다. 각 단계는 별도 Human 승인 후에만 추가하며, 결과는 실제 실행한 테스트와 측정 로그만 기록한다.

## 실험 목적

주문 후속 처리를 주문 트랜잭션과 어떤 시점·스레드에서 분리할지 판단하기 위해, 직접 동기 호출부터 단계적으로 지연·예외·Rollback 영향을 확인한다.

이번 기록은 Issue #28의 1단계만 다룬다. 2단계 동기 Event, 3단계 `AFTER_COMMIT`, 4단계 `AFTER_COMMIT + @Async`는 아직 구현하거나 측정하지 않았다.

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

- Commit SHA: _미기록 — 이번 단계는 Commit을 수행하지 않음_

