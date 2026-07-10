package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

	@Mock
	private MenuRepository menuRepository;

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

	private Menu menu(Long id, String name, Long price, MenuStatus status) {
		Menu menu = Menu.create(name, price, status);
		ReflectionTestUtils.setField(menu, "id", id);
		return menu;
	}
}
