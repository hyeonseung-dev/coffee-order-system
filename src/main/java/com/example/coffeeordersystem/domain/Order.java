package com.example.coffeeordersystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 포인트 결제가 완료된 커피 주문을 저장하는 Entity다.
 *
 * 실패한 요청은 생성하지 않으며, 성공 주문만 인기 메뉴 집계의 원본 데이터가 된다.
 */
@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private Menu menu;

	@Column(nullable = false)
	private Long orderPrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false)
	private LocalDateTime orderedAt;

	protected Order() {
	}

	private Order(User user, Menu menu, long orderPrice, LocalDateTime orderedAt) {
		this.user = user;
		this.menu = menu;
		this.orderPrice = orderPrice;
		this.status = OrderStatus.COMPLETED;
		this.orderedAt = orderedAt;
	}

	/**
	 * 결제가 완료된 주문을 생성한다.
	 *
	 * 주문 상태는 호출자가 변경할 수 없도록 COMPLETED로 고정한다.
	 */
	public static Order completed(User user, Menu menu, long orderPrice, LocalDateTime orderedAt) {
		return new Order(user, menu, orderPrice, orderedAt);
	}

	public Long getId() { return id; }
	public User getUser() { return user; }
	public Menu getMenu() { return menu; }
	public Long getOrderPrice() { return orderPrice; }
	public OrderStatus getStatus() { return status; }
	public LocalDateTime getOrderedAt() { return orderedAt; }
}
