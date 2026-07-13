# API 명세서

이 문서는 포인트 기반 커피 주문 시스템의 v0 필수 API를 정의한다.

현재 단계에서는 실제 PG/포트원 연동을 하지 않고, 사용자의 보유 포인트를 차감하는 방식으로 주문/결제를 처리한다. Redis, RabbitMQ, MySQL Replication은 현재 API 구현의 필수 전제가 아니며, 이후 단계의 도전 과제로 분리한다.

---

## 1. 커피 메뉴 목록 조회 API

### API 이름

커피 메뉴 목록 조회

### Method

`GET`

### Endpoint

`/api/menus`

### Description

주문 가능한 커피 메뉴 목록을 조회한다.

### Path Variable

없음

### Query Parameter

없음

### Request Body

없음

### Response Body

```json
{
  "data": [
    {
      "menuId": 1,
      "name": "Americano",
      "price": 3000
    },
    {
      "menuId": 2,
      "name": "Latte",
      "price": 4000
    }
  ]
}
```

### Error Response

현재 단계에서 별도 비즈니스 예외는 정의하지 않는다.

### 비즈니스 규칙

- 메뉴 목록 조회는 포인트 잔액이나 주문 상태를 변경하지 않는다.
- 메뉴 응답에는 `menuId`, `name`, `price`를 포함한다.
- v3 읽기/쓰기 분리 단계에서는 Replica 조회 대상으로 검토할 수 있다.

### 검증 포인트

- 응답 데이터에 메뉴 식별값, 이름, 가격이 포함되는지 확인한다.
- 비활성 메뉴 노출 여부는 구현 단계의 정책으로 명확히 결정한다.
- 조회 API가 데이터 변경을 수행하지 않는지 확인한다.

---

## 2. 포인트 충전 API

### API 이름

포인트 충전

### Method

`POST`

### Endpoint

`/api/users/{userId}/points/charge`

### Description

사용자 식별값을 기준으로 포인트를 충전한다.

### Path Variable

| 이름 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `userId` | Long | 필수 | 포인트를 충전할 사용자 ID |

### Query Parameter

없음

### Request Body

```json
{
  "amount": 10000
}
```

### Response Body

```json
{
  "data": {
    "userId": 1,
    "chargedAmount": 10000,
    "balance": 10000
  }
}
```

### Error Response

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

```json
{
  "code": "INVALID_CHARGE_AMOUNT",
  "message": "충전 금액은 0보다 커야 합니다."
}
```

### 비즈니스 규칙

- `amount`는 0보다 커야 한다.
- 사용자 식별값이 필요하다.
- 충전 성공 시 `point_wallet.balance`가 증가한다.
- 충전 이력은 `point_history`에 `CHARGE` 타입으로 저장한다.
- 포인트 잔액 변경은 원본 저장소인 MySQL 기준으로 처리한다.

### 검증 포인트

- 존재하지 않는 사용자 요청은 실패해야 한다.
- 0 이하의 충전 금액은 실패해야 한다.
- 충전 성공 후 잔액이 요청 금액만큼 증가해야 한다.
- 충전 성공 후 `point_history`에 `CHARGE` 이력이 남아야 한다.

---

## 3. 커피 주문/결제 API

### API 이름

커피 주문/결제

### Method

`POST`

### Endpoint

`/api/orders`

### Description

사용자가 보유한 포인트로 커피 메뉴 1개를 주문하고 결제한다.

### Path Variable

없음

### Query Parameter

없음

### Request Body

```json
{
  "userId": 1,
  "menuId": 1
}
```

### Response Body

```json
{
  "data": {
    "orderId": 1,
    "userId": 1,
    "menuId": 1,
    "orderPrice": 3000,
    "remainingBalance": 7000,
    "orderedAt": "2026-07-08T20:00:00"
  }
}
```

### Error Response

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

```json
{
  "code": "MENU_NOT_FOUND",
  "message": "메뉴를 찾을 수 없습니다."
}
```

```json
{
  "code": "INACTIVE_MENU",
  "message": "비활성 메뉴는 주문할 수 없습니다."
}
```

```json
{
  "code": "INSUFFICIENT_POINT",
  "message": "포인트 잔액이 부족합니다."
}
```

### 비즈니스 규칙

- 수량은 1개 고정이다.
- 하나의 주문은 하나의 메뉴만 주문한다.
- 주문 실패 시 `orders`에 저장하지 않는다.
- 성공한 주문만 `orders`에 저장한다.
- 포인트가 부족하면 주문은 실패한다.
- 주문 성공 시 `point_wallet.balance`를 차감한다.
- 주문 성공 시 `point_history`에 `USE` 타입으로 저장한다.
- 주문 성공 시 `OrderCompletedEvent` 발행 대상으로 본다.
- 실제 PG/포트원 연동은 하지 않는다.
- 잔액 검증, 포인트 차감, 포인트 사용 이력 저장, 주문 저장은 하나의 트랜잭션에서 처리한다.

