package com.example.coffeeordersystem.dto;

import java.time.LocalDateTime;

/** 결제 완료된 주문의 식별값, 결제 금액, 남은 포인트를 표현하는 응답 DTO다. */
public record OrderResponse(Long orderId, Long userId, Long menuId, Long orderPrice,
							Long remainingBalance, LocalDateTime orderedAt) {
}
