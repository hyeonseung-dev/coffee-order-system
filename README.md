# Coffee Order System

다중 서버 환경을 고려한 포인트 기반 커피 주문 시스템

## 1. 프로젝트 목표

이 프로젝트는 커피 메뉴 조회, 포인트 충전, 커피 주문/결제, 최근 7일 인기 메뉴 조회 기능을 제공하는 백엔드 시스템이다.

단순 CRUD 구현이 아니라, 다중 서버 환경에서 발생할 수 있는 데이터 정합성 문제, 동시성 문제, 성능 문제, 캐시 장애, 비동기 후속 처리 흐름을 단계적으로 검증하는 것을 목표로 한다.

핵심 목표는 다음과 같다.

- 포인트 충전/차감 정합성 보장
- 동일 사용자 동시 주문 상황에서 DB 비관적 락 검증
- 주문/결제 트랜잭션 경계 명확화
- 최근 7일 인기 메뉴 조회 SQL 작성
- 인덱스 적용 전/후 성능 비교
- Redis 캐시 적용 및 TTL 검증
- 주문 완료 후 데이터 수집 플랫폼 전송 흐름 분리(Spring Event)
- MySQL Replication 기반 읽기/쓰기 분리 도전
- Redis Master-Replica 기반 캐시 장애 대응 도전
- RabbitMQ 기반 주문 이벤트 비동기 전송 도전

---

## 2. 과제 요구사항 분석

### 필수 요구사항

- 커피 메뉴 목록 조회 API
- 포인트 충전 API
- 커피 주문/결제 API
- 최근 7일 인기 메뉴 TOP 3 조회 API
- 주문 내역을 데이터 수집 플랫폼으로 실시간 전송하는 로직

### 도전 요구사항

- 다수 서버/다수 인스턴스 환경에서도 기능이 정상 동작하도록 설계
- 동시성 이슈 고려
- 데이터 일관성 고려
- 각 기능 및 제약사항에 대한 테스트 작성

---

## 3. 개발 전략

이번 프로젝트는 한 번에 모든 기술을 붙이지 않고, v0부터 v3까지 단계적으로 확장한다.

| 버전 | 목표 | 핵심 기술 | 완료 기준 |
| --- | --- | --- | --- |
| v0 | 필수 API 완성 | Spring Boot, JPA, MySQL | API 4개 정상 동작 |
| v1 | 정합성/동시성 검증 | `@Transactional`, DB 비관적 락 | 동시 주문 테스트 통과 |
| v2 | 성능 개선 | Index, Redis Cache | 인덱스/캐시 전후 비교 |
| v3-1 | 읽기/쓰기 분리 도전 | MySQL Replication | Primary/Replica 복제 확인 |
| v3-2 | 캐시 고가용성 도전 | Redis Master-Replica | Redis 장애 시 DB fallback |
| v3-3 | 메시징 도전 | Spring Event, RabbitMQ | 주문 이벤트 발행/소비 |

중요한 원칙은 다음과 같다.

- v0~v2는 제출 필수 범위로 반드시 완성한다.
- v3는 도전 범위로 진행한다.
- v3는 v0~v2 완료 이후 진행한다.
- 정합성이 중요한 포인트 차감/주문 저장/잔액 검증은 반드시 Primary DB에서 처리한다.
- Redis는 원본 저장소가 아니라 조회 성능 개선용 캐시로만 사용한다.
- 메시지는 DB commit 이후 발행한다.

---

## 4. 전체 아키텍처

### 4-1. v0 기본 구조

```text
Client
  ↓
Spring Boot App
  ↓
MySQL
````

### 4-2. v1 다중 서버 고려 구조

```text
Client
  ↓
Load Balancer
  ├── Spring Boot App 1
  └── Spring Boot App 2
        ↓
      MySQL Primary
```

다중 App Server 환경에서도 같은 MySQL Primary의 `point_wallet` row에 비관적 락을 걸면, 동일 사용자의 포인트 차감 정합성을 보장할 수 있다.

### 4-3. v2 Redis Cache 구조

```text
Client
  ↓
Spring Boot App
  ↓
Redis Cache
  ↓ miss
MySQL
```

Redis는 인기 메뉴 TOP 3 조회 결과를 캐싱한다.

원본 데이터는 MySQL `orders` 테이블이다.

### 4-4. v3 도전 구조

```text
Client
  ↓
