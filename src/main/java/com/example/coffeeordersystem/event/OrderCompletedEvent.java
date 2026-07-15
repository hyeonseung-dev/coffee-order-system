package com.example.coffeeordersystem.event;

import java.time.Instant;

/** 주문 후속 처리에 필요한 완료 주문 정보만 전달하는 Event다. */
public record OrderCompletedEvent(Long orderId, Long userId, Long menuId, Long orderPrice,
								 Instant orderedAt, String businessZone) {
}
