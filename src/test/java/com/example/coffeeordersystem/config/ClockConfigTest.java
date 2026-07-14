package com.example.coffeeordersystem.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

	@Test
	void Clock은_UTC_시간대를_사용한다() {
		assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}
}
