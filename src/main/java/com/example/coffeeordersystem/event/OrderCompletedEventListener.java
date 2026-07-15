package com.example.coffeeordersystem.event;

import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Commit된 주문 Event를 받아 외부 데이터 플랫폼으로 주문 완료 정보를 전달한다. */
@Component
public class OrderCompletedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderCompletedEventListener.class);

	private final OrderDataPlatformClient orderDataPlatformClient;

	public OrderCompletedEventListener(OrderDataPlatformClient orderDataPlatformClient) {
		this.orderDataPlatformClient = orderDataPlatformClient;
	}

	@Async("orderFollowUpExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(OrderCompletedEvent event) {
		log.info("[LISTENER] orderId={} thread={}", event.orderId(), Thread.currentThread().getName());
		orderDataPlatformClient.sendOrderCompleted(event.orderId(), event.userId(), event.menuId(), event.orderPrice(),
				event.orderedAt(), event.businessZone());
	}
}
