# ERD 설계 문서

이 문서는 포인트 기반 커피 주문 시스템의 핵심 테이블과 관계를 정의한다.

현재 단계의 원본 데이터 저장소는 MySQL이다. Redis는 이후 단계에서 인기 메뉴 조회 성능 개선을 위한 캐시로만 사용하며, 원본 랭킹 저장소로 사용하지 않는다.

---

## 1. 테이블 목록

### 1-1. users

사용자 기본 정보를 저장한다.

| 컬럼명 | 타입 초안 | 제약 조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| `name` | VARCHAR(100) | NOT NULL | 사용자 이름 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

### 1-2. point_wallet

사용자의 현재 포인트 잔액을 저장한다.

| 컬럼명 | 타입 초안 | 제약 조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 포인트 지갑 ID |
| `user_id` | BIGINT | FK, UNIQUE, NOT NULL | 사용자 ID |
| `balance` | BIGINT | NOT NULL | 현재 포인트 잔액 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

### 1-3. point_history

포인트 충전과 사용 이력을 저장한다.

| 컬럼명 | 타입 초안 | 제약 조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 포인트 이력 ID |
| `user_id` | BIGINT | FK, NOT NULL | 사용자 ID |
| `amount` | BIGINT | NOT NULL | 충전 또는 사용 금액 |
| `type` | VARCHAR(20) | NOT NULL | 포인트 이력 타입, `CHARGE` 또는 `USE` |
| `balance_after` | BIGINT | NOT NULL | 이력 반영 후 잔액 |
| `created_at` | DATETIME | NOT NULL | 이력 생성 일시 |

### 1-4. menu

커피 메뉴 정보를 저장한다.

| 컬럼명 | 타입 초안 | 제약 조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 메뉴 ID |
| `name` | VARCHAR(100) | NOT NULL | 메뉴 이름 |
| `price` | BIGINT | NOT NULL | 메뉴 가격 |
| `status` | VARCHAR(20) | NOT NULL | 메뉴 상태, `ACTIVE` 또는 `INACTIVE` |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

### 1-5. orders

성공한 주문 정보를 저장한다.

| 컬럼명 | 타입 초안 | 제약 조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| `user_id` | BIGINT | FK, NOT NULL | 주문 사용자 ID |
| `menu_id` | BIGINT | FK, NOT NULL | 주문 메뉴 ID |
| `order_price` | BIGINT | NOT NULL | 주문 당시 메뉴 가격 |
| `status` | VARCHAR(20) | NOT NULL | 주문 상태, 현재 단계에서는 `COMPLETED` |
| `ordered_at` | DATETIME | NOT NULL | 주문 완료 일시 |

---

## 2. 관계

```text
users 1 : 1 point_wallet
users 1 : N point_history
users 1 : N orders
menu  1 : N orders
```

- 한 사용자는 하나의 포인트 지갑을 가진다.
- 한 사용자는 여러 포인트 이력을 가질 수 있다.
- 한 사용자는 여러 주문을 생성할 수 있다.
- 하나의 메뉴는 여러 주문에서 참조될 수 있다.

---

## 3. 설계 의도

### users와 point_wallet을 분리한 이유

`users`는 사용자 기본 정보의 책임을 가진다. `point_wallet`은 포인트 잔액과 잔액 변경의 책임을 가진다.

두 책임을 분리하면 주문/결제 시 포인트 잔액 row만 명확하게 잠금 대상으로 삼을 수 있고, 사용자 정보 변경과 포인트 정합성 처리를 분리해서 설명할 수 있다.

### point_wallet이 비관적 락 대상인 이유

동일 사용자가 동시에 여러 주문을 요청하면 같은 잔액을 기준으로 중복 차감이 발생할 수 있다.

포인트 차감의 원본 데이터는 MySQL의 `point_wallet.balance`이므로, 주문/결제 트랜잭션에서는 해당 사용자의 `point_wallet` row를 비관적 락으로 조회해 잔액 검증과 차감을 직렬화한다.

### point_history가 정합성 검증에 필요한 이유

`point_wallet`은 현재 잔액만 가진다. 충전과 사용이 어떤 순서로 발생했는지, 각 처리 후 잔액이 얼마였는지는 `point_history`가 있어야 검증할 수 있다.

따라서 포인트 충전 성공 시 `CHARGE`, 주문 성공 시 `USE` 이력을 저장하고, `balance_after`를 통해 잔액 변경 결과를 추적한다.

### orders에는 성공 주문만 저장하는 이유

인기 메뉴 집계와 주문 내역의 기준은 성공한 주문이다.

실패 주문까지 `orders`에 저장하면 집계 시 실패 상태를 계속 제외해야 하고, 주문 데이터의 의미가 흐려질 수 있다. 현재 설계에서는 포인트 부족, 비활성 메뉴, 사용자 없음 등의 실패는 주문으로 저장하지 않는다.

### 인기 메뉴 집계 원본을 orders로 보는 이유

인기 메뉴는 실제로 완료된 주문 수를 기준으로 계산해야 한다.

`orders`에는 성공 주문만 저장되므로 최근 7일 인기 메뉴 TOP 3 집계의 원본 데이터로 적합하다. 메뉴별 주문 횟수는 `orders.menu_id` 기준으로 집계한다.

### Redis를 원본 랭킹 저장소로 사용하지 않는 이유

Redis에 랭킹 카운트를 직접 누적하면 DB 주문 저장과 Redis 증가 사이에 불일치가 생길 수 있다.

예를 들어 DB 주문 저장은 성공했지만 Redis 증가가 실패할 수 있고, 반대로 Redis 증가 후 DB 트랜잭션이 롤백될 수도 있다. Redis 장애나 재시작으로 데이터가 유실될 가능성도 있다.

따라서 정확한 원본은 MySQL `orders`로 두고, Redis는 v2에서 조회 성능 개선을 위한 캐시로만 사용한다.

---

## 4. 제외 범위와 도전 과제

- v0에서는 Entity, Repository, Docker Compose, Redis, RabbitMQ, MySQL Replication 설정을 만들지 않는다.
- v1에서는 DB 비관적 락과 동시 주문 테스트로 포인트 정합성을 검증한다.
- v2에서는 인덱스와 Redis 캐시를 적용하되, Redis는 원본 데이터가 아니라 캐시로만 사용한다.
- v3에서는 MySQL Replication, Redis Master-Replica, RabbitMQ를 도전 과제로 검토한다.