Load Balancer
  ├── Spring Boot App 1
  └── Spring Boot App 2

Database
  ├── MySQL Primary      // write
  └── MySQL Replica      // read

Cache
  ├── Redis Master
  └── Redis Replica

Message Queue
  └── RabbitMQ
        ↓
      Order Event Consumer
        ↓
      MockDataPlatformClient / log
```

---

## 5. ERD

```text
users
- id PK
- name
- created_at
- updated_at

point_wallet
- id PK
- user_id FK, UNIQUE
- balance
- created_at
- updated_at

point_history
- id PK
- user_id FK
- amount
- type: CHARGE, USE
- balance_after
- created_at

menu
- id PK
- name
- price
- status: ACTIVE, INACTIVE
- created_at
- updated_at

orders
- id PK
- user_id FK
- menu_id FK
- order_price
- status: COMPLETED
- ordered_at
```

관계:

```text
users 1 : 1 point_wallet
users 1 : N point_history
users 1 : N orders
menu  1 : N orders
```

설계 의도:

* `users`와 `point_wallet`을 분리하여 회원 정보와 포인트 잔액 책임을 분리한다.
* `point_wallet`은 주문/결제 시 비관적 락 대상이 된다.
* `point_history`는 충전/사용 이력을 저장하여 정합성 검증에 사용한다.
* `orders`에는 성공한 주문만 저장한다.
* 인기 메뉴 집계의 원본 데이터는 `orders`이다.

---

## 6. API 명세

### 6-1. 메뉴 목록 조회

```http
GET /api/menus
```

응답 예시:

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

---

### 6-2. 포인트 충전

```http
POST /api/users/{userId}/points/charge
```

요청 예시:

```json
{
  "amount": 10000
}
```

응답 예시:

```json
{
  "data": {
    "userId": 1,
    "chargedAmount": 10000,
    "balance": 10000
  }
}
```

예외:

* `USER_NOT_FOUND`
* `INVALID_CHARGE_AMOUNT`

---

### 6-3. 커피 주문/결제

```http
POST /api/orders
```

요청 예시:

```json
{
  "userId": 1,
  "menuId": 1
}
```

응답 예시:

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

예외:

* `USER_NOT_FOUND`
* `MENU_NOT_FOUND`
* `INACTIVE_MENU`
* `INSUFFICIENT_POINT`

주문 정책:

* 수량은 1개 고정
* 하나의 주문은 하나의 메뉴만 주문
* 주문 실패는 `orders`에 저장하지 않음
* 성공한 주문만 `orders`에 저장
* 실제 PG/포트원 연동은 하지 않음
* 포인트 사용 이력은 `point_history`에 저장
* 주문 성공 이후 주문 완료 이벤트를 발행한다.

---

### 6-4. 최근 7일 인기 메뉴 TOP 3 조회

```http
GET /api/menus/popular?days=7&limit=3
```

응답 예시:

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

---

## 7. 주문/결제 트랜잭션 설계

주문/결제는 하나의 트랜잭션 안에서 처리한다.

처리 흐름:

```text
1. 사용자 조회
2. 메뉴 조회
3. point_wallet 비관적 락 조회
4. 잔액 검증
5. 포인트 차감
6. 포인트 사용 이력 저장
7. 주문 저장
8. 주문 완료 이벤트 발행
```

중요한 원칙:

* 잔액 검증과 포인트 차감은 같은 트랜잭션에서 처리한다.
* 포인트 차감과 주문 저장은 분리하지 않는다.
* 주문 실패 시 주문 데이터와 포인트 사용 이력은 저장하지 않는다.
* 주문 성공 시에만 주문 완료 이벤트를 발행한다.
* 외부 전송은 DB commit 이후 처리하는 방향으로 확장한다.

---

## 8. 동시성 문제와 DB 비관적 락 선택 이유

대표 동시성 테스트 시나리오:

```text
초기 포인트: 10,000P
메뉴 가격: 3,000P
동시 주문 요청: 10개
```

기대 결과:

```text
성공 주문: 3개
실패 주문: 7개
최종 잔액: 1,000P
주문 저장: 3개
포인트 사용 이력: 3개
```

이번 프로젝트에서는 Redis 분산락보다 DB 비관적 락을 우선한다.

이유:

* 보호 대상은 Redis key가 아니라 MySQL의 `point_wallet.balance`이다.
* 원본 저장소가 MySQL이다.
* 다중 App Server 환경에서도 같은 Primary MySQL을 바라보면 row lock이 공통으로 동작한다.
* 포인트 잔액 검증과 차감은 반드시 DB 트랜잭션 안에서 처리되어야 한다.

---

## 9. 인기 메뉴 조회 SQL

최근 7일간 주문 횟수 기준 TOP 3 메뉴를 조회한다.

```sql
SELECT menu_id, COUNT(*) AS order_count
FROM orders
WHERE ordered_at >= NOW() - INTERVAL 7 DAY
GROUP BY menu_id
ORDER BY order_count DESC
LIMIT 3;
```

정확성 기준:

* 주문 실패 건은 `orders`에 저장되지 않으므로 집계 대상에서 제외된다.
* 성공 주문만 집계된다.
* 원본 데이터는 MySQL `orders`이다.

---

## 10. 인덱스 성능 개선

인기 메뉴 조회 성능 개선을 위해 인덱스를 검토한다.

1차 후보:

```sql
CREATE INDEX idx_orders_ordered_at_menu
ON orders (ordered_at, menu_id);
```

비교 후보:

```sql
CREATE INDEX idx_orders_menu_ordered_at
ON orders (menu_id, ordered_at);
```

검증 방식:

* 대량 주문 더미 데이터 생성
* 인덱스 적용 전 `EXPLAIN` 확인
* 인덱스 적용 후 `EXPLAIN` 확인
* 실행 시간 비교
* 결과를 README/TIL에 표로 정리

---

## 11. Redis 캐시 성능 개선

인기 메뉴 TOP 3 조회 결과를 Redis에 캐싱한다.

캐시 정책:

```text
key: popular:menus:7days
TTL: 1분 또는 5분
value: 인기 메뉴 TOP 3 조회 결과
```

처리 흐름:

```text
1. 인기 메뉴 조회 요청
2. Redis cache 조회
3. cache hit → Redis 결과 반환
4. cache miss → MySQL에서 집계
5. 집계 결과 Redis 저장
6. 응답 반환
```

Redis를 원본 랭킹 저장소로 사용하지 않는 이유:

* DB 주문 저장 성공 후 Redis 증가 실패 가능성
* Redis 증가 후 DB 트랜잭션 롤백 가능성
* Redis 장애/재시작 시 데이터 유실 가능성
* DB와 Redis 간 보정 작업 필요

설계 원칙:

```text
정확한 원본: MySQL orders
성능 개선: Redis cache
```

---

## 12. Spring Event 기반 주문 후속 처리

과제 요구사항에는 주문 내역을 데이터 수집 플랫폼으로 실시간 전송하는 로직이 포함된다.

v1~v2에서는 주문 트랜잭션과 외부 전송 관심사를 분리하기 위해 Spring Event를 사용한다.

처리 흐름:

```text
1. OrderService에서 주문/결제 트랜잭션 처리
2. 주문 저장 성공
3. OrderCompletedEvent 발행
4. DB commit 이후 AFTER_COMMIT Listener 실행
5. MockDataPlatformClient 또는 log로 주문 데이터 전송 시뮬레이션
```

이렇게 분리하는 이유:

* 주문/결제 핵심 로직과 외부 전송 로직의 책임을 분리한다.
* 주문 트랜잭션이 실패했는데 외부 전송이 먼저 발생하는 문제를 방지한다.
* RabbitMQ 확장 전 단계로 이벤트 흐름을 먼저 검증할 수 있다.

주의:

* DB commit 전에 외부 전송이 발생하면 안 된다.
* 주문 실패 시 이벤트가 발행되면 안 된다.

---

## 13. MySQL Replication과 읽기/쓰기 분리 도전

v3-1 목표는 MySQL Primary/Replica 구조를 구성하고, 쓰기와 읽기 요청을 분리하는 것이다.

구조:

```text
Spring Boot App
  ↓ write
