package com.example.coffeeordersystem.dto;

/**
 * 포인트 충전 성공 결과를 표현하는 응답 DTO다.
 *
 * 충전 대상 사용자, 충전된 금액, 충전 후 잔액을 포함한다.
 *
 * @param userId 충전 대상 사용자 식별자
 * @param chargedAmount 이번 요청으로 충전된 포인트 금액
 * @param balance 충전 후 포인트 잔액
 */
public record PointChargeResponse(
		Long userId,
		Long chargedAmount,
		Long balance
) {
}
