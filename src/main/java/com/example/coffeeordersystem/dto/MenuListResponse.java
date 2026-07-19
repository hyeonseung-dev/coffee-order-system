package com.example.coffeeordersystem.dto;

import java.util.List;

/**
 * 메뉴 목록 조회 API의 최상위 응답 DTO다.
 *
 * 공통 응답 추상화 없이, API 명세의 data 필드에 개별 메뉴 응답 목록을 담는다.
 *
 * @param data 주문 가능한 메뉴 응답 목록
 */
public record MenuListResponse(
		List<MenuResponse> data
) {
}
