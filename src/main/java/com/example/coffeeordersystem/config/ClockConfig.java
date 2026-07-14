package com.example.coffeeordersystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 주문 발생 시각을 환경과 무관한 UTC 절대 시각으로 생성할 Clock을 제공한다. */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
