package com.example.coffeeordersystem;

import org.junit.jupiter.api.Test;

class CoffeeOrderSystemApplicationTests {

	@Test
	void 애플리케이션_클래스를_생성할_수_있다() {
		// given
		CoffeeOrderSystemApplication application = new CoffeeOrderSystemApplication();

		// when & then
		org.assertj.core.api.Assertions.assertThat(application).isNotNull();
	}

}
