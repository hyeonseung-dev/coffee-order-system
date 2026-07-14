package com.example.coffeeordersystem.dto;

import java.util.List;

/** 인기 메뉴 조회 API의 최상위 응답 DTO다. */
public record PopularMenuListResponse(
		List<PopularMenuResponse> data
) {
}