MySQL Primary
  ↓ replication
MySQL Replica
  ↑ read
Spring Boot App
```

쓰기 요청은 Primary로 보낸다.

```text
- 포인트 충전
- 포인트 차감
- 주문 저장
- 포인트 이력 저장
- 포인트 잔액 검증
```

읽기 요청은 Replica로 보낼 수 있다.

```text
- 메뉴 목록 조회
- 인기 메뉴 조회
```

중요한 정합성 판단:

* 포인트 차감, 주문 저장, 잔액 검증은 반드시 Primary에서 처리한다.
* Replica는 복제 지연이 있을 수 있다.
* 포인트 잔액 검증을 Replica에서 하면 오래된 잔액 기준으로 주문 가능 여부를 판단할 위험이 있다.
* 정합성이 중요한 조회는 Primary를 사용한다.

검증 항목:

```text
[복제 검증]
- Primary에 메뉴/주문 insert
- Replica에서 데이터 조회되는지 확인

[읽기/쓰기 분리 검증]
- 포인트 충전/주문은 Primary 로그 확인
- 메뉴 조회/인기 메뉴 조회는 Replica 로그 확인

[복제 지연 트러블슈팅]
- 주문 직후 Replica에서 인기 메뉴 조회 시 반영 지연 가능성 확인
- 정합성이 중요한 조회는 Primary로 보내야 함을 정리
```

---

## 14. Redis Master-Replica와 장애 대응 도전

v3-2 목표는 Redis Master-Replica 구조를 구성하고, Redis 장애 상황에서도 서비스 핵심 기능이 동작하도록 fallback을 검증하는 것이다.

구조:

```text
Spring Boot App
  ↓
