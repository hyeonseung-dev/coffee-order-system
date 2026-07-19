package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.OrderStatus;
import com.example.coffeeordersystem.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 인기 메뉴 JPQL이 날짜 경계, 활성 메뉴, 0건 메뉴와 결정적 정렬을 DB에서 처리하는지 검증한다. */
@SpringBootTest
@ActiveProfiles({"test", "prod"})
class MenuRepositoryPopularMenuIntegrationTest {

	@Autowired private MenuRepository menuRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private JdbcTemplate jdbcTemplate;

	private User user;
	private Menu americano;
	private Menu latte;
	private Menu zeroOrder;
	private Menu fourthMenu;
	private Menu inactive;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("popular-menu-test-user"));
		americano = menuRepository.save(Menu.create("popular-americano", 3000L, MenuStatus.ACTIVE));
		latte = menuRepository.save(Menu.create("popular-latte", 4000L, MenuStatus.ACTIVE));
		zeroOrder = menuRepository.save(Menu.create("popular-zero", 4500L, MenuStatus.ACTIVE));
		fourthMenu = menuRepository.save(Menu.create("popular-fourth", 5000L, MenuStatus.ACTIVE));
		inactive = menuRepository.save(Menu.create("popular-inactive", 5000L, MenuStatus.INACTIVE));
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("DELETE FROM orders WHERE user_id = ?", user.getId());
		menuRepository.deleteAll(List.of(americano, latte, zeroOrder, fourthMenu, inactive));
		userRepository.deleteById(user.getId());
	}

	@Test
	void 완료_주문만_집계하고_날짜_경계와_동률_정렬과_0건_메뉴를_적용한다() {
		orderAt(americano, Instant.parse("2026-07-05T15:00:00Z"));
		orderAt(americano, Instant.parse("2026-07-10T03:00:00Z"));
		orderAt(latte, Instant.parse("2026-07-07T01:00:00Z"));
		orderAt(latte, Instant.parse("2026-07-11T01:00:00Z"));
		orderAt(americano, Instant.parse("2026-07-05T14:59:59Z"));
		orderAt(americano, Instant.parse("2026-07-12T15:00:00Z"));
		orderAt(inactive, Instant.parse("2026-07-10T03:00:00Z"));

		List<PopularMenuProjection> result = menuRepository.findPopularMenus(
				MenuStatus.ACTIVE, OrderStatus.COMPLETED,
				Instant.parse("2026-07-05T15:00:00Z"),
				Instant.parse("2026-07-12T15:00:00Z"),
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
				Instant.parse("2026-07-05T15:00:00Z"),
				Instant.parse("2026-07-12T15:00:00Z"),
				PageRequest.of(0, 3));

		assertThat(result).extracting(PopularMenuProjection::getMenuId)
				.containsExactly(americano.getId(), latte.getId(), zeroOrder.getId());
		assertThat(result).extracting(PopularMenuProjection::getOrderCount)
				.containsOnly(0L);
	}

	@Test
	void ACTIVE_메뉴가_3개_미만이면_존재하는_메뉴만_반환한다() {
		menuRepository.deleteAll(List.of(zeroOrder, fourthMenu));

		List<PopularMenuProjection> result = findPopularMenus();

		assertThat(result).extracting(PopularMenuProjection::getMenuId)
				.containsExactly(americano.getId(), latte.getId());
	}

	@Test
	void ACTIVE_메뉴가_없으면_빈_목록을_반환한다() {
		menuRepository.deleteAll(List.of(americano, latte, zeroOrder, fourthMenu));

		assertThat(findPopularMenus()).isEmpty();
	}

	@Test
	void KST_날짜_시작은_포함하고_다음날_시작은_제외하며_자정직후와_오전_주문을_포함한다() {
		Instant fromInclusive = Instant.parse("2026-07-13T15:00:00Z");
		Instant toExclusive = Instant.parse("2026-07-14T15:00:00Z");
		orderAt(americano, fromInclusive);
		orderAt(americano, Instant.parse("2026-07-13T15:30:00Z"));
		orderAt(latte, Instant.parse("2026-07-14T01:00:00Z"));
		orderAt(latte, toExclusive);

		List<PopularMenuProjection> result = menuRepository.findPopularMenus(
				MenuStatus.ACTIVE, OrderStatus.COMPLETED, fromInclusive, toExclusive, PageRequest.of(0, 3));

		assertThat(result).extracting(PopularMenuProjection::getMenuId)
				.containsExactly(americano.getId(), latte.getId(), zeroOrder.getId());
		assertThat(result).extracting(PopularMenuProjection::getOrderCount)
				.containsExactly(2L, 1L, 0L);
	}

	private List<PopularMenuProjection> findPopularMenus() {
		return menuRepository.findPopularMenus(
				MenuStatus.ACTIVE, OrderStatus.COMPLETED,
				Instant.parse("2026-07-05T15:00:00Z"),
				Instant.parse("2026-07-12T15:00:00Z"),
				PageRequest.of(0, 3));
	}

	private void orderAt(Menu menu, Instant orderedAt) {
		jdbcTemplate.update(
				"INSERT INTO orders (user_id, menu_id, order_price, status, ordered_at) VALUES (?, ?, ?, ?, ?)",
				user.getId(), menu.getId(), menu.getPrice(), OrderStatus.COMPLETED.name(), orderedAt
		);
	}
}
