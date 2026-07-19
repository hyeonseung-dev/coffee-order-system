package com.example.coffeeordersystem.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.coffeeordersystem.event.OrderCompletedOutboxPayload;

/** 실제 네트워크 대신 로그로 외부 데이터 플랫폼 전송을 모의한다. */
@Component
public class MockOrderDataPlatformClient implements OrderDataPlatformClient {

	private static final Logger log = LoggerFactory.getLogger(MockOrderDataPlatformClient.class);

	@Override
	public void sendOrderCompleted(OrderCompletedOutboxPayload payload) {
		log.info("Mock data-platform order completed: eventId={}, orderId={}", payload.eventId(), payload.orderId());
	}
}
