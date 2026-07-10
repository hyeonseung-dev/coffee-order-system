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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 사용자의 포인트 변동 이력을 저장하는 Entity다.
 *
 * 충전 또는 사용 시점의 금액과 변동 후 잔액을 보존해
 * 지갑 잔액 변경의 감사 기록으로 사용한다.
 */
@Entity
@Table(name = "point_history")
public class PointHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointHistoryType type;

	@Column(nullable = false)
	private Long balanceAfter;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected PointHistory() {
	}

	private PointHistory(User user, Long amount, PointHistoryType type, Long balanceAfter) {
		this.user = user;
		this.amount = amount;
		this.type = type;
		this.balanceAfter = balanceAfter;
	}

	/**
	 * 포인트 충전 이력을 생성한다.
	 *
	 * 충전 금액과 충전 후 잔액을 함께 저장해,
	 * 이후 지갑 잔액만으로는 알 수 없는 변동 당시 상태를 보존한다.
	 *
	 * @param user 충전한 사용자
	 * @param amount 충전 금액
	 * @param balanceAfter 충전 후 잔액
	 * @return CHARGE 타입의 포인트 이력 Entity
	 */
	public static PointHistory charge(User user, long amount, long balanceAfter) {
		return new PointHistory(user, amount, PointHistoryType.CHARGE, balanceAfter);
	}

	@PrePersist
	void prePersist() {
		// 이력은 생성 후 수정하지 않으므로 생성 시각만 저장한다.
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Long getAmount() {
		return amount;
	}

	public PointHistoryType getType() {
		return type;
	}

	public Long getBalanceAfter() {
		return balanceAfter;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
