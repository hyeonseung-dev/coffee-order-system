# API 명세서

이 문서는 포인트 기반 커피 주문 시스템의 외부 HTTP 계약을 정의한다.

- 실제 PG·포트원은 연동하지 않고 보유 포인트로 결제한다.
- MySQL Primary·Replica와 Redis Sentinel은 내부 인프라 구현이며 API 요청·응답 형식을 변경하지 않는다.
- Redis는 인기 메뉴 캐시이며 정확성 원본은 MySQL `orders`다.
- 주문 완료 후속 처리는 현재 Transactional Outbox를 사용한다.

---

## 1. 커피 메뉴 목록 조회

### 요청

```http
GET /api/menus
```

Path Variable, Query Parameter, Request Body는 없다.

### 성공 응답

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

### 비즈니스 규칙

- `ACTIVE` 메뉴만 반환한다.
- 메뉴 ID 오름차순으로 반환한다.
- 응답 DTO는 `menuId`, `name`, `price`를 포함한다.
- 메뉴가 없으면 예외가 아니라 빈 배열을 반환한다.
- 데이터 변경을 수행하지 않는다.
- 활성 readOnly 트랜잭션으로 실행되어 MySQL Replica 조회 대상이다.

### 검증 포인트

- ACTIVE 메뉴만 노출되는가?
- ID 순서가 유지되는가?
- Entity가 직접 노출되지 않는가?
- 0건일 때 `data: []`인가?

---

## 2. 포인트 충전

### 요청

```http
POST /api/users/{userId}/points/charge
Content-Type: application/json
```

| 이름 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| `userId` | Path | Long | 예 | 충전 대상 사용자 ID |
| `amount` | Body | Long | 예 | 충전 금액, 0보다 커야 함 |

```json
{
  "amount": 10000
}
```

### 성공 응답

```json
{
  "data": {
    "userId": 1,
    "chargedAmount": 10000,
    "balance": 10000
  }
}
```

### 오류 응답 예시

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

```json
{
  "code": "POINT_WALLET_NOT_FOUND",
  "message": "포인트 지갑을 찾을 수 없습니다."
}
```

### 비즈니스 규칙

- `amount`는 0보다 커야 한다.
- Primary MySQL의 사용자 지갑 행을 비관적 쓰기 락으로 조회한다.
- 충전 성공 시 `point_wallet.balance`가 증가한다.
- `point_history`에 `CHARGE`, 충전 금액, 반영 후 잔액을 저장한다.
- 지갑 변경과 이력 저장은 하나의 트랜잭션에서 처리한다.
- 충전은 Redis와 Replica에 의존하지 않는다.

### 검증 포인트

- 존재하지 않는 사용자·지갑 요청은 실패하는가?
- 0 이하 금액은 실패하는가?
- 잔액과 `CHARGE` 이력이 함께 반영되는가?
- 실패 시 일부 데이터만 남지 않는가?

---

## 3. 커피 주문·결제

### 요청

```http
POST /api/orders
Content-Type: application/json
```

```json
{
  "userId": 1,
  "menuId": 1
}
```

### 성공 응답

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

`orderedAt`은 DB의 UTC `Instant`를 `Asia/Seoul`로 변환한 응답 시각이다.

### 오류 응답 예시

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

```json
{
  "code": "POINT_WALLET_NOT_FOUND",
  "message": "포인트 지갑을 찾을 수 없습니다."
}
```

### 비즈니스 규칙

- 수량은 1개로 고정한다.
- 하나의 주문은 하나의 메뉴만 참조한다.
- ACTIVE 메뉴만 주문할 수 있다.
- 주문 가능 여부는 Primary의 최신 `point_wallet.balance`로 판단한다.
- 지갑 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 잔액을 검증·차감한다.
- 성공 시 `point_history`에 `USE` 이력을 저장한다.
- 성공한 주문만 `orders`에 `COMPLETED`로 저장한다.
- 주문 시각은 UTC `Instant`로 저장한다.
- 주문 완료 Outbox 이벤트를 `PENDING` 상태로 같은 트랜잭션에 저장한다.
- 잔액 차감, `USE` 이력, 주문, Outbox는 함께 Commit 또는 Rollback한다.
- 외부 데이터 플랫폼 전송은 Commit 이후 별도 Publisher가 처리한다.
- 실제 PG는 연동하지 않는다.

### 내부 트랜잭션 흐름

