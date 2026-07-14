package com.example.coffeeordersystem.event;

import java.time.Instant;

/**
 * 결제 완료 주문을 트랜잭션 Commit 이후 후속 처리에 전달하는 이벤트다.
 *
 * 외부 플랫폼 전송에 필요한 식별값, 결제 금액, UTC 주문 시각과 업무 시간대를 포함한다.
 */
public record OrderCompletedEvent(Long orderId, Long userId, Long menuId, Long orderPrice,
								 Instant orderedAt, String businessZone) {
}
