package com.example.coffeeordersystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 포인트 충전 API의 요청 DTO다.
 *
 * amount는 충전할 포인트 금액이며 null이 아니고 0보다 커야 한다.
 * 검증 실패는 전역 예외 처리에서 INVALID_REQUEST 응답으로 변환된다.
 *
 * @param amount 충전할 포인트 금액
 */
public record PointChargeRequest(
		@NotNull(message = "충전 금액은 필수입니다.")
		@Positive(message = "충전 금액은 0보다 커야 합니다.")
		Long amount
) {
}
