package com.example.coffeeordersystem.exception;

import com.example.coffeeordersystem.dto.PointErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller 밖으로 전파된 예외를 HTTP 오류 응답으로 변환하는 전역 예외 처리기다.
 *
 * 현재는 포인트 API의 오류 응답 DTO를 사용하며,
 * 비즈니스 예외와 Spring 요청 처리 예외를 분리해서 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Service 또는 도메인 계층에서 발생한 비즈니스 규칙 위반을 처리한다.
	 *
	 * @param exception ErrorCode를 포함한 비즈니스 예외
	 * @return ErrorCode의 HTTP 상태와 코드, 예외 메시지를 담은 오류 응답
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<PointErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(new PointErrorResponse(errorCode.getCode(), exception.getMessage()));
	}

	/**
	 * 요청 JSON이 DTO로 변환된 뒤 Bean Validation에 실패한 경우를 처리한다.
	 *
	 * 첫 번째 필드 검증 메시지를 응답해 클라이언트가 수정할 값을 바로 알 수 있게 한다.
	 *
	 * @param exception Spring MVC 검증 실패 예외
	 * @return INVALID_REQUEST 코드와 검증 메시지를 담은 400 응답
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<PointErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
		// 여러 필드가 실패해도 현재 API는 가장 먼저 발견된 검증 메시지만 응답한다.
		String message = exception.getBindingResult().getFieldErrors()
				.stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse(errorCode.getMessage());
		return ResponseEntity.status(errorCode.getStatus())
				.body(new PointErrorResponse(errorCode.getCode(), message));
	}

	/**
	 * 잘못된 JSON 문법 또는 DTO 필드 타입 불일치로 요청 본문 역직렬화에 실패한 경우를 처리한다.
	 *
	 * @return INVALID_REQUEST_BODY 코드와 기본 메시지를 담은 400 응답
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<PointErrorResponse> handleHttpMessageNotReadable() {
		ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
		return ResponseEntity.status(errorCode.getStatus())
				.body(new PointErrorResponse(errorCode.getCode(), errorCode.getMessage()));
	}
}
