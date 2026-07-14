package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.OrderStatus;
import com.example.coffeeordersystem.dto.MenuResponse;
import com.example.coffeeordersystem.dto.PopularMenuResponse;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private final Clock clock;

	public MenuService(MenuRepository menuRepository, Clock clock) {
		this.menuRepository = menuRepository;
		this.clock = clock;
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

	/**
	 * 오늘을 제외한 직전 7개 완료 일자 동안의 ACTIVE 메뉴 인기 순위를 최대 3건 조회한다.
	 *
	 * 기간과 주문 상태 조건을 LEFT JOIN의 ON 절에 둬 주문이 없는 활성 메뉴도 0건 후보로 남긴다.
	 */
	@Transactional(readOnly = true)
	public List<PopularMenuResponse> findPopularMenus() {
		LocalDate today = LocalDate.now(clock);
		LocalDateTime fromInclusive = today.minusDays(7).atStartOfDay();
		LocalDateTime toExclusive = today.atStartOfDay();

		return menuRepository.findPopularMenus(
				MenuStatus.ACTIVE,
				OrderStatus.COMPLETED,
				fromInclusive,
				toExclusive,
				PageRequest.of(0, 3)
		).stream().map(PopularMenuResponse::from).toList();
	}
}
