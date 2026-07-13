package com.example.coffeeordersystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 애플리케이션에서 현재 날짜와 시간을 일관되게 주입하기 위한 설정이다. */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
