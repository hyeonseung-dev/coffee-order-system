package com.example.coffeeordersystem.domain;

/**
 * 주문의 처리 상태를 표현한다.
 *
 * Issue #8에서는 결제까지 완료된 성공 주문만 저장하므로 COMPLETED만 사용한다.
 */
public enum OrderStatus {
	COMPLETED
}
