# Issue #45 인기 메뉴 API K6 성능 비교

## 결론

Human 로컬 K6 실측에서 Cache Hit는 MySQL 직접 조회 기준선보다 평균 응답시간이 약 16.4% 감소하고, p95가 약 18.4% 감소하며, 측정 구간 RPS가 약 19.6% 증가했다. 세 시나리오는 캐시 상태를 분리했고, MySQL 직접 조회와 Cache Hit의 모든 반복에서 HTTP 실패율은 0%였다.

이 결과는 로컬 단일 장비·소규모 데이터 조건의 비교 근거이며 운영 TPS나 운영 환경 성능으로 일반화하지 않는다.

## 시나리오와 상태 통제

| 시나리오 | Spring Boot 설정 | 사전 조건 | K6 부하 | 반복 | 기록 지표 |
| --- | --- | --- | --- | --- | --- |
| MySQL 직접 조회 | `POPULAR_MENU_CACHE_ENABLED=false` | Redis는 실행하되 애플리케이션이 호출하지 않음 | 30 VU, 워밍업 10초 + 측정 30초 | 3회 | 평균, p95, RPS, HTTP 실패율 |
| Cache Hit | `POPULAR_MENU_CACHE_ENABLED=true` | 매 회 API를 한 번 호출해 Key 생성 | 30 VU, 워밍업 10초 + 측정 30초 | 3회 | 평균, p95, RPS, HTTP 실패율 |
| Cache Miss | `POPULAR_MENU_CACHE_ENABLED=true` | 매 회 대상 KST 날짜 Key 삭제 | 1 VU, 1 iteration | 3회 | 응답시간, 성공 여부, DB 조회·Redis 저장 근거 |

Cache Miss는 첫 요청 이후 Hit가 되므로 지속 부하 시나리오에 사용하지 않는다. Redis 장애 fallback은 기능 검증일 뿐 이 표의 MySQL 기준선이 아니다.

## 실행 환경 기록

| 항목 | 실제 값 |
| --- | --- |
| 실행 일시 | 2026-07-18, Human 로컬 측정 |
| Host OS / CPU / Memory | 제공되지 않음 |
| Java / Spring Boot | 제공되지 않음 |
| MySQL / Redis Docker 이미지 | MySQL 8.4 / Redis 7.4-alpine |
| K6 실행 도구 | 로컬 k6 |
| 애플리케이션 URL | Human 로컬 환경 |
| 데이터 규모 | 소규모 데이터. 정확한 menu/orders 건수는 제공되지 않음 |
| JVM·HikariCP·Redis 튜닝 | 수행하지 않음 |

## 실행 순서

`docker compose up -d mysql redis`로 의존 서비스를 실행한다. MySQL 직접 조회와 Cache Hit는 서로 다른 Spring Boot 프로세스로 실행해 설정이 섞이지 않게 한다.

```bash
# 1. MySQL 직접 조회용 Spring Boot (별도 터미널)
# .env의 Docker MySQL 계정을 Spring Boot 환경 변수명으로 매핑한다.
set -a; source .env; set +a
DB_USERNAME="$MYSQL_USER" DB_PASSWORD="$MYSQL_PASSWORD" POPULAR_MENU_CACHE_ENABLED=false ./gradlew bootRun

# 2. MySQL 직접 조회 3회
scripts/k6/run-popular-menu-benchmark.sh mysql

# 3. 위 Spring Boot를 종료한 뒤 Cache Hit/Miss용으로 재시작
set -a; source .env; set +a
DB_USERNAME="$MYSQL_USER" DB_PASSWORD="$MYSQL_PASSWORD" POPULAR_MENU_CACHE_ENABLED=true ./gradlew bootRun

# 4. Cache Hit와 단일 Cache Miss를 각각 3회
scripts/k6/run-popular-menu-benchmark.sh hit
scripts/k6/run-popular-menu-benchmark.sh miss
```

다른 포트를 쓸 경우 모든 명령에 `BASE_URL=http://host.docker.internal:포트`를 붙인다. `scripts/k6/run-popular-menu-benchmark.sh`는 Miss 직전에 `popular:menus:7days:{KST 날짜}:v1` Key를 삭제하며, Hit 직전에 API를 한 번 호출한다.

MySQL 직접 조회·Cache Hit에서 K6 출력의 `popular_menu_measure_duration`, `popular_menu_measure_failure_rate`, `popular_menu_measure_requests`를 기록한다. 이 메트릭은 10초 워밍업을 제외한 30초 측정 구간만 집계한다. Cache Miss는 `popular_menu_cold_request_duration`과 HTTP 성공 여부만 기록한다.

## 결과 기록

### MySQL 직접 조회와 Cache Hit

