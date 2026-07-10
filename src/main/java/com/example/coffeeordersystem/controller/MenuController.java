package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.MenuListResponse;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public ResponseEntity<MenuListResponse> getMenus() {
		List<MenuResponse> menus = menuService.findActiveMenus();
		return ResponseEntity.ok(new MenuListResponse(menus));
	}
}