```text
사용자 조회
→ 메뉴 조회·ACTIVE 검증
→ Primary 지갑 PESSIMISTIC_WRITE 조회
→ 잔액 검증·차감
→ USE 이력 저장
→ COMPLETED 주문 저장
→ Outbox PENDING 저장
→ Commit
```

### 검증 포인트

- 사용자·메뉴·지갑 없음과 비활성 메뉴가 실패하는가?
- 잔액 부족 시 지갑·USE 이력·주문·Outbox가 모두 원상태인가?
- 주문 성공 시 잔액, 주문, USE 이력과 Outbox 수가 일치하는가?
- 동일 사용자 10개 동시 요청에서 성공 3, 실패 7, 잔액 1,000P인가?
- Outbox Payload 생성 또는 저장 실패 시 주문 전체가 Rollback되는가?

---

## 4. 최근 7일 인기 메뉴 TOP 3

### 요청

```http
GET /api/menus/popular
```

Path Variable, Query Parameter, Request Body는 없다.

### 성공 응답

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

### 비즈니스 규칙

- 기준 시간대는 `Asia/Seoul`이다.
- 범위는 오늘의 7일 전 `00:00` 이상, 오늘 `00:00` 미만이다.
- 오늘 진행 중인 주문은 포함하지 않는다.
- `COMPLETED` 주문만 집계한다.
- `ACTIVE` 메뉴만 순위 후보다.
- 주문 0건 ACTIVE 메뉴도 `orderCount: 0`으로 후보에 포함한다.
- `orderCount` 내림차순, 동률이면 `menuId` 오름차순이다.
- ACTIVE 메뉴가 3개 미만이면 존재하는 메뉴만 반환한다.
- 후보가 없으면 빈 배열을 반환한다.
- `orderCount`는 Long 숫자로 반환한다.
- 정확한 원본은 MySQL `orders`다.

### 캐시 정책

```text
Key: popular:menus:7days:{KST businessDate}:v1
기본 TTL: 86,400초
```

- Hit: Redis 결과 반환, MySQL 집계 생략
- Miss: MySQL 조회 후 Redis 저장
- Redis 조회·저장·역직렬화 실패: 경고 로그 후 MySQL 결과 반환
- 캐시가 비활성화되면 Redis를 호출하지 않고 MySQL을 조회
- Redis를 원본 랭킹 저장소로 사용하지 않음

### MySQL Replica·장애 정책

- 활성 readOnly 트랜잭션이므로 정상 구성에서는 Replica 조회 대상이다.
- Replica는 비동기 복제로 stale할 수 있다.
- Replica 장애 시 자동 Primary fallback은 구현하지 않았다.
- Redis 장애 fallback의 MySQL 조회도 현재 readOnly 라우팅 정책을 따르므로 Replica 연결 상태의 영향을 받을 수 있다.
- Redis 전체 장애 중 정상 Replica 환경에서 MySQL 결과 반환은 직접 검증했다.

### 검증 포인트

- 시작 경계 포함·종료 경계 제외인가?
- ACTIVE/INACTIVE, COMPLETED 조건이 정확한가?
- 주문 0건 메뉴와 전체 0건을 처리하는가?
- 주문 수와 동률 정렬이 결정적인가?
- Cache Hit에서 DB Supplier가 호출되지 않는가?
- TTL 만료 후 다시 MySQL을 조회하고 Key를 재생성하는가?
- Redis 장애가 HTTP 기능 전체 장애가 되지 않는가?

---

## 5. 공통 오류 계약

비즈니스 오류 응답은 다음 구조를 사용한다.

```json
{
  "code": "ERROR_CODE",
  "message": "사용자에게 전달할 오류 메시지"
}
```

정확한 HTTP Status와 ErrorCode 매핑은 `GlobalExceptionHandler`, `BusinessException`, `ErrorCode` 구현을 따른다.

## 6. API 외부 계약과 내부 인프라의 분리

다음 내부 개선은 API Method·Endpoint·정상 응답 필드를 변경하지 않는다.

- 지갑 비관적 락
- 인기 메뉴 복합 인덱스
- Redis Cache-Aside
- Transactional Outbox
- MySQL Primary·Replica 라우팅
- Redis Sentinel Failover

인프라 장애 시 오류 가능성과 fallback 정책은 달라질 수 있으므로 운영 계약으로 확정하려면 별도 HTTP 장애 시나리오 테스트가 필요하다.
