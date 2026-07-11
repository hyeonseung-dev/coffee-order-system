package com.example.coffeeordersystem.dto;

import com.example.coffeeordersystem.domain.Menu;

/**
 * 메뉴 목록 조회 API에서 개별 메뉴를 표현하는 응답 DTO다.
 *
 * Entity를 직접 노출하지 않고, 클라이언트에 필요한 메뉴 ID, 이름, 가격만 전달한다.
 *
 * @param menuId 메뉴 식별자
 * @param name 메뉴명
 * @param price 메뉴 가격
 */
public record MenuResponse(
		Long menuId,
		String name,
		Long price
) {

	/**
	 * Menu Entity에서 API 응답에 필요한 값만 추출한다.
	 *
	 * @param menu 조회된 메뉴 Entity
	 * @return 개별 메뉴 응답 DTO
	 */
	public static MenuResponse from(Menu menu) {
		return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice());
	}
}
