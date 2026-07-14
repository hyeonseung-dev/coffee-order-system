package com.example.coffeeordersystem.dto;

import com.example.coffeeordersystem.repository.PopularMenuProjection;

/** 최근 7개 완료 일자의 메뉴별 주문 수를 표현하는 응답 DTO다. */
public record PopularMenuResponse(
		Long menuId,
		String name,
		Long orderCount
) {
	public static PopularMenuResponse from(PopularMenuProjection projection) {
		return new PopularMenuResponse(
				projection.getMenuId(),
				projection.getName(),
				projection.getOrderCount()
		);
	}
}
