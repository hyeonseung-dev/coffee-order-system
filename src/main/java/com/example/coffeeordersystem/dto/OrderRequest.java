package com.example.coffeeordersystem.dto;

import jakarta.validation.constraints.NotNull;

/** 주문할 사용자와 메뉴를 표현하는 요청 DTO다. */
public record OrderRequest(
		@NotNull(message = "사용자 ID는 필수입니다.") Long userId,
		@NotNull(message = "메뉴 ID는 필수입니다.") Long menuId
) {
}
