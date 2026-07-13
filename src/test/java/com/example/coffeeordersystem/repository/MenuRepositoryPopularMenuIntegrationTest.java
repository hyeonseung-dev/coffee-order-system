package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.Order;
import com.example.coffeeordersystem.domain.OrderStatus;
import com.example.coffeeordersystem.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 인기 메뉴 JPQL이 날짜 경계, 활성 메뉴, 0건 메뉴와 결정적 정렬을 DB에서 처리하는지 검증한다. */
@SpringBootTest
@ActiveProfiles({"test", "prod"})
class MenuRepositoryPopularMenuIntegrationTest {

	@Autowired private MenuRepository menuRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private UserRepository userRepository;

	private User user;
	private Menu americano;
	private Menu latte;
	private Menu zeroOrder;
	private Menu inactive;
	private final List<Order> orders = new java.util.ArrayList<>();

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("popular-menu-test-user"));
		americano = menuRepository.save(Menu.create("popular-americano", 3000L, MenuStatus.ACTIVE));
		latte = menuRepository.save(Menu.create("popular-latte", 4000L, MenuStatus.ACTIVE));
		zeroOrder = menuRepository.save(Menu.create("popular-zero", 4500L, MenuStatus.ACTIVE));
		inactive = menuRepository.save(Menu.create("popular-inactive", 5000L, MenuStatus.INACTIVE));
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll(orders);
		menuRepository.deleteAll(List.of(americano, latte, zeroOrder, inactive));
		userRepository.deleteById(user.getId());
	}

	@Test
	void 완료_주문만_집계하고_날짜_경계와_동률_정렬과_0건_메뉴를_적용한다() {
		orderAt(americano, LocalDateTime.of(2026, 7, 6, 0, 0));
		orderAt(americano, LocalDateTime.of(2026, 7, 10, 12, 0));
		orderAt(latte, LocalDateTime.of(2026, 7, 7, 10, 0));
		orderAt(latte, LocalDateTime.of(2026, 7, 11, 10, 0));
		orderAt(americano, LocalDateTime.of(2026, 7, 5, 23, 59, 59));
		orderAt(americano, LocalDateTime.of(2026, 7, 13, 0, 0));
		orderAt(inactive, LocalDateTime.of(2026, 7, 10, 12, 0));

		List<PopularMenuProjection> result = menuRepository.findPopularMenus(
				MenuStatus.ACTIVE, OrderStatus.COMPLETED,
				LocalDateTime.of(2026, 7, 6, 0, 0),
				LocalDateTime.of(2026, 7, 13, 0, 0),
				PageRequest.of(0, 3));

		assertThat(result).extracting(PopularMenuProjection::getMenuId)
				.containsExactly(americano.getId(), latte.getId(), zeroOrder.getId());
		assertThat(result).extracting(PopularMenuProjection::getOrderCount)
				.containsExactly(2L, 2L, 0L);
	}

	@Test
	void 전체_주문이_0건이면_ACTIVE_메뉴를_ID_오름차순으로_최대_3개_반환한다() {
		List<PopularMenuProjection> result = menuRepository.findPopularMenus(
				MenuStatus.ACTIVE, OrderStatus.COMPLETED,
				LocalDateTime.of(2026, 7, 6, 0, 0),
				LocalDateTime.of(2026, 7, 13, 0, 0),
				PageRequest.of(0, 3));

		assertThat(result).extracting(PopularMenuProjection::getMenuId)
				.containsExactly(americano.getId(), latte.getId(), zeroOrder.getId());
		assertThat(result).extracting(PopularMenuProjection::getOrderCount)
				.containsOnly(0L);
	}

	private void orderAt(Menu menu, LocalDateTime orderedAt) {
		Order order = orderRepository.save(Order.completed(user, menu, menu.getPrice()));
		ReflectionTestUtils.setField(order, "orderedAt", orderedAt);
		orders.add(orderRepository.saveAndFlush(order));
	}
}
