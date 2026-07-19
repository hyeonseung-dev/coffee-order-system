package com.example.coffeeordersystem.dto;

/**
 * 포인트 API 오류 응답 DTO다.
 *
 * 전역 예외 처리기가 ErrorCode 또는 Spring 예외를 이 구조로 변환한다.
 *
 * @param code 클라이언트가 오류 종류를 식별할 수 있는 코드
 * @param message 사용자 또는 개발자가 원인을 파악할 수 있는 메시지
 */
public record PointErrorResponse(
		String code,
		String message
) {
}
