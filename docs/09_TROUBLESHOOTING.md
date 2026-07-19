# Troubleshooting

프로젝트 진행 중 발생한 문제와 해결 과정을 기록한다.

## Issue #44 — Redis Sentinel Failover와 MySQL Fallback 확인 절차

### 목적

인기 메뉴의 정답 데이터는 MySQL `orders`이며 Redis는 Cache-Aside 결과 캐시다. 따라서 Redis Master 장애 시 Sentinel이 Replica를 승격하는지, Redis 전체 장애 시에도 캐시 예외가 MySQL fallback으로 격리되는지를 별도로 확인한다.

### 확인 명령

```bash
docker compose up -d redis-master redis-replica redis-sentinel-1 redis-sentinel-2 redis-sentinel-3
scripts/redis/verify-sentinel-failover.sh
```

스크립트는 `SENTINEL CKQUORUM`, 장애 전후 `GET-MASTER-ADDR-BY-NAME`, 각 노드의 `INFO replication`, Sentinel의 감지·승격 로그를 검증한다. 일반 `./gradlew test`는 외부 Sentinel을 요구하지 않으며, Sentinel 연결·Cache Miss/Hit/TTL 재생성은 Docker network 안의 전용 서비스로 실행한다.

```bash
docker compose --profile redis-ha-test run --rm redis-ha-integration-test
```

### 해석과 제한

Sentinel의 장애 감지·승격·클라이언트 재연결 동안 요청이 지연되거나 일부 실패할 수 있다. 캐시 조회·저장 예외가 `PopularMenuCache`에서 잡혀 MySQL 결과를 반환하면 캐시 장애가 주문·포인트로 전파되지 않는다. Redis 복제는 비동기이므로 Failover 직전 캐시 Key가 유실될 수 있으나, 다음 Cache Miss에서 MySQL 집계로 재생성한다.

Redis Cluster, 샤딩, 운영 수준 백업·복구, Redis 분산락은 이 Issue 범위에 포함하지 않는다.

일반 `./gradlew test`는 외부 Redis 없이 실행한다. Sentinel HA 통합 테스트는 Redis·Sentinel과 동일 Docker 네트워크에서 별도 profile로 실행해야 하며, 일반 테스트 성공을 외부 인프라 검증으로 해석하지 않는다.

로컬 HA 검증은 다음 명령으로 시작한다.

```bash
docker compose --profile redis-ha up -d --build app-ha
docker compose exec redis-sentinel-1 redis-cli -p 26379 SENTINEL CKQUORUM coffee-order-redis
scripts/redis/verify-sentinel-failover.sh
docker compose --profile redis-ha down
```

Compose 서비스 내부 통신은 `redis-master`, `redis-replica`, `redis-sentinel-*` hostname을 사용한다. 호스트 공개 포트는 `127.0.0.1`로 제한한다. 이 검증은 로컬 Docker 장애 재현 결과이며 운영 SLA나 절대적 무중단을 보장하지 않는다. Redis 복제는 비동기라 Failover 직전 일부 캐시 Key가 유실될 수 있고, 다음 Cache Miss에서 MySQL 원본으로 재생성한다.

호스트의 기존 Redis와 포트가 충돌하면 HA 또는 Sentinel 테스트 명령 앞에 `REDIS_PORT=16379 REDIS_REPLICA_PORT=16380 REDIS_SENTINEL_1_PORT=36379 REDIS_SENTINEL_2_PORT=36380 REDIS_SENTINEL_3_PORT=36381`를 지정한다.

### 2026-07-18 직접 검증 결과

- Sentinel 3개 healthy와 `CKQUORUM coffee-order-redis` 성공을 확인했다.
- Master 중지 뒤 Replica 승격, `+promoted-slave`·`+switch-master`, app-ha의 새 Master 재연결을 확인했다.
- Redis 전체 장애 중 인기 메뉴 API는 MySQL 결과로 200을 반환했고, 포인트 충전·주문은 MySQL의 지갑·이력·주문 데이터를 정상 갱신했다.
- 기본 Lettuce 공유 연결은 전체 Redis 복구 뒤 이전 연결을 재사용하며 약 43초간 timeout을 반복했다. `redis-ha` 프로필에서만 `validateConnection`을 켠 뒤 app-ha 재시작 없이 첫 요청에서 Key 재생성과 TTL 86400을 확인했다.

