package com.example.coffeeordersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 커피 주문 시스템 Spring Boot 애플리케이션의 시작점이다.
 *
 * 컴포넌트 스캔과 자동 설정의 기준 패키지를 제공하며,
 * 실제 API 흐름은 Controller, Service, Repository 계층으로 위임한다.
 */
@SpringBootApplication
public class CoffeeOrderSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeeOrderSystemApplication.class, args);
	}

}
