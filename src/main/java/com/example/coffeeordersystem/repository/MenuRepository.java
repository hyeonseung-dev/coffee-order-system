package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