## 기록 양식

### 문제

<!-- 어떤 문제가 발생했는지 작성 -->

### 원인

<!-- 왜 발생했는지 작성 -->

### 해결

<!-- 어떻게 해결했는지 작성 -->

### 검증

<!-- 테스트, 빌드, API 호출 등으로 어떻게 검증했는지 작성 -->

### 배운 점

<!-- 기술적으로 배운 점 작성 -->

### AI 활용 여부

<!-- AI를 사용했다면 어떤 방식으로 사용했는지 작성 -->

### 최종 판단

<!-- 실무/학습/면접 관점에서 남길 가치가 있는지 작성 -->

---

## Issue #43 - Replica 읽기 직후 최신 쓰기 결과를 조회하면 오래된 데이터를 볼 수 있음

### 문제

MySQL 비동기 복제는 Primary 커밋과 Replica 적용 사이에 지연이 생길 수 있다. 따라서 쓰기 성공 직후 `@Transactional(readOnly = true)` 조회를 Replica로 보내면 방금 저장한 메뉴·주문 집계가 아직 보이지 않을 수 있다.

### 해결

`ReplicationRoutingDataSource`는 읽기 전용 트랜잭션만 Replica로 보낸다. 쓰기 트랜잭션, 트랜잭션 밖의 조회, 주문 중 지갑을 `PESSIMISTIC_WRITE`로 읽는 경로는 모두 Primary를 사용한다. `LazyConnectionDataSourceProxy`로 트랜잭션 속성이 결정된 뒤에 실제 Connection을 얻도록 구성했다.

Replica 장애의 자동 Primary fallback은 적용하지 않았다. 장애 시 읽기 부하가 Primary로 몰려 주문·포인트 원본 처리까지 영향을 받는 것을 숨기지 않기 위해서다.

### 직접 검증 절차

```bash
docker compose up -d mysql-primary mysql-replica
docker compose exec mysql-replica sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW REPLICA STATUS\\G"'
docker compose exec mysql-replica sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "STOP REPLICA SQL_THREAD;"'
# Primary에 데이터를 저장한 뒤 Replica에서 아직 보이지 않는지 조회한다.
docker compose exec mysql-replica sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "START REPLICA SQL_THREAD;"'
```

`SHOW REPLICA STATUS`의 IO/SQL thread와 `Seconds_Behind_Source`, `Last_SQL_Error`를 함께 기록한다. Replica 컨테이너를 중지했을 때 메뉴·인기 메뉴 요청이 명시적으로 실패하고, 주문·포인트 요청이 Primary에서 계속 동작하는지도 별도로 확인한다.

### 한계

이 구성은 로컬 단일 Replica 학습 환경이다. 자동 failover, 다중 Replica 부하 분산, ProxySQL·HAProxy, 운영 비밀 관리와 GTID 토폴로지 복구는 범위에서 제외한다.

---

## Issue #9 - 주문 저장 시각과 인기 메뉴 조회 경계의 시간 원본 불일치

### 상태

- 발견 시점: PR #32 ChatGPT 재리뷰 및 Contract Traceability 검증
- 현재 상태: 원인과 해결 방향을 확정했으나 근본 해결은 미구현
- 차단 항목: PR #32 Contract Traceability `C-09 HOLD`
- 후속 작업: Issue #34 `주문 생성 시각을 Service Clock 기준으로 통일`

### 문제

인기 메뉴 조회는 `Clock.system(ZoneId.of("Asia/Seoul"))`을 사용해 오늘과 최근 7일의 경계를 계산한다.

반면 주문 생성 시각은 `Order.prePersist()`에서 `LocalDateTime.now()`로 생성하며 JVM 기본 시간대에 의존한다.

```text
주문 저장 시각
Order.prePersist()
→ LocalDateTime.now()
→ JVM 기본 시간대

인기 메뉴 조회 경계
MenuService.findPopularMenus()
→ LocalDate.now(clock)
→ Asia/Seoul 고정
```

따라서 JVM 기본 시간대가 KST가 아닌 환경에서는 주문 저장 시각과 조회 경계가 서로 다른 시간 기준을 사용할 수 있다.

