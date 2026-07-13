package com.example.coffeeordersystem.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** 인기 메뉴 집계의 날짜 기준이 서버 기본 시간대와 무관하게 KST로 고정되는지 검증한다. */
class ClockConfigTest {

	@Test
	void Clock은_Asia_Seoul_시간대를_사용한다() {
		ClockConfig clockConfig = new ClockConfig();

		assertThat(clockConfig.clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
	}
}
