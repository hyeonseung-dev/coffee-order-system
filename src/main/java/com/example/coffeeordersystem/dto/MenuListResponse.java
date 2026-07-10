package com.example.coffeeordersystem.dto;

import java.util.List;

public record MenuListResponse(
		List<MenuResponse> data
) {
}
