package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.dto.PopularMenuResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.PopularMenuProjection;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 메뉴 조회 Service가 ACTIVE 메뉴 조회와 DTO 변환 규칙을 지키는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

	@Mock
	private MenuRepository menuRepository;
	@Mock
	private Clock clock;

	@InjectMocks
	private MenuService menuService;

	@Test
	void 활성_메뉴를_ID_오름차순으로_응답_DTO로_반환한다() {
		// given
		Menu americano = menu(1L, "Americano", 3000L, MenuStatus.ACTIVE);
		Menu latte = menu(2L, "Latte", 4000L, MenuStatus.ACTIVE);
		when(menuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE))
				.thenReturn(List.of(americano, latte));

		// when
		List<MenuResponse> responses = menuService.findActiveMenus();

		// then
		assertThat(responses).containsExactly(
				new MenuResponse(1L, "Americano", 3000L),
				new MenuResponse(2L, "Latte", 4000L)
		);
		verify(menuRepository).findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE);
	}

	@Test
	void 활성_메뉴가_없으면_빈_목록을_반환한다() {
		// given
		when(menuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE))
				.thenReturn(List.of());

		// when
		List<MenuResponse> responses = menuService.findActiveMenus();

		// then
		assertThat(responses).isEmpty();
	}

	@Test
	void 직전_7개_완료_일자와_TOP3_조건으로_인기_메뉴를_조회한다() {
		when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
		when(clock.instant()).thenReturn(Instant.parse("2026-07-13T03:00:00Z"));
		PopularMenuProjection americano = popularMenu(1L, "Americano", 5L);
		PopularMenuProjection latte = popularMenu(2L, "Latte", 3L);
		when(menuRepository.findPopularMenus(
				org.mockito.ArgumentMatchers.eq(MenuStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(com.example.coffeeordersystem.domain.OrderStatus.COMPLETED),
				org.mockito.ArgumentMatchers.eq(java.time.LocalDateTime.of(2026, 7, 6, 0, 0)),
				org.mockito.ArgumentMatchers.eq(java.time.LocalDateTime.of(2026, 7, 13, 0, 0)),
				org.mockito.ArgumentMatchers.any(Pageable.class)
		)).thenReturn(List.of(americano, latte));

		List<PopularMenuResponse> responses = menuService.findPopularMenus();

		assertThat(responses).containsExactly(
				new PopularMenuResponse(1L, "Americano", 5L),
				new PopularMenuResponse(2L, "Latte", 3L)
		);
		org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
		verify(menuRepository).findPopularMenus(
				org.mockito.ArgumentMatchers.eq(MenuStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(com.example.coffeeordersystem.domain.OrderStatus.COMPLETED),
				org.mockito.ArgumentMatchers.eq(java.time.LocalDateTime.of(2026, 7, 6, 0, 0)),
				org.mockito.ArgumentMatchers.eq(java.time.LocalDateTime.of(2026, 7, 13, 0, 0)),
				pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
	}

	private Menu menu(Long id, String name, Long price, MenuStatus status) {
		Menu menu = Menu.create(name, price, status);
		ReflectionTestUtils.setField(menu, "id", id);
		return menu;
	}

	private PopularMenuProjection popularMenu(Long menuId, String name, Long orderCount) {
		return new PopularMenuProjection() {
			public Long getMenuId() { return menuId; }
			public String getName() { return name; }
			public Long getOrderCount() { return orderCount; }
		};
	}
}
