# ERD 설계 문서

이 문서는 포인트 기반 커피 주문 시스템의 핵심 테이블, 관계, 인덱스와 데이터 원본 정책을 정의한다.

- 정확한 원본 저장소는 MySQL Primary다.
- MySQL Replica는 readOnly 조회용 비동기 복제본이다.
- Redis는 인기 메뉴 조회 결과 캐시이며 ERD의 원본 엔티티가 아니다.
- 주문 후속 전송은 `outbox_events`에 저장된 이벤트를 기준으로 처리한다.

## 1. ERD

```mermaid
erDiagram
    USERS ||--|| POINT_WALLET : owns
    USERS ||--o{ POINT_HISTORY : has
    USERS ||--o{ ORDERS : places
    MENU ||--o{ ORDERS : ordered_as
    ORDERS ||--o| OUTBOX_EVENTS : publishes

    USERS {
        BIGINT id PK
        VARCHAR name
        DATETIME created_at
        DATETIME updated_at
    }
    POINT_WALLET {
        BIGINT id PK
        BIGINT user_id FK UK
        BIGINT balance
        DATETIME created_at
        DATETIME updated_at
    }
    POINT_HISTORY {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT amount
        VARCHAR type
        BIGINT balance_after
        DATETIME created_at
    }
    MENU {
        BIGINT id PK
        VARCHAR name
        BIGINT price
        VARCHAR status
        DATETIME created_at
        DATETIME updated_at
    }
    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT menu_id FK
        BIGINT order_price
        VARCHAR status
        DATETIME ordered_at
    }
    OUTBOX_EVENTS {
        BIGINT id PK
        VARCHAR event_id UK
        VARCHAR event_type
        BIGINT aggregate_id
        TEXT payload
        VARCHAR status
        INT retry_count
        VARCHAR last_error
        DATETIME created_at
        DATETIME processed_at
    }
```

`outbox_events.aggregate_id`에는 주문 ID가 저장되지만 현재 JPA Entity에서 `Order` 연관관계 FK로 매핑하지 않는다. 이벤트 재처리에 필요한 aggregate 식별값으로 관리한다.

## 2. 테이블 정의

### 2.1 `users`

사용자 기본 정보를 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| `name` | VARCHAR(100) | NOT NULL | 사용자 이름 |
| `created_at` | DATETIME | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME | NOT NULL | 수정 시각 |

### 2.2 `point_wallet`

사용자의 현재 포인트 잔액을 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 지갑 ID |
| `user_id` | BIGINT | FK, UNIQUE, NOT NULL | 사용자 ID |
| `balance` | BIGINT | NOT NULL | 현재 잔액 |
| `created_at` | DATETIME | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME | NOT NULL | 수정 시각 |

`point_wallet`은 주문·충전 시 `PESSIMISTIC_WRITE` 잠금 대상이다.

### 2.3 `point_history`

충전과 사용 이력을 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| `user_id` | BIGINT | FK, NOT NULL | 사용자 ID |
| `amount` | BIGINT | NOT NULL | 충전 또는 사용 금액 |
| `type` | VARCHAR(20) | NOT NULL | `CHARGE` 또는 `USE` |
| `balance_after` | BIGINT | NOT NULL | 반영 후 잔액 |
| `created_at` | DATETIME | NOT NULL | 생성 시각 |

### 2.4 `menu`