Redis Master
  ↓ replication
Redis Replica
```

이번 프로젝트에서 Redis의 역할:

```text
Redis = 인기 메뉴 조회 결과 캐시
MySQL = 정확한 원본 데이터
```

Redis 정상 시:

```text
1. 인기 메뉴 조회 요청
2. Redis cache hit
3. Redis 결과 반환
```

Redis miss 시:

```text
1. Redis cache miss
2. MySQL orders 기준으로 인기 메뉴 집계
3. Redis에 결과 저장
4. 응답 반환
```

Redis 장애 시:

```text
1. Redis 조회 실패
2. 장애 로그 기록
3. MySQL에서 인기 메뉴 집계
4. 정상 응답 반환
```

중요한 판단:

* Redis 장애가 주문/결제 정합성에 영향을 주면 안 된다.
* Redis는 원본 저장소가 아니므로 장애 시에도 MySQL 기준으로 응답할 수 있어야 한다.
* Redis Master-Replica만으로 자동 장애 전환이 완성되는 것은 아니다.
* 자동 failover는 Sentinel 또는 Redis Cluster 영역이므로 이번 과제에서는 한계와 개선 방향으로 정리한다.

---

## 15. RabbitMQ 기반 주문 이벤트 전송 도전

v3-3 목표는 Spring Event로 처리하던 주문 후속 처리를 RabbitMQ 기반 비동기 메시징으로 확장하는 것이다.

처리 흐름:

```text
1. OrderService에서 주문/결제 트랜잭션 처리
2. 주문 저장 성공
3. OrderCompletedEvent 발행
4. DB commit 이후 AFTER_COMMIT Listener 실행
5. RabbitMQ exchange로 메시지 발행
6. Consumer가 queue에서 메시지 수신
7. MockDataPlatformClient 또는 log로 외부 전송 시뮬레이션
```

RabbitMQ 메시지는 DB commit 전에 발행하지 않는다.

이유:

* DB commit 전에 메시지를 발행하면 RabbitMQ에는 메시지가 있지만 실제 DB 주문은 롤백될 수 있다.
* 이 경우 외부 시스템에는 존재하지 않는 주문이 전송되는 불일치가 발생한다.

한계:

* AFTER_COMMIT 이후 RabbitMQ 발행 중 실패하면 DB 주문은 저장됐지만 메시지는 유실될 수 있다.
* 더 실무적인 해결책은 Outbox Pattern이다.
* 이번 과제에서는 AFTER_COMMIT 이후 발행을 적용하고, Outbox Pattern은 한계와 개선 방향으로 정리한다.

---

## 16. 테스트 전략

### v0 테스트

* 메뉴 목록 조회 API 테스트
* 포인트 충전 API 테스트
* 주문/결제 API 테스트
* 인기 메뉴 TOP 3 조회 API 테스트
* 기본 예외 테스트

### v1 테스트

* 동일 사용자 동시 주문 테스트
* 잔액 음수 방지 테스트
* 주문 저장 개수 검증
* 포인트 사용 이력 개수 검증
* 포인트 부족 시 주문 저장 안 됨 검증

### v2 테스트

* 인기 메뉴 정확성 테스트
* 인덱스 적용 전/후 성능 비교
* Redis cache hit/miss 테스트
* TTL 만료 후 DB 재조회 테스트

### v3 테스트

* MySQL Primary/Replica 복제 확인
* 읽기/쓰기 분리 로그 확인
* Redis Master/Replica 복제 확인
* Redis 장애 시 DB fallback 확인
* 주문 성공 시 RabbitMQ 메시지 발행 확인
* 주문 실패 시 RabbitMQ 메시지 미발행 확인

---

## 17. Docker Compose 구성 계획

초기 v0~v2:

```text
- Spring Boot App
- MySQL
- Redis
```

v3 도전:

```text
- Spring Boot App 1
- Spring Boot App 2
- MySQL Primary
- MySQL Replica
- Redis Master
- Redis Replica
- RabbitMQ
```

주의:

* v0부터 복제 구조를 만들지 않는다.
* v0~v2가 완료된 뒤 v3에서 복제 구조를 확장한다.
* Docker Compose 설정에 과도하게 시간을 쓰지 않는다.

---

## 18. 제외한 기술과 제외 이유

### Kafka

이번 과제에서는 Kafka를 구현 범위에서 제외한다.

이유:

* 현재 핵심 문제는 대용량 이벤트 스트리밍이 아니라 주문/결제 정합성이다.
* Kafka를 먼저 도입하면 트랜잭션, 동시성, 데이터 일관성 검증이 흐려질 수 있다.
* 주문 완료 후 후속 처리는 Spring Event와 RabbitMQ 단계로도 충분히 설명 가능하다.

### MSA

이번 과제에서는 MSA를 적용하지 않는다.

이유:

* 개인 과제 범위에서 서비스 분리는 오히려 트랜잭션과 정합성 설명을 어렵게 만든다.
* 현재 목표는 분산 시스템 설계보다 단일 애플리케이션에서 정합성과 성능 개선을 검증하는 것이다.

### Kubernetes

이번 과제에서는 Kubernetes를 적용하지 않는다.

이유:

* 인프라 운영 학습 비용이 크다.
* 과제 핵심인 주문/결제 정합성, 동시성, 캐싱 검증과 직접 관련성이 낮다.

### 복잡한 인증/인가

이번 과제에서는 Spring Security 기반 인증/인가를 적용하지 않는다.

이유:

* 요구사항은 사용자 식별값 기반 포인트 충전/주문이다.
* 인증/인가를 추가하면 과제 핵심 범위가 흐려진다.

---

## 19. 트러블슈팅 기록 예정 항목

* 동시 주문 시 잔액이 음수가 되는 문제
* DB 비관적 락 적용 전/후 차이
* 주문 실패 시 orders 저장 여부
* 포인트 이력과 주문 데이터 불일치 가능성
* 인기 메뉴 조회 인덱스 적용 전/후 차이
* Redis cache hit/miss 검증
* Redis 장애 시 DB fallback
* MySQL Replica 복제 지연
* readOnly 트랜잭션을 무조건 Replica로 보내면 위험한 경우
* RabbitMQ 메시지 발행 시점과 DB commit 불일치
* Outbox Pattern 필요성

---

## 20. 한계와 개선 방향

이번 프로젝트는 개인 과제 범위에서 정합성, 동시성, 성능 개선, 비동기 처리, 장애 대응을 단계적으로 검증하는 데 초점을 둔다.

한계:

* Redis Master-Replica만으로 자동 failover는 완성되지 않는다.
* RabbitMQ 발행 실패에 대한 완전한 정합성 보장은 Outbox Pattern이 필요하다.
* MySQL Replica는 복제 지연이 있으므로 모든 읽기 요청을 Replica로 보낼 수 없다.
* 인기 메뉴 Redis 캐시는 성능 개선용이며 원본 데이터가 아니다.

개선 방향:

* Redis Sentinel 또는 Redis Cluster 검토
* Outbox Pattern 적용
* 읽기/쓰기 분리 기준 고도화
* 부하 테스트 기반 병목 분석
* 모니터링 지표 추가

---

## Development Process

본 프로젝트는 Issue 기반 Human-in-the-loop AI-assisted 개발 프로세스를 적용한다.

- [Project Context](docs/PROJECT_CONTEXT.md)
- [AI Workflow](docs/AI_WORKFLOW.md)
- [Codex Rules](docs/CODEX_RULES.md)
- [AI Review Log](docs/AI_REVIEW_LOG.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