### 검증 포인트

- 존재하지 않는 사용자의 주문은 실패해야 한다.
- 존재하지 않는 메뉴의 주문은 실패해야 한다.
- 비활성 메뉴의 주문은 실패해야 한다.
- 잔액이 메뉴 가격보다 적으면 주문은 실패해야 한다.
- 주문 실패 시 `orders`와 `point_history`에 사용 이력이 저장되지 않아야 한다.
- 주문 성공 시 잔액이 메뉴 가격만큼 차감되어야 한다.
- 주문 성공 시 `orders`에는 성공 주문만 저장되어야 한다.
- Issue #8에서는 단일 요청의 트랜잭션 원자성과 실패 시 전체 Rollback을 검증한다. 동시 주문 문제 재현은 #10, DB 비관적 락 적용과 정합성 검증은 #11에서 수행한다.

---

## 4. 최근 7일 인기 메뉴 TOP 3 조회 API

### API 이름

최근 7일 인기 메뉴 TOP 3 조회

### Method

`GET`

### Endpoint

`/api/menus/popular`

### Description

조회 기준 시각으로부터 최근 7일간의 성공 주문을 메뉴별로 집계하여 주문 횟수가 많은 메뉴를 최대 3개 조회한다.

### Path Variable

없음

### Query Parameter

없음

> 과제 요구사항이 최근 7일과 TOP 3로 고정되어 있으므로 v0에서는 `days`, `limit`을 외부 파라미터로 노출하지 않는다.

### Request Body

없음

### Response Body

```json
{
  "data": [
    {
      "menuId": 1,
      "name": "Americano",
      "orderCount": 15
    },
    {
      "menuId": 2,
      "name": "Latte",
      "orderCount": 10
    },
    {
      "menuId": 3,
      "name": "Mocha",
      "orderCount": 7
    }
  ]
}
```

### Error Response

현재 단계에서 별도 비즈니스 예외는 정의하지 않는다. 집계 대상 주문이 없으면 `data`에 빈 목록을 반환한다.

### 비즈니스 규칙

- 조회 요청마다 기준 시각을 한 번만 계산한다.
- 조회 범위는 `기준 시각 - 7일` 이상, 기준 시각 이하로 한다.
- 정확히 7일 전 생성된 주문은 집계에 포함한다.
- `OrderStatus.COMPLETED`인 주문만 집계한다.
- 현재 구조에서는 실패 주문이 `orders`에 저장되지 않지만, 조회 조건에도 완료 상태를 명시한다.
- `orders` 테이블을 정확한 원본 데이터로 사용한다.
- 메뉴별 주문 횟수를 집계한다.
- 주문 횟수 내림차순으로 정렬한다.
- 주문 횟수가 같으면 `menuId` 오름차순으로 정렬한다.
- 정렬 결과에서 최대 3개만 반환한다.
- `orderCount`의 응답 타입은 `Long`으로 한다.
- Redis는 v2에서 조회 결과 캐시로만 사용한다.
- Redis를 원본 랭킹 저장소로 사용하지 않는다.

### 구현 방향

- 애플리케이션에서 조회 기준 시각을 계산하고 조회 시작·종료 시각을 Repository에 전달한다.
- 테스트 가능한 시간 계산을 위해 `Clock` 주입을 허용한다.
- JPQL 집계 쿼리와 Projection을 사용한다.
- 조회 개수 제한은 `Pageable`을 이용해 3개로 고정한다.
- Native Query와 QueryDSL은 이번 범위에서 사용하지 않는다.

### 기본 JPQL 형태

```text
SELECT m.id, m.name, COUNT(o.id)
FROM Order o
JOIN o.menu m
WHERE o.status = COMPLETED
  AND o.orderedAt >= :fromInclusive
  AND o.orderedAt <= :toInclusive
GROUP BY m.id, m.name
ORDER BY COUNT(o.id) DESC, m.id ASC
```

### 검증 포인트

- 최근 7일 범위의 완료 주문만 집계되는지 확인한다.
- 정확히 7일 전 주문이 포함되는지 확인한다.
- 7일보다 오래된 주문은 제외되는지 확인한다.
- 기준 시각 이후 주문은 제외되는지 확인한다.
- 메뉴별 주문 횟수가 정확한지 확인한다.
- 상위 3개만 반환되는지 확인한다.
- 주문 횟수가 같을 때 `menuId` 오름차순으로 반환되는지 확인한다.
- 집계 대상 주문이 없을 때 빈 목록을 반환하는지 확인한다.
- Redis 캐시 적용 후에도 원본 집계 기준은 MySQL `orders`인지 확인한다.
