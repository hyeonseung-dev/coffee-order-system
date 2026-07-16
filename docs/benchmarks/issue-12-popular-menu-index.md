# Issue #12 인기 메뉴 인덱스 비교

## 결론

`orders(menu_id, status, ordered_at)`를 최종 인덱스로 선택한다. 실제 JPQL은 ACTIVE 메뉴에서 시작해 `orders`를 LEFT JOIN하므로, `menu_id` 선두 인덱스가 조인 경로와 맞는다. 후보 B의 측정 중앙값은 기준선보다 약 64.9% 낮았고, 후보 A는 실행 계획과 측정값 모두 기준선과 유의미하게 다르지 않았다.

이 결과는 로컬 Docker MySQL 8.4 안의 전용 `coffee_order_issue12_benchmark` 스키마에서 측정한 DB 쿼리 결과다. 일반 개발 스키마와 `MenuDeveloperDataInitializer` 데이터는 포함하지 않았다. HTTP 응답시간·처리량·p95는 이 문서의 결론에 포함하지 않으며 #45에서 검증한다.

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
- [benchmark 스키마 SQL](../../scripts/benchmark/issue-12-benchmark-schema.sql)로 일반 개발 DB와 분리된 `coffee_order_issue12_benchmark`를 재생성했다.
- 전용 스키마에는 쿼리에 필요한 `users`, `menu`, `orders`만 생성했고 개발용 Spring 초기화기는 실행하지 않았다.
- 주문: 500,000건, 메뉴: 20개(그중 ACTIVE 15개)
- 주문 분포: 최근 7일 약 10%, 이전 약 90%, 상태는 모두 `COMPLETED`
- 최근 여부는 `n % 10`, 메뉴 배정은 `FLOOR(n / 10) % 20`으로 계산해 두 분포가 같은 나머지 연산에 결합되지 않게 했다.
- fixture 실행 후 메뉴별 전체 주문 수와 최근 주문 수를 직접 집계했다. 20개 메뉴 모두 전체 25,000건, 최근 7일 2,500건으로 확인됐다.
- 각 조건: 인덱스 생성 또는 제거 → `ANALYZE TABLE orders` → 워밍업 3회 → `EXPLAIN ANALYZE` 측정 5회
- 대표값: 최상단 Limit 노드의 actual end time 중앙값
- fixture는 `issue-12-benchmark-` 접두사로 분리했고 전용 스키마 내부 데이터만 재생성했다.
- 같은 MySQL 세션에서 fixture를 연속 두 번 실행해 임시 테이블 재생성과 동일한 분포를 확인했다.

### 격리 상태 직접 검증

Fixture 생성 직후 실제 측정 스키마에서 다음 결과를 확인했다.

| 검증 항목 | 결과 |
|---|---:|
| 전체 메뉴 | 20 |
| ACTIVE 메뉴 | 15 |
| fixture 외 메뉴 | 0 |
| 전체 주문 | 500,000 |
| fixture 외 주문 | 0 |
| 메뉴별 전체 주문 | 25,000 |
| 메뉴별 최근 7일 주문 | 2,500 |

따라서 인기 메뉴 쿼리의 `menu` 테이블 스캔은 20행, ACTIVE 필터 결과는 15행이다. 일반 개발 스키마의 기본 ACTIVE 메뉴 5개는 측정 대상에 포함되지 않는다.

## 현재 인덱스 기준선

측정 전 전용 스키마의 `SHOW CREATE TABLE orders`와 `SHOW INDEX FROM orders`에서 단일 `menu_id` 인덱스와 `user_id` 인덱스만 확인했다.

후보는 동시에 유지하지 않고 아래 DDL로 하나씩 생성·측정·제거했다.

```sql
-- 후보 A
ALTER TABLE orders ADD INDEX idx_orders_ordered_at_menu_id (ordered_at, menu_id);

-- 후보 B
ALTER TABLE orders ADD INDEX idx_orders_menu_status_ordered_at (menu_id, status, ordered_at);
```

| 조건 | 측정 5회(ms) | 중앙값(ms) | 주문 인덱스 actual rows × loops | EXPLAIN ANALYZE 요약 |
|---|---:|---:|---:|---|
| 기준선: `menu_id` | 182, 181, 180, 179, 181 | 181.0 | 25,000 × 15 | `menu_id` index lookup 후 최근 2,500행 × 15 filter, 총 37,500행 임시 집계·정렬 |
| 후보 A: `(ordered_at, menu_id)` | 179, 179, 203, 181, 179 | 179.0 | 25,000 × 15 | 기존 `menu_id` 인덱스를 계속 사용하며 기준선과 같은 filter·집계 계획 |
| 후보 B: `(menu_id, status, ordered_at)` | 64.2, 62.9, 64.8, 62.9, 63.5 | 63.5 | 25,000 × 15 | 후보 B covering index lookup 후 최근 2,500행 × 15 filter, 총 37,500행 임시 집계·정렬 |

세 조건 모두 메뉴 테이블 20행을 읽고 ACTIVE 15행에서 주문 조회를 15회 수행했다. 후보 B의 중앙값은 기준선 대비 약 64.9% 낮았다. 후보 B도 메뉴별 25,000개 인덱스 항목을 읽은 뒤 기간 조건에 맞는 2,500행을 집계하므로, `GROUP BY`, `COUNT`, 정렬을 위한 임시 테이블과 TOP 3 정렬 비용은 남는다.

## 저장 공간과 쓰기 비용

| 조건 | 격리 스키마 `information_schema.tables.index_length` |
|---|---:|
| 단일 `menu_id` 기준선 | 24,215,552 bytes |
| 후보 B | 29,458,432 bytes |

- 후보 B 적용 후 총 인덱스 영역은 약 5,242,880 bytes(약 21.7%) 증가했다.
- 복합 인덱스 유지로 INSERT·UPDATE 시 추가 페이지 갱신 비용이 발생할 수 있다. 이번 격리 재측정에서는 혼합 쓰기 부하를 측정하지 않았으므로 실제 쓰기 지연 크기는 결론 내리지 않는다.
- 이전 일반 개발 DB에서 수행한 단일 10,000건 INSERT 값은 격리 조건과 맞지 않아 최종 근거에서 제외했다.

## 최종 반영과 한계

- `Order` Entity의 `@Table(indexes = ...)`에 `idx_orders_menu_status_ordered_at`를 반영했다. 현재 프로젝트의 `spring.jpa.hibernate.ddl-auto=update` 방식과 일치한다.
- 후보 A는 최종 스키마에 남기지 않았다.
- 전용 benchmark 스키마에는 후보 B와 `user_id` 인덱스만 남겼으며 일반 개발 스키마 데이터는 삭제하거나 수정하지 않았다.
- 인기 메뉴 JPQL·LEFT JOIN·0건 ACTIVE 메뉴 정책·API 응답은 변경하지 않았다.
- 실제 운영 데이터 분포, 동시 쓰기 부하, HTTP 부하와 장기 인덱스 유지 비용은 미검증이다.
- 후속 #13은 Redis 캐시, #45는 API 부하와 응답시간을 별도로 다룬다.
