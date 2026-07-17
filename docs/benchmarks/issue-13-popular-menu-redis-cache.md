# Issue #13 인기 메뉴 Redis 캐시 정책

## 결론

최근 7일 인기 메뉴의 정확한 원본은 MySQL `orders`이며, Redis는 계산된 응답의 Cache-Aside 복사본이다. 캐시 Key는 KST 업무 날짜별로 분리하고, 운영 TTL은 24시간으로 둔다.

## Key와 TTL

- Key: `popular:menus:7days:{businessDate}:v1`
- `businessDate`: 주입된 UTC `Clock`의 현재 `Instant`를 `Asia/Seoul`로 변환한 날짜
- 운영 TTL: `POPULAR_MENU_CACHE_TTL_SECONDS` 환경 변수로 변경 가능하며 기본값은 86,400초(24시간)
- 테스트 프로필 TTL: 2초

KST 자정 뒤에는 다음 날짜 Key를 새로 사용한다. 따라서 TTL은 날짜 전환이 아니라 이전 날짜 Key를 자동 정리하는 역할이다.

## 장애와 데이터 경계

- Cache Hit이면 MySQL 집계 Repository를 호출하지 않는다.
- Cache Miss와 역직렬화 실패이면 MySQL 결과를 조회한다.
- Redis 읽기·저장 실패는 경고 로그를 남기고 이미 조회한 MySQL 결과를 반환한다.
- 빈 목록은 `[]`로 저장해 Miss의 `null`과 구분한다.
- 주문 성공 시 캐시 Evict·Put, Redis ZSet 카운터 갱신은 적용하지 않는다.

## 알려진 제한

- 같은 KST 날짜에 메뉴 상태나 과거 주문이 수정되면 해당 날짜 캐시가 만료될 때까지 이전 결과가 반환될 수 있다.
- 동시에 같은 날짜 Key가 Miss하면 여러 MySQL 집계가 실행될 수 있다. Cache Stampede 방지는 이번 범위에서 제외한다.
- Redis 장애 중에도 기능은 MySQL fallback으로 유지하지만, 캐시 성능은 보장하지 않는다.
- 평균·p95·처리량·실패율 측정은 Issue #45에서 수행한다.
