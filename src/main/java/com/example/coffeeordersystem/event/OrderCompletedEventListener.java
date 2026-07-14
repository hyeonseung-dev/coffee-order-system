package com.example.coffeeordersystem.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 완료 주문을 데이터 수집 플랫폼으로 전달하는 흐름을 로그로 모의한다.
 *
 * AFTER_COMMIT을 사용하므로 롤백된 주문에는 실행되지 않고, 이 처리의 실패는 이미
 * Commit된 주문·포인트 트랜잭션을 되돌리지 않는다.
 */
@Component
public class OrderCompletedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderCompletedEventListener.class);

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(OrderCompletedEvent event) {
		log.info("Mock data-platform order completed: orderId={}, userId={}, menuId={}, orderPrice={}, orderedAt={}, businessZone={}",
				event.orderId(), event.userId(), event.menuId(), event.orderPrice(), event.orderedAt(), event.businessZone());
	}
}
