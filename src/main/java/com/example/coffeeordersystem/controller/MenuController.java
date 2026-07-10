package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.MenuListResponse;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주문 가능한 커피 메뉴 목록 조회 요청을 처리하는 REST Controller다.
 *
 * HTTP 요청과 응답 구조만 담당하며, 활성 메뉴 조회 규칙은 {@link MenuService}에 위임한다.
 */
@RestController
@RequestMapping("/api/menus")
public class MenuController {

	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	/**
	 * GET /api/menus 요청을 받아 활성 메뉴 목록 조회 흐름을 실행한다.
	 *
	 * @return data 필드에 메뉴 응답 목록을 담은 200 OK 응답
	 */
	@GetMapping
	public ResponseEntity<MenuListResponse> getMenus() {
		List<MenuResponse> menus = menuService.findActiveMenus();
		return ResponseEntity.ok(new MenuListResponse(menus));
	}
}
