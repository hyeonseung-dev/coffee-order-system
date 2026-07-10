package com.example.coffeeordersystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 포인트를 보유하고 주문을 수행할 사용자를 표현하는 Entity다.
 *
 * 현재 포인트 충전 기능에서는 사용자 존재 여부를 확인하는 기준이며,
 * 포인트 지갑이나 이력과의 양방향 연관관계는 만들지 않는다.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected User() {
	}

	private User(String name) {
		this.name = name;
	}

	/**
	 * 지정한 이름으로 사용자를 생성한다.
	 *
	 * Entity 생성 책임을 호출 계층에 흩뜨리지 않기 위해
	 * public setter 대신 정적 팩토리를 제공한다.
	 *
	 * @param name 사용자 이름
	 * @return 새 사용자 Entity
	 */
	public static User create(String name) {
		return new User(name);
	}

	@PrePersist
	void prePersist() {
		// JPA 저장 시점에 생성일과 수정일을 함께 채운다.
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		// 사용자 정보가 변경되어 flush될 때 수정일만 갱신한다.
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
