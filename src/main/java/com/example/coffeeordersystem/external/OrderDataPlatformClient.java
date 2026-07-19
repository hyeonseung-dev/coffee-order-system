package com.example.coffeeordersystem.external;

import com.example.coffeeordersystem.event.OrderCompletedOutboxPayload;

/** 주문 완료 정보를 외부 데이터 플랫폼으로 전달한다. */
public interface OrderDataPlatformClient {

	void sendOrderCompleted(OrderCompletedOutboxPayload payload);
}
