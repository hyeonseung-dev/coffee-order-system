package com.example.coffeeordersystem.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

	@Test
	void Clock은_Asia_Seoul_시간대를_사용한다() {
		assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
	}
}
