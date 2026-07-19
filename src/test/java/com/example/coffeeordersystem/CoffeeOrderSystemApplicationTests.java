package com.example.coffeeordersystem;

import org.junit.jupiter.api.Test;

/**
 * 애플리케이션 시작 클래스가 기본 생성 가능한지 확인하는 최소 단위 테스트다.
 */
class CoffeeOrderSystemApplicationTests {

	@Test
	void 애플리케이션_클래스를_생성할_수_있다() {
		// given
		CoffeeOrderSystemApplication application = new CoffeeOrderSystemApplication();

		// when & then
		org.assertj.core.api.Assertions.assertThat(application).isNotNull();
	}

}
