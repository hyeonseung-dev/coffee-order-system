package com.example.coffeeordersystem.exception;

import org.springframework.http.HttpStatus;

/**
 * API 오류 응답에 필요한 HTTP 상태, 오류 코드, 기본 메시지를 정의한다.
 *
 * 비즈니스 예외와 Spring 요청 처리 예외가 같은 응답 DTO로 내려가도록
 * 오류 정보를 한 곳에서 관리한다.
 */
public enum ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다."),
	POINT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "POINT_WALLET_NOT_FOUND", "포인트 지갑을 찾을 수 없습니다."),
	INACTIVE_MENU(HttpStatus.BAD_REQUEST, "INACTIVE_MENU", "비활성 메뉴는 주문할 수 없습니다."),
	INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT", "포인트 잔액이 부족합니다."),
	INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_CHARGE_AMOUNT", "충전 금액은 0보다 커야 합니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
	INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "요청 본문을 읽을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
