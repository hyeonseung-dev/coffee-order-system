# Requirements

## 1. 과제 목적

다중 애플리케이션 인스턴스 환경에서도 데이터 정합성과 기능 의미가 유지되는 포인트 기반 커피 주문 시스템을 구현한다.

필수 API 완성뿐 아니라 다음을 설명 가능한 근거로 남긴다.

- 트랜잭션과 Rollback
- 동시성 문제 재현과 제어
- 인기 메뉴 집계 정확성
- 인덱스·Redis 성능 개선
- 주문 후속 처리의 실패 격리와 재시도
- MySQL 복제 지연과 읽기·쓰기 분리
- Redis 장애 전환과 DB fallback
- 테스트·성능·장애 검증의 보장·비보장 범위

## 2. 필수 기능 요구사항

### R-F01. 메뉴 목록 조회

- ACTIVE 메뉴만 조회한다.
- 응답에는 `menuId`, `name`, `price`를 포함한다.
- 메뉴 ID 오름차순으로 반환한다.
- 메뉴가 없으면 빈 배열을 반환한다.

### R-F02. 포인트 충전

- 사용자 ID와 0보다 큰 충전 금액을 입력받는다.
- 사용자 지갑 잔액을 증가시킨다.
- `point_history`에 `CHARGE` 이력과 반영 후 잔액을 저장한다.
- 사용자·지갑 없음과 0 이하 금액은 실패한다.

### R-F03. 주문·결제

- 사용자 ID와 메뉴 ID를 입력받는다.
- 수량은 1개로 고정한다.
- ACTIVE 메뉴만 주문할 수 있다.
- Primary 지갑 행을 비관적 쓰기 락으로 조회한다.
- 잔액이 충분하면 포인트 차감, `USE` 이력, `COMPLETED` 주문, Outbox `PENDING` 이벤트를 하나의 트랜잭션에서 저장한다.
- 실패하면 잔액·이력·주문·Outbox를 함께 Rollback한다.
- 실제 PG는 연동하지 않는다.

### R-F04. 인기 메뉴 TOP 3

- KST 기준 오늘을 제외한 직전 7개 완료 일자를 집계한다.
- 범위는 시작 포함·종료 제외다.
- ACTIVE 메뉴와 COMPLETED 주문을 기준으로 한다.
- 주문 0건 ACTIVE 메뉴도 후보로 포함한다.
- 주문 수 내림차순, 동률이면 메뉴 ID 오름차순이다.
- 최대 3개를 반환하고 후보가 없으면 빈 배열을 반환한다.
- 정확한 원본은 MySQL `orders`다.

### R-F05. 주문 후속 데이터 전송

- 주문 완료 정보를 Mock 데이터 플랫폼으로 전송한다.
- 전송할 Outbox 이벤트는 주문과 같은 트랜잭션에서 저장한다.
- 외부 전송은 Commit된 `PENDING` 이벤트를 별도 Publisher가 처리한다.
- 성공은 `SENT`, 재시도 한도 도달은 `FAILED`로 관리한다.
- 같은 이벤트 재시도에는 같은 `eventId`를 전달한다.

## 3. 데이터 정합성 요구사항

### R-C01. 주문 원자성

다음 데이터는 주문 트랜잭션 안에서 함께 성공하거나 함께 실패해야 한다.

- `point_wallet.balance`
- `point_history`의 `USE`
- `orders`
- `outbox_events`

### R-C02. 동시 주문

초기 10,000P, 메뉴 3,000P, 동일 사용자 동시 주문 10건에서 다음 결과를 만족해야 한다.

- 성공 주문 3건
- `INSUFFICIENT_POINT` 실패 7건
- 최종 잔액 1,000P
- 주문 3건
- `USE` 이력 3건

### R-C03. 데이터 원본

- 포인트·주문·Outbox 원본: MySQL Primary
- 인기 메뉴 정확성 원본: MySQL `orders`
- Redis: 조회 결과 캐시
- MySQL Replica: 지연을 허용할 수 있는 readOnly 조회

Redis·Replica의 값으로 주문 가능 여부나 포인트 잔액을 판단하지 않는다.

## 4. 성능 요구사항

### R-P01. 인기 메뉴 인덱스

- 실제 JPQL·실행계획을 기준으로 후보를 비교한다.
- 최종 인덱스는 `orders(menu_id, ordered_at, status)`다.
- 조회 개선뿐 아니라 인덱스 공간과 쓰기 비용 한계를 기록한다.
- 데이터·상태 분포가 바뀌면 재측정한다.