예를 들어 실제 주문 시각이 `2026-07-13 00:30 KST`인데 애플리케이션 JVM이 UTC라면, `LocalDateTime.now()`는 `2026-07-12 15:30`을 생성할 수 있다. 시간대 정보가 없는 `LocalDateTime` 값이 저장되면 실제로는 7월 13일 주문이지만 7월 12일 주문처럼 집계될 가능성이 생긴다.

### 영향

- KST 자정 부근 주문이 최근 7일 집계에 잘못 포함되거나 제외될 수 있다.
- 로컬 JVM이 KST이면 정상처럼 보여 환경 차이를 놓치기 쉽다.
- H2 테스트가 성공해도 실제 주문 생성 경로의 시간대 일관성을 보장하지 못한다.
- 동일 데이터라도 실행 환경에 따라 인기 메뉴 결과가 달라질 수 있다.

### 발견 과정

Issue #9 구현에서는 조회 시간대를 KST로 고정하고, H2 Repository 테스트에서 `JdbcTemplate`으로 경계 시각의 주문 데이터를 직접 삽입했다.

이 테스트는 조회 쿼리의 시작·종료 경계를 정확히 검증했지만, 실제 주문 생성 경로인 `Order.prePersist()`를 통과하지 않았다.

PR #32 재리뷰에서 다음 두 시간 원본을 직접 대조하면서 문제가 발견됐다.

- 조회 시간 원본: `ClockConfig#clock`의 `Asia/Seoul`
- 저장 시간 원본: `Order#prePersist`의 `LocalDateTime.now()`

Contract Traceability에서 코드와 테스트 증거를 항목별로 연결한 결과, 시간 원본 일관성을 증명할 설정이나 테스트가 없어 `C-09 HOLD`로 판정했다.

### 원인

시스템 전체의 시간 생성 정책이 통일되지 않았다.

- 조회 계층은 명시적으로 주입된 KST `Clock`을 사용한다.
- Entity의 생성 시각은 JVM 기본 시간대에 의존한다.
- `LocalDateTime`은 시간대나 UTC 오프셋 정보를 보유하지 않는다.
- 테스트 Fixture가 실제 주문 생성 경로를 우회해 저장·조회 시간 원본의 차이를 드러내지 못했다.

핵심 원인은 다음과 같다.

```text
조회 기준 시간대만 고정함
≠ 데이터 생성 시간까지 동일한 기준을 사용함
```

### 검토한 해결 방법

#### 1. JVM 기본 시간대를 Asia/Seoul로 고정

장점:

- 적용이 빠르다.
- 기존 `LocalDateTime.now()` 코드를 유지할 수 있다.

단점:

- 로컬, CI, 애플리케이션 컨테이너, 운영 서버 설정에 의존한다.
- 설정이 누락되면 동일한 문제가 다시 발생한다.
- 코드만 보고 시간 정책을 파악하기 어렵다.
- 테스트에서 시간을 결정적으로 제어하기 어렵다.

#### 2. 주문 생성 Service 또는 Facade에서 Clock으로 orderedAt 생성

구조:

```text
OrderService 또는 OrderFacade
→ LocalDateTime.now(clock)
→ Order 생성 메서드에 orderedAt 전달
→ Order Entity는 전달받은 시각 저장
```

장점:

- 주문 저장과 인기 메뉴 조회가 같은 시간 원본을 사용할 수 있다.
- Fixed Clock으로 결정적인 테스트가 가능하다.
- 실행 환경의 JVM 기본 시간대에 덜 의존한다.
- 시간 생성 책임이 코드에 명시된다.

주의점:

- Entity에 Spring `Clock` Bean을 직접 주입하지 않는다.
- Service 또는 Facade가 시간을 계산하고 Entity에는 값만 전달한다.

### 결정

2번 방식을 권장안으로 선택했다.

다만 Issue #9에서는 Order 시간 구조 변경이 제외 범위였기 때문에 PR #32에서 임의로 수정하지 않았다. 별도 선행 Issue #34로 분리하고, Issue #34가 Merge되어 시간 원본 일관성이 확보될 때까지 PR #32의 `C-09 HOLD`를 유지한다.

