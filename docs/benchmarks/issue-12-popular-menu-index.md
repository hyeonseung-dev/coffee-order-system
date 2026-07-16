# Issue #12 인기 메뉴 인덱스 비교

## 결론

`orders(menu_id, status, ordered_at)`를 최종 인덱스로 선택한다. 실제 JPQL은 ACTIVE 메뉴에서 시작해 `orders`를 LEFT JOIN하므로, `menu_id` 선두 인덱스가 조인 경로와 맞는다. 후보 B의 측정 중앙값은 기준선보다 낮았고, 후보 A는 실행 계획과 측정값 모두 기준선과 유의미하게 다르지 않았다.

이 결과는 로컬 Docker MySQL 8.4 단일 컨테이너에서의 DB 쿼리 측정이다. HTTP 응답시간·처리량·p95는 이 문서의 결론에 포함하지 않으며 #45에서 검증한다.

## 대상 쿼리와 바인딩

`MenuRepository#findPopularMenus`의 Hibernate SQL 로그에서 다음 구조와 바인딩을 확인했다.

```sql
SELECT m.id, m.name, COUNT(o.id)
FROM menu m
LEFT JOIN orders o ON o.menu_id = m.id
  AND o.status = ?
  AND o.ordered_at >= ?
  AND o.ordered_at < ?
WHERE m.status = ?
GROUP BY m.id, m.name
ORDER BY COUNT(o.id) DESC, m.id ASC
FETCH FIRST ? ROWS ONLY
```

- Hibernate 회귀 테스트 바인딩: `COMPLETED`, `2026-07-05T15:00:00Z`, `2026-07-12T15:00:00Z`, `ACTIVE`, `3`
- 벤치마크 바인딩: `COMPLETED`, `2026-07-08 15:00:00` 이상, `2026-07-15 15:00:00` 미만, `ACTIVE`, TOP 3
- MySQL 측정은 [fixture SQL](../../scripts/benchmark/issue-12-popular-menu-fixture.sql)의 동등 SQL과 위 기간을 사용했다.

## 데이터·측정 조건

- MySQL Docker 이미지: 8.4
- 주문: 500,000건, 메뉴: 20개(그중 ACTIVE 15개)
- 주문 분포: 최근 7일 약 10%, 이전 약 90%, 상태는 모두 `COMPLETED`
- 각 조건: 인덱스 생성 또는 제거 → `ANALYZE TABLE orders` → 워밍업 3회 → `EXPLAIN ANALYZE` 측정 5회
- 대표값: 최상단 Limit 노드의 actual end time 중앙값
- fixture는 `issue-12-benchmark-` 접두사로 분리했고 측정 후 주문·메뉴·사용자 데이터를 제거했다.

## 현재 인덱스 기준선

측정 전 `SHOW CREATE TABLE orders`와 `SHOW INDEX FROM orders`에서 FK로 생성된 단일 `menu_id` 인덱스와 `user_id` 인덱스를 확인했다.

후보는 동시에 유지하지 않고 아래 DDL로 하나씩 생성·측정·제거했다.

```sql
-- 후보 A
ALTER TABLE orders ADD INDEX idx_orders_ordered_at_menu_id (ordered_at, menu_id);

-- 후보 B
ALTER TABLE orders ADD INDEX idx_orders_menu_status_ordered_at (menu_id, status, ordered_at);
```

| 조건 | 측정 5회(ms) | 중앙값(ms) | EXPLAIN ANALYZE 요약 |
|---|---:|---:|---|
| 기준선: `menu_id` | 196, 195, 200, 193, 202 | 196.0 | `menu_id` index lookup 후 상태·기간 filter, 메뉴별 약 18,751행 접근, 임시 집계·정렬 |
| 후보 A: `(ordered_at, menu_id)` | 192, 223, 192, 195, 196 | 195.0 | 기존 `menu_id` 인덱스를 계속 사용, 실행 계획 변화 없음 |
| 후보 B: `(menu_id, status, ordered_at)` | 79.9, 87.6, 84.2, 82.6, 106.0 | 84.2 | 후보 B의 covering index lookup 사용, 임시 집계·정렬은 유지 |

후보 B의 중앙값은 기준선 대비 약 57.0% 낮았다. `GROUP BY`, `COUNT`, 정렬 때문에 집계용 임시 테이블과 TOP 3 정렬 비용은 남는다.

## 저장 공간과 쓰기 비용

| 조건 | `information_schema.tables.index_length` | 10,000건 INSERT 1회 |
|---|---:|---:|
| 단일 `menu_id` 기준선 | 24,215,552 bytes | 43.331 ms |
| 후보 B | 29,458,432 bytes | 35.615 ms |

- 후보 B 적용 후 총 인덱스 영역은 약 5,242,880 bytes(약 21.7%) 증가했다.
- INSERT 시간은 단일 측정이므로 캐시·컨테이너 상태 편차를 분리하지 못했다. 후보 B의 쓰기 비용이 개선됐거나 악화됐다고 결론 내리지 않는다.
- 개별 InnoDB 인덱스 페이지 크기 조회는 로컬 `coffee` 계정에 `mysql.innodb_index_stats` SELECT 권한이 없어 수행하지 못했다. 위 값은 테이블 전체 인덱스 영역이다.

## 최종 반영과 한계

- `Order` Entity의 `@Table(indexes = ...)`에 `idx_orders_menu_status_ordered_at`를 반영했다. 현재 프로젝트의 `spring.jpa.hibernate.ddl-auto=update` 방식과 일치한다.
- 후보 A는 최종 스키마에 남기지 않았다.
- 인기 메뉴 JPQL·LEFT JOIN·0건 ACTIVE 메뉴 정책·API 응답은 변경하지 않았다.
- 실제 운영 데이터 분포, 동시 쓰기 부하, HTTP 부하와 장기 인덱스 유지 비용은 미검증이다.
- 후속 #13은 Redis 캐시, #45는 API 부하와 응답시간을 별도로 다룬다.
