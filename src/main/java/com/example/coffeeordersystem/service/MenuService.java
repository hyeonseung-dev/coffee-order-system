package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

	private final MenuRepository menuRepository;

	public MenuService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	@Transactional(readOnly = true)
	public List<MenuResponse> findActiveMenus() {
		return menuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE)
				.stream()
				.map(MenuResponse::from)
				.toList();
	}
}