| 시나리오 | 회차 | 평균 응답시간 | p95 응답시간 | RPS | HTTP 실패율 | K6 원본 결과 위치 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| MySQL 직접 조회 | 1 | 11.18ms | 14.85ms | 2,670.0 | 0% | Human 로컬 k6 출력 |
| MySQL 직접 조회 | 2 | 10.99ms | 14.36ms | 2,714.3 | 0% | Human 로컬 k6 출력 |
| MySQL 직접 조회 | 3 | 11.23ms | 14.23ms | 2,656.1 | 0% | Human 로컬 k6 출력 |
| Cache Hit | 1 | 9.32ms | 11.84ms | 3,201.0 | 0% | Human 로컬 k6 출력 |
| Cache Hit | 2 | 9.33ms | 11.95ms | 3,193.9 | 0% | Human 로컬 k6 출력 |
| Cache Hit | 3 | 9.26ms | 11.64ms | 3,222.4 | 0% | Human 로컬 k6 출력 |

| 3회 평균 | 평균 응답시간 | 평균 p95 | 평균 측정 구간 RPS | HTTP 실패율 |
| --- | ---: | ---: | ---: | ---: |
| MySQL 직접 조회 | 약 11.13ms | 약 14.48ms | 약 2,680.1 | 0% |
| Cache Hit | 약 9.30ms | 약 11.81ms | 약 3,205.8 | 0% |

비교 기준은 각 시나리오의 3회 평균이다. 즉, 평균 응답시간·p95·RPS는 같은 지표의 3회 값을 평균한 Human 실측값을 사용했고, Cache Hit와 MySQL 직접 조회의 차이는 해당 3회 평균끼리 비교했다. 최종 비교 수치는 평균 응답시간 약 16.4% 감소, p95 약 18.4% 감소, RPS 약 19.6% 증가다.

### Cache Miss 단일 cold request

| 회차 | 응답시간 | HTTP 성공 여부 | DB 조회 근거 | Redis 저장 근거 |
| ---: | ---: | --- | --- | --- |
| 1 | 13.01ms | HTTP 200, 실패율 0% | `PopularMenuCacheTest#Cache_Miss이면_MySQL_결과를_TTL과_함께_저장한다`의 Supplier 1회 검증 | 직전 `DEL` 결과 1, 요청 뒤 `EXISTS` 결과 1 |
| 2 | 5.16ms | HTTP 200, 실패율 0% | `PopularMenuCacheTest#Cache_Miss이면_MySQL_결과를_TTL과_함께_저장한다`의 Supplier 1회 검증 | 직전 `DEL` 결과 1, 요청 뒤 `EXISTS` 결과 1 |
| 3 | 9.51ms | HTTP 200, 실패율 0% | `PopularMenuCacheTest#Cache_Miss이면_MySQL_결과를_TTL과_함께_저장한다`의 Supplier 1회 검증 | 직전 `DEL` 결과 1, 요청 뒤 `EXISTS` 결과 1 |

3개 cold request 표본에는 p95나 처리량을 적지 않는다.

## 무효 Docker K6 실행

Codex가 Docker K6 실행을 시도했으나, Spring Boot 실행 세션이 측정 중 유지되지 않아 요청이 진행되지 않았다. 이 실행은 시간 초과·중단된 무효 시도로 분리하며, 위 결과 표와 최종 비교에는 사용하지 않았다. 최종 비교에는 Human이 로컬 k6로 성공한 값만 사용한다.

## DB·Redis 증거 확인

`PopularMenuCacheTest#캐시가_비활성화되면_Redis를_호출하지_않고_MySQL_결과를_한번만_반환한다`는 기준선에서 Redis 미호출과 DB Supplier 정확히 1회 실행을 자동 검증한다. `PopularMenuCacheTest#Cache_Miss이면_MySQL_결과를_TTL과_함께_저장한다`는 Cache Miss에서 DB Supplier 1회와 TTL 저장을 검증한다. Cache Miss 실측에서는 매 회차 직전 `DEL` 결과 1과 요청 뒤 `EXISTS` 결과 1을 확인해 Redis 재저장 근거를 남겼다.

Cache Miss 직후 아래 명령으로 Key와 TTL을 추가 확인할 수 있다. Cache Hit에서는 SQL 집계가 반복 호출되지 않는지 애플리케이션 로그로 검토한다.

```bash
docker compose exec redis redis-cli --scan --pattern 'popular:menus:7days:*:v1'
docker compose exec redis redis-cli TTL "popular:menus:7days:$(TZ=Asia/Seoul date +%F):v1"
```

## 제한과 후속 검토

- Docker, Spring Boot, MySQL, Redis, K6가 한 로컬 장비에서 자원을 공유하므로 결과에는 자원 경합과 실행 편차가 포함된다.
- 캐시 비활성화는 벤치마크 기준선용 설정이며 API 요청·응답은 바꾸지 않는다.
- 캐시 Stampede, Redis 장애 fallback, 운영 규모 TPS는 이 측정의 결론 대상이 아니다.
