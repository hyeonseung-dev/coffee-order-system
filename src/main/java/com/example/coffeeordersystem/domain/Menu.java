package com.example.coffeeordersystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 주문 가능한 커피 메뉴를 표현하는 Entity다.
 *
 * 메뉴명, 가격, 판매 상태를 보유하며 메뉴 목록 조회 API에서는
 * ACTIVE 상태의 메뉴만 응답 DTO로 변환되어 노출된다.
 */
@Entity
@Table(name = "menu")
public class Menu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private Long price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MenuStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected Menu() {
	}

	public Menu(String name, Long price, MenuStatus status) {
		this.name = name;
		this.price = price;
		this.status = status;
	}

	/**
	 * 지정한 이름, 가격, 상태로 메뉴를 생성한다.
	 *
	 * 테스트와 개발용 초기 데이터에서 생성 의도를 드러내기 위해
	 * 필드 setter 대신 정적 팩토리를 사용한다.
	 *
	 * @param name 메뉴명
	 * @param price 메뉴 가격
	 * @param status 메뉴 판매 상태
	 * @return 새 메뉴 Entity
	 */
	public static Menu create(String name, Long price, MenuStatus status) {
		return new Menu(name, price, status);
	}

	@PrePersist
	void prePersist() {
		// JPA가 처음 저장하는 시점에 생성일과 수정일을 같은 값으로 맞춘다.
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		// Entity가 변경되어 flush될 때 수정일만 갱신한다.
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getPrice() {
		return price;
	}

	public MenuStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
