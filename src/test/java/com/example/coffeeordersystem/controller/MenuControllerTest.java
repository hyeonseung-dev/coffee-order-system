package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.dto.PopularMenuResponse;
import com.example.coffeeordersystem.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 메뉴 목록 Controller의 HTTP 응답 구조를 검증하는 Web MVC 테스트다.
 *
 * Service는 mock으로 대체해 Controller가 data wrapper와 필요한 필드만 반환하는지 확인한다.
 */
@WebMvcTest(MenuController.class)
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MenuService menuService;

	@Test
	void 메뉴_목록_응답은_data_필드와_menuId_name_price를_포함한다() throws Exception {
		// given
		when(menuService.findActiveMenus()).thenReturn(List.of(
				new MenuResponse(1L, "Americano", 3000L),
				new MenuResponse(2L, "Latte", 4000L)
		));

		// when & then
		mockMvc.perform(get("/api/menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].menuId").value(1))
				.andExpect(jsonPath("$.data[0].name").value("Americano"))
				.andExpect(jsonPath("$.data[0].price").value(3000))
				.andExpect(jsonPath("$.data[0].status").doesNotExist())
				.andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
				.andExpect(jsonPath("$.data[0].updatedAt").doesNotExist())
				.andExpect(jsonPath("$.data[1].menuId").value(2))
				.andExpect(jsonPath("$.data[1].name").value("Latte"))
				.andExpect(jsonPath("$.data[1].price").value(4000));
	}

	@Test
	void 활성_메뉴가_없으면_data에_빈_배열을_반환한다() throws Exception {
		// given
		when(menuService.findActiveMenus()).thenReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void 인기_메뉴_응답은_data에_menuId_name_orderCount를_포함한다() throws Exception {
		when(menuService.findPopularMenus()).thenReturn(List.of(
				new PopularMenuResponse(1L, "Americano", 5L)
		));

		mockMvc.perform(get("/api/menus/popular"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].menuId").value(1))
				.andExpect(jsonPath("$.data[0].name").value("Americano"))
				.andExpect(jsonPath("$.data[0].orderCount").value(5))
				.andExpect(jsonPath("$.data[0].price").doesNotExist());
	}

	@Test
	void 인기_메뉴가_없으면_data에_빈_배열을_반환한다() throws Exception {
		when(menuService.findPopularMenus()).thenReturn(List.of());

		mockMvc.perform(get("/api/menus/popular"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty());
	}
}
