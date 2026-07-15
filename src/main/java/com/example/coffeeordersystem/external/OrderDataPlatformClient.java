package com.example.coffeeordersystem.external;

import java.time.Instant;

/** 주문 완료 정보를 외부 데이터 플랫폼으로 전달한다. */
public interface OrderDataPlatformClient {

	void sendOrderCompleted(Long orderId, Long userId, Long menuId, Long orderPrice,
			Instant orderedAt, String businessZone);
}