### R-P02. Redis Cache-Aside

- Key: `popular:menus:7days:{KST businessDate}:v1`
- 기본 TTL: 86,400초
- Hit이면 MySQL 집계를 생략한다.
- Miss이면 MySQL 조회 후 Redis에 저장한다.
- Redis 읽기·쓰기·역직렬화 실패는 MySQL fallback으로 처리한다.
- 캐시 비활성화 시 Redis를 호출하지 않는다.

### R-P03. 성능 측정

- MySQL 직접 조회와 Cache Hit의 상태를 분리한다.
- 워밍업과 본 측정을 분리한다.
- 반복 횟수와 평균·p95·RPS·실패율을 기록한다.
- Cache Miss는 첫 요청 후 Hit로 바뀌므로 지속 부하 결과와 섞지 않는다.
- 로컬 결과를 운영 TPS로 일반화하지 않는다.

## 5. 확장·장애 요구사항

### R-A01. MySQL 읽기·쓰기 분리

- 활성 `@Transactional(readOnly = true)` 조회만 Replica로 라우팅한다.
- 쓰기 트랜잭션, 비관적 락, 트랜잭션 밖 조회는 Primary로 라우팅한다.
- 실제 Connection은 트랜잭션 속성 결정 뒤 획득한다.
- 복제 지연으로 stale read가 발생할 수 있음을 재현한다.
- Replica 장애의 자동 Primary fallback은 현재 구현하지 않는다.

### R-A02. Redis 고가용성

- Master 1, Replica 1, Sentinel 3을 구성한다.
- quorum은 2로 설정한다.
- 현재 Master 탐색, 장애 감지, Replica 승격과 애플리케이션 재연결을 검증한다.
- Redis 전체 장애 시 인기 메뉴는 MySQL 결과를 반환한다.
- Redis 장애가 주문·포인트 기능에 영향을 주지 않아야 한다.

### R-A03. Outbox 전달 신뢰성

- 외부 시스템 장애가 완료 주문을 Rollback하지 않아야 한다.
- 실패 원인과 재시도 횟수를 저장한다.
- 서버 재시작 후 DB의 `PENDING` 이벤트를 다시 처리할 수 있어야 한다.
- Exactly Once를 주장하지 않는다.

## 6. 시간대 요구사항

- DB 저장 시각은 UTC `Instant`를 사용한다.
- JDBC·Hibernate 시간대는 UTC다.
- 주문 응답과 업무 날짜는 `Asia/Seoul`로 변환한다.
- 인기 메뉴 기간과 Redis Key 날짜는 KST 자정 경계를 사용한다.
- 서버 기본 시간대에 의존하지 않는다.

## 7. 검증 요구사항

| 검증 종류 | 필수 확인 |
|---|---|
| 단위·통합 테스트 | 정상·실패·경계·Rollback |
| 실제 MySQL 동시성 | 성공·실패 수, 주문·이력 수, 최종 잔액 |
| 인덱스 | 격리 Fixture, `EXPLAIN ANALYZE`, 반복 중앙값 |
| Redis | Key, JSON, TTL, Hit, 만료, 읽기·쓰기 실패 |
| K6 | 상태 통제, 워밍업, 반복, 평균·p95·RPS·실패율 |
| MySQL 복제 | 실제 라우팅, 복제 상태, stale read, 복구 |
| Redis Sentinel | quorum, Master 주소, 승격, 재연결, 전체 장애 fallback |
| Outbox | Commit·Rollback, 성공·실패·재시도, 동일 eventId |

실행하지 않은 검증은 성공으로 기록하지 않는다. 테스트 통과만으로 운영 무중단·성능·Exactly Once를 주장하지 않는다.

## 8. 제외 범위

- 인증·인가
- 실제 PG·포트원
- 장바구니·쿠폰·주문 수량
- 주문 취소·환불 상태
- MySQL Primary 자동 Failover
- ProxySQL·HAProxy·Orchestrator·InnoDB Cluster
- Redis Cluster·샤딩
- Outbox 다중 Publisher 선점과 Consumer 멱등 저장소
- RabbitMQ·Kafka 실구현
- MSA·Kubernetes
- 과도한 모니터링·CI/CD·AI 자동화

제외 범위는 중요하지 않아서가 아니라, 과제의 핵심 정합성·성능·장애 문제를 검증 가능한 크기로 완성하기 위한 선택이다.
