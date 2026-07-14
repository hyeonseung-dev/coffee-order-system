package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** 고정 Clock으로 생성한 주문 시각이 응답과 영속 데이터에 동일하게 저장되는지 검증한다. */
@SpringBootTest
@ActiveProfiles({"test", "prod"})
@Import(OrderClockIntegrationTest.FixedClockConfiguration.class)
class OrderClockIntegrationTest {

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;

	private User user;
	private Menu menu;
	private PointWallet wallet;
	private Long orderId;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("order-clock-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("order-clock-menu", 3000L, MenuStatus.ACTIVE));
	}

	@AfterEach
	void tearDown() {
		if (orderId != null) {
			orderRepository.deleteById(orderId);
		}
		pointHistoryRepository.deleteAll();
		pointWalletRepository.deleteById(wallet.getId());
		menuRepository.deleteById(menu.getId());
		userRepository.deleteById(user.getId());
	}

	@Test
	void 고정_Clock의_KST_시각을_응답과_orders에_동일하게_저장한다() {
		OrderResponse response = orderService.order(user.getId(), menu.getId());
		orderId = response.orderId();

		LocalDateTime expected = LocalDateTime.of(2026, 7, 12, 23, 59, 59);
		assertThat(response.orderedAt()).isEqualTo(expected);
		assertThat(orderRepository.findById(orderId).orElseThrow().getOrderedAt()).isEqualTo(expected);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-07-12T14:59:59Z"), ZoneId.of("Asia/Seoul"));
		}
	}
}
