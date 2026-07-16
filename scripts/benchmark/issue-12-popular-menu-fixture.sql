-- Issue #12: MySQL 8.4 인기 메뉴 인덱스 비교용 재실행 fixture
-- 실행 전후에는 아래 BENCHMARK_CLEANUP 구문으로 이 fixture만 제거한다.

SET @fixture_prefix = 'issue-12-benchmark-';
SET @from_inclusive = TIMESTAMP('2026-07-08 15:00:00');
SET @to_exclusive = TIMESTAMP('2026-07-15 15:00:00');

-- 이전 실행의 같은 접두사 fixture만 정리해 재실행 가능하게 만든다.
DELETE o FROM orders o
JOIN users u ON u.id = o.user_id
WHERE u.name = CONCAT(@fixture_prefix, 'user');
DELETE FROM menu WHERE name LIKE CONCAT(@fixture_prefix, '%');
DELETE FROM users WHERE name = CONCAT(@fixture_prefix, 'user');

INSERT INTO users (name, created_at, updated_at)
VALUES (CONCAT(@fixture_prefix, 'user'), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @fixture_user_id = LAST_INSERT_ID();

INSERT INTO menu (name, price, status, created_at, updated_at)
SELECT CONCAT(@fixture_prefix, sequence_number), 3000,
       IF(sequence_number <= 15, 'ACTIVE', 'INACTIVE'), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (
    SELECT 1 AS sequence_number UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) AS menu_numbers;

CREATE TEMPORARY TABLE benchmark_menu_ids AS
SELECT id, CAST(SUBSTRING_INDEX(name, '-', -1) AS UNSIGNED) AS sequence_number
FROM menu
WHERE name LIKE CONCAT(@fixture_prefix, '%');

-- 500,000 orders: 10% recent seven-day range, 90% older range; all COMPLETED.
INSERT INTO orders (user_id, menu_id, order_price, status, ordered_at)
WITH digits AS (
    SELECT 0 AS digit UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
), sequence_numbers AS (
    SELECT d0.digit + 10 * d1.digit + 100 * d2.digit + 1000 * d3.digit + 10000 * d4.digit + 100000 * d5.digit AS n
    FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3 CROSS JOIN digits d4 CROSS JOIN digits d5
)
SELECT @fixture_user_id,
       menus.id,
       3000,
       'COMPLETED',
       CASE WHEN numbers.n % 10 = 0
            THEN @from_inclusive + INTERVAL (numbers.n % 604800) SECOND
            ELSE @from_inclusive - INTERVAL (1 + (numbers.n % 365)) DAY
       END
FROM sequence_numbers numbers
JOIN benchmark_menu_ids menus ON menus.sequence_number = (numbers.n % 20) + 1
WHERE numbers.n < 500000;

ANALYZE TABLE orders;

-- Hibernate JPQL과 동등한 측정 SQL. :fromInclusive=2026-07-08T15:00:00Z, :toExclusive=2026-07-15T15:00:00Z
SELECT m.id AS menu_id, m.name, COUNT(o.id) AS order_count
FROM menu m
LEFT JOIN orders o ON o.menu_id = m.id
    AND o.status = 'COMPLETED'
    AND o.ordered_at >= @from_inclusive
    AND o.ordered_at < @to_exclusive
WHERE m.status = 'ACTIVE'
GROUP BY m.id, m.name
ORDER BY COUNT(o.id) DESC, m.id ASC
LIMIT 3;

-- BENCHMARK_CLEANUP: fixture 외의 데이터는 삭제하지 않는다.
-- DELETE o FROM orders o JOIN users u ON u.id = o.user_id
-- WHERE u.name = CONCAT(@fixture_prefix, 'user');
-- DELETE FROM menu WHERE name LIKE CONCAT(@fixture_prefix, '%');
-- DELETE FROM users WHERE name = CONCAT(@fixture_prefix, 'user');
