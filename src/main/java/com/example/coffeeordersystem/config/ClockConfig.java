package com.example.coffeeordersystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** 애플리케이션의 날짜 기반 규칙이 같은 KST 기준 시각을 사용하도록 Clock을 제공한다. */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