커피 메뉴 정보를 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 메뉴 ID |
| `name` | VARCHAR(100) | NOT NULL | 메뉴 이름 |
| `price` | BIGINT | NOT NULL | 메뉴 가격 |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE` 또는 `INACTIVE` |
| `created_at` | DATETIME | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME | NOT NULL | 수정 시각 |

### 2.5 `orders`

포인트 결제가 완료된 주문만 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| `user_id` | BIGINT | FK, NOT NULL | 주문 사용자 ID |
| `menu_id` | BIGINT | FK, NOT NULL | 주문 메뉴 ID |
| `order_price` | BIGINT | NOT NULL | 주문 당시 가격 |
| `status` | VARCHAR(20) | NOT NULL | 현재 `COMPLETED` |
| `ordered_at` | DATETIME | NOT NULL | UTC 주문 완료 시각 |

최종 인기 메뉴 인덱스:

```sql
CREATE INDEX idx_orders_menu_ordered_at_status
ON orders (menu_id, ordered_at, status);
```

이 인덱스는 실제 ACTIVE 메뉴 LEFT JOIN 쿼리의 `menu_id` 조회 경로와 Projection에 필요한 `ordered_at`, `status`를 함께 제공하는 covering index다. 현재 모든 주문이 `COMPLETED`라 상태 선택도는 없으며 상태 모델이 확장되면 재측정한다.

### 2.6 `outbox_events`

주문 완료 후 외부 데이터 플랫폼에 전달할 이벤트를 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | 내부 ID |
| `event_id` | VARCHAR(36) | UNIQUE, NOT NULL | 외부 멱등성에 사용할 이벤트 ID |
| `event_type` | VARCHAR(50) | NOT NULL | 현재 `ORDER_COMPLETED` |
| `aggregate_id` | BIGINT | NOT NULL | 주문 ID |
| `payload` | TEXT | NOT NULL | 주문 완료 JSON Payload |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `SENT`, `FAILED` |
| `retry_count` | INT | NOT NULL | 전송 실패 횟수 |
| `last_error` | VARCHAR(1000) | NULL | 마지막 실패 원인 |
| `created_at` | DATETIME | NOT NULL | UTC 생성 시각 |
| `processed_at` | DATETIME | NULL | UTC 전송 완료 시각 |

Publisher 조회 인덱스:

```sql
CREATE INDEX idx_outbox_events_status_created_at
ON outbox_events (status, created_at);
```

`PENDING` 이벤트를 생성 시각 오름차순으로 조회하는 경로에 사용한다.

## 3. 관계

```text
users 1 : 1 point_wallet
users 1 : N point_history
users 1 : N orders
menu  1 : N orders
orders 1 : 0..1 outbox_events  (논리 관계, JPA FK 연관관계 미매핑)
```

- 한 사용자는 하나의 포인트 지갑을 가진다.
- 한 사용자는 여러 포인트 이력과 주문을 가질 수 있다.
- 하나의 메뉴는 여러 주문에서 참조될 수 있다.
- 현재 주문 성공 시 하나의 `ORDER_COMPLETED` Outbox 이벤트를 저장한다.

## 4. 설계 의도

### 4.1 `users`와 `point_wallet` 분리

사용자 기본 정보와 포인트 잔액 변경 책임을 분리한다. 주문·충전 시 사용자 전체가 아니라 지갑 행만 명확한 잠금 대상으로 삼을 수 있다.

### 4.2 `point_wallet`을 비관적 락 대상으로 선택

동일 사용자의 동시 요청이 같은 잔액을 읽고 덮어쓰는 Lost Update를 방지한다. 원본이 MySQL이므로 Primary 지갑 행을 잠그고 잔액 검증과 변경을 같은 트랜잭션에서 직렬화한다.

Replica에서 얻은 lock은 Primary 행을 보호하지 않으며 복제 지연 잔액일 수 있으므로 정합성 경로에 사용하지 않는다.

### 4.3 `point_history`의 감사·검증 역할

지갑은 현재 잔액만 가진다. `point_history`는 충전·사용 순서, 금액과 변경 후 잔액을 보존해 다음을 확인할 수 있게 한다.

- 성공 주문 수와 `USE` 이력 수 일치
- 충전 결과와 `CHARGE` 이력 일치
- 최종 잔액 계산과 실제 잔액 비교

### 4.4 성공 주문만 `orders`에 저장

인기 메뉴와 주문 내역의 의미를 결제 완료 주문으로 고정한다. 사용자 없음, 비활성 메뉴, 잔액 부족 등의 실패 요청은 저장하지 않는다.

향후 취소·환불·결제 대기 상태를 도입하면 `orders` 상태 모델과 집계·인덱스를 다시 설계해야 한다.

### 4.5 인기 메뉴의 원본을 `orders`로 유지

Redis에 주문 수를 직접 증가시키면 DB Commit과 Redis 업데이트 사이에 불일치가 생긴다. 따라서 완료 주문 수의 정확한 원본은 `orders`로 두고 Redis는 완성된 조회 결과만 캐시한다.

### 4.6 `outbox_events`를 별도 테이블로 둔 이유

외부 전송 성공 여부는 주문 상태와 다른 수명주기를 가진다.

- 주문은 이미 완료됐지만 외부 전송은 재시도 중일 수 있다.
- 주문을 Rollback하지 않고 전송 실패 원인과 횟수를 보존해야 한다.
- 서버 재시작 후에도 DB의 `PENDING` 이벤트를 다시 조회해야 한다.

주문과 Outbox 이벤트는 같은 트랜잭션에서 저장하지만, 외부 전송 상태는 `outbox_events`가 독립적으로 관리한다.

## 5. 데이터 원본과 복제본

| 데이터 | 정확성 원본 | 복제·캐시 | 사용 제한 |
|---|---|---|---|
| 지갑 잔액 | MySQL Primary | MySQL Replica | 잔액 검증·lock은 Primary만 |
| 포인트 이력 | MySQL Primary | MySQL Replica | 쓰기는 Primary |
| 주문 | MySQL Primary | MySQL Replica | 인기 메뉴 원본은 Primary에서 복제된 orders |
| Outbox | MySQL Primary | MySQL Replica | Publisher 상태 변경은 Primary |
| 인기 메뉴 결과 | MySQL 집계 | Redis | Redis 결과는 TTL 동안 stale 가능 |

MySQL Replica와 Redis는 비동기·캐시 복사본이므로 최신 정합성이 필요한 판단에 사용하지 않는다.

## 6. 트랜잭션별 변경 테이블

### 포인트 충전

```text
point_wallet UPDATE
+ point_history(CHARGE) INSERT
```

### 주문·결제

```text
point_wallet SELECT FOR UPDATE / UPDATE
+ point_history(USE) INSERT
+ orders(COMPLETED) INSERT
+ outbox_events(PENDING) INSERT
```

주문 트랜잭션이 실패하면 네 변경을 모두 Rollback한다.

### Outbox 발행

```text
outbox_events PENDING 조회
→ 외부 전송
→ SENT 또는 retryCount·lastError·FAILED 갱신
```

외부 전송은 주문 트랜잭션과 분리되어 주문 완료 데이터를 되돌리지 않는다.

## 7. 시간대 정책

- `orders.ordered_at`, `outbox_events.created_at`, `processed_at`: UTC `Instant`
- JDBC·Hibernate: UTC
- API 주문 시각: KST 변환
- 인기 메뉴 기간과 Redis Key: KST 업무 날짜

DB의 DATETIME 표현만 보고 시스템 기본 시간대를 추정하지 않고 애플리케이션·JDBC 설정과 변환 정책을 함께 적용한다.

## 8. 알려진 한계

- `outbox_events.aggregate_id`에 DB FK가 없어 주문 삭제·변경 정책은 애플리케이션 계약에 의존한다.
- 현재 주문 상태는 `COMPLETED`만 존재한다.
- Outbox 이벤트는 현재 주문당 1개라는 애플리케이션 규칙이며 `aggregate_id + event_type` UNIQUE 제약은 없다.
- Consumer 멱등 저장소가 없어 Exactly Once를 보장하지 않는다.
- JPA `ddl-auto=update`를 사용하며 운영 Migration 도구는 도입하지 않았다.
- 복제 지연과 캐시 stale을 허용할 수 없는 기능은 Primary에서 직접 조회해야 한다.
