package com.example.coffeeordersystem.dto;

/**
 * 포인트 충전 API의 최상위 성공 응답 DTO다.
 *
 * 전역 공통 응답 구조를 만들지 않고, 현재 API 명세의 data 필드만 감싼다.
 *
 * @param data 포인트 충전 결과
 */
public record PointChargeResultResponse(
		PointChargeResponse data
) {
}
