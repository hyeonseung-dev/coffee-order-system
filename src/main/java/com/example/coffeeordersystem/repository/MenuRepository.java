package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메뉴 Entity의 영속성 접근을 담당하는 Repository다.
 *
 * 메뉴 목록 조회 기능에서 판매 상태와 정렬 조건을 DB 조회로 표현한다.
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

	/**
	 * 지정한 상태의 메뉴를 ID 오름차순으로 조회한다.
	 *
	 * @param status 조회할 메뉴 상태
	 * @return 조건에 맞는 메뉴 Entity 목록
	 */
	List<Menu> findAllByStatusOrderByIdAsc(MenuStatus status);

	@Query("""
			SELECT m.id AS menuId, m.name AS name, COUNT(o.id) AS orderCount
			FROM Menu m
			LEFT JOIN Order o ON o.menu = m
				AND o.status = :orderStatus
				AND o.orderedAt >= :fromInclusive
				AND o.orderedAt < :toExclusive
			WHERE m.status = :menuStatus
			GROUP BY m.id, m.name
			ORDER BY COUNT(o.id) DESC, m.id ASC
			""")
	List<PopularMenuProjection> findPopularMenus(
			@Param("menuStatus") MenuStatus menuStatus,
			@Param("orderStatus") OrderStatus orderStatus,
			@Param("fromInclusive") LocalDateTime fromInclusive,
			@Param("toExclusive") LocalDateTime toExclusive,
			Pageable pageable
	);
}
