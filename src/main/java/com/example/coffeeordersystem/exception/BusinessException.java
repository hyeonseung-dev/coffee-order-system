package com.example.coffeeordersystem.exception;

/**
 * 비즈니스 규칙 위반을 표현하는 공통 런타임 예외다.
 *
 * 도메인별 예외 클래스를 늘리지 않고 {@link ErrorCode}로
 * HTTP 상태와 오류 코드를 결정할 수 있게 한다.
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	/**
	 * ErrorCode의 기본 메시지를 사용하는 비즈니스 예외를 생성한다.
	 *
	 * @param errorCode 오류 응답에 사용할 코드 정보
	 */
	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/**
	 * ErrorCode는 유지하되 상황에 맞는 메시지를 별도로 전달할 때 사용한다.
	 *
	 * @param errorCode 오류 응답에 사용할 코드 정보
	 * @param message 기본 메시지 대신 응답할 메시지
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
