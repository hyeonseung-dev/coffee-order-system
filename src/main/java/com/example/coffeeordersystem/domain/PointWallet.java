package com.example.coffeeordersystem.domain;

import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 사용자의 포인트 잔액을 보관하는 Entity다.
 *
 * 포인트 충전 시 잔액 변경의 중심이 되며,
 * 잔액은 setter가 아니라 도메인 메서드로만 변경한다.
 */
@Entity
@Table(name = "point_wallet")
public class PointWallet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	// 사용자당 하나의 지갑만 허용해야 하므로 user_id에 unique 제약을 둔다.
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false)
	private Long balance;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected PointWallet() {
	}

	private PointWallet(User user, Long balance) {
		this.user = user;
		this.balance = balance;
	}

	/**
	 * 지정한 사용자와 초기 잔액으로 포인트 지갑을 생성한다.
	 *
	 * 개발용 초기 데이터는 0원 지갑을 만들고,
	 * 이후 잔액 변경은 {@link #charge(long)}를 통해서만 수행한다.
	 *
	 * @param user 지갑을 소유할 사용자
	 * @param balance 초기 잔액
	 * @return 새 포인트 지갑 Entity
	 */
	public static PointWallet create(User user, Long balance) {
		return new PointWallet(user, balance);
	}

	/**
	 * 포인트 지갑 잔액을 충전 금액만큼 증가시킨다.
	 *
	 * 충전 금액은 0보다 커야 하며, 유효하지 않으면 잔액을 변경하지 않고
	 * {@link BusinessException}을 발생시킨다.
	 *
	 * @param amount 충전할 포인트 금액
	 * @throws BusinessException 충전 금액이 0 이하인 경우
	 */
	public void charge(long amount) {
		validateChargeAmount(amount);
		this.balance += amount;
	}

	private void validateChargeAmount(long amount) {
		if (amount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT);
		}
	}

	@PrePersist
	void prePersist() {
		// JPA 저장 시점에 지갑 생성일과 수정일을 같은 값으로 맞춘다.
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		// 잔액 변경이 flush될 때 수정일을 갱신한다.
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Long getBalance() {
		return balance;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