### 검증 계획

Issue #34 구현 시 다음을 검증한다.

- Fixed Clock을 주입했을 때 주문의 `orderedAt`이 기대 시각과 동일한지
- 주문 생성 경로가 JVM 기본 시간대에 의존하지 않는지
- Order Entity가 Spring Bean을 직접 참조하지 않는지
- KST 자정 전후 주문이 인기 메뉴 집계 경계에 정확히 포함·제외되는지
- 관련 테스트, 전체 테스트, 빌드, `git diff --check`가 성공하는지

### 현재 검증 결과

확인한 것:

- `ClockConfig`는 `Asia/Seoul`을 사용한다.
- 인기 메뉴 조회의 시작 포함·종료 제외 경계 테스트는 성공한다.
- `Order.prePersist()`는 여전히 JVM 기본 시간대에 의존한다.
- 현재 코드에는 JVM 기본 시간대를 KST로 강제하고 검증하는 애플리케이션 설정이 없다.

아직 확인하지 못한 것:

- UTC JVM에서 실제 주문 생성 후 MySQL에 저장되는 최종 시각
- Issue #34 적용 후 주문 저장과 인기 메뉴 조회의 통합 경계 동작
- 실제 MySQL HTTP 호출과 실행계획

### 배운 점

- 날짜 집계에서는 조회 범위의 시간대만 고정하면 충분하지 않다.
- 데이터를 생성하는 시점과 조회하는 시점이 동일한 시간 정책을 사용해야 한다.
- 테스트 데이터 직접 삽입은 경계 쿼리를 검증하는 데 유용하지만 실제 생성 경로의 시간 정책을 우회할 수 있다.
- `LocalDateTime`은 시간대 정보를 갖지 않으므로 시스템의 시간 원본을 명시적으로 관리해야 한다.
- 전체 테스트 성공은 작성된 테스트의 성공일 뿐, 계약 전체 충족을 자동으로 의미하지 않는다.

### AI 활용 여부

- ChatGPT가 PR #32의 실제 Diff를 검토하며 저장 시간과 조회 시간 원본이 다른 점을 발견했다.
- Codex가 Contract Traceability에 시간 원본 일관성을 `C-09`로 분리하고 증거 부족을 `HOLD`로 기록했다.
- AI가 해결을 임의 적용하지 않고 JVM 시간대 고정과 Service Clock 도입의 대안을 제시했다.
- Human은 Service 또는 Facade에서 Clock으로 주문 시각을 생성하는 방향을 선택하고 Issue #34로 분리했다.

### 최종 판단

이 문제는 실제 장애가 발생한 뒤 수정한 사례가 아니라, 리뷰와 계약 추적 과정에서 발견한 환경 의존형 잠재 정합성 결함이다.

단순 오타나 구현 실수보다 다음 학습 가치가 있어 별도 트러블슈팅으로 남긴다.

- JVM 시간대와 애플리케이션 시간 정책의 차이
- `LocalDateTime`의 한계
- 저장 시각과 조회 경계의 일관성
- 테스트 Fixture가 실제 경로를 우회할 때 생기는 검증 공백
- Issue 범위를 지키면서 선행 작업으로 분리하는 방법

현재는 원인과 해결 방향만 확정된 상태다. Issue #34 구현과 검증이 끝난 뒤 해결 결과와 재발 방지 테스트를 추가로 갱신한다.

### 학습 후속 메모

현재 Human 이해도 검증은 완료되지 않았다. 다음 학습 시 아래 질문에 자신의 언어로 답한다.

1. `LocalDateTime.now()`가 왜 JVM 기본 시간대에 의존하는가?
2. `hibernate.jdbc.time_zone=Asia/Seoul`만으로 Java 코드의 `LocalDateTime.now()`까지 KST로 고정되지 않는 이유는 무엇인가?
3. 조회 Clock만 KST로 고정했을 때 자정 경계에서 어떤 데이터 오류가 생길 수 있는가?
4. Entity에 `Clock`을 직접 주입하지 않고 Service에서 시간을 생성하는 이유는 무엇인가?
5. `JdbcTemplate` Fixture 테스트가 실제 주문 생성 경로의 시간 문제를 발견하지 못한 이유는 무엇인가?
