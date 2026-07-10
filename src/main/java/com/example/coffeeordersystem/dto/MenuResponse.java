package com.example.coffeeordersystem.dto;

import com.example.coffeeordersystem.domain.Menu;

public record MenuResponse(
		Long menuId,
		String name,
		Long price
) {

	public static MenuResponse from(Menu menu) {
		return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice());
	}
}
