package com.example.coffeeordersystem.event;

/**
 * 결제 완료 주문을 트랜잭션 Commit 이후 후속 처리에 전달하는 이벤트다.
 *
 * 외부 플랫폼 전송에 필요한 최소 식별값과 결제 금액만 포함한다.
 */
public record OrderCompletedEvent(Long orderId, Long userId, Long menuId, Long orderPrice) {
}
