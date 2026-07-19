package com.example.coffeeordersystem.domain;

/**
 * 메뉴의 판매 가능 상태를 표현한다.
 *
 * 메뉴 목록 조회에서는 ACTIVE 메뉴만 노출하고,
 * INACTIVE 메뉴는 응답에서 제외된다.
 */
public enum MenuStatus {
	ACTIVE,
	INACTIVE
}
