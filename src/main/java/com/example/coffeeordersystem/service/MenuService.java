package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 메뉴 조회 비즈니스 흐름을 담당하는 Service다.
 *
 * Repository에서 조회한 Menu Entity를 API 응답 DTO로 변환해
 * Controller가 Entity를 직접 노출하지 않도록 경계를 만든다.
 */
@Service
public class MenuService {

	private final MenuRepository menuRepository;

	public MenuService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	/**
	 * 주문 가능한 ACTIVE 메뉴만 ID 오름차순으로 조회한다.
	 *
	 * 조회 전용 트랜잭션 안에서 데이터를 변경하지 않으며,
	 * 메뉴가 없으면 예외 대신 빈 목록을 반환한다.
	 *
	 * @return 활성 메뉴 목록을 표현하는 응답 DTO 목록
	 */
	@Transactional(readOnly = true)
	public List<MenuResponse> findActiveMenus() {
		return menuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE)
				.stream()
				.map(MenuResponse::from)
				.toList();
	}
}
