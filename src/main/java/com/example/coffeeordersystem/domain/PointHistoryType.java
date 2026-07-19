package com.example.coffeeordersystem.domain;

/**
 * 포인트 이력의 변동 종류를 표현한다.
 *
 * CHARGE는 포인트 증가 이력, USE는 포인트 차감 이력을 의미한다.
 */
public enum PointHistoryType {
	CHARGE,
	USE
}
