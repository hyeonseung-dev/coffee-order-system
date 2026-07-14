package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.Order;
import com.example.coffeeordersystem.domain.PointHistory;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.event.OrderCompletedEvent;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 메뉴 주문과 포인트 결제를 하나의 트랜잭션으로 처리하는 Service다.
 *
 * 사용자·메뉴·지갑 검증 후 포인트 차감, USE 이력, 성공 주문 저장과 완료 이벤트 발행을
 * 같은 트랜잭션에서 실행해 중간 저장 실패 시 모든 데이터 변경을 롤백한다.
 */
@Service
public class OrderService {
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;
	private final MenuRepository menuRepository;
	private final PointWalletRepository pointWalletRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final OrderRepository orderRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	public OrderService(UserRepository userRepository, MenuRepository menuRepository,
			PointWalletRepository pointWalletRepository, PointHistoryRepository pointHistoryRepository,
			OrderRepository orderRepository, ApplicationEventPublisher eventPublisher, Clock clock) {
		this.userRepository = userRepository;
		this.menuRepository = menuRepository;
		this.pointWalletRepository = pointWalletRepository;
		this.pointHistoryRepository = pointHistoryRepository;
		this.orderRepository = orderRepository;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	/**
	 * 사용자의 포인트로 활성 메뉴 한 개를 결제한다.
	 *
	 * 사용자 조회부터 완료 이벤트 발행 요청까지 단일 트랜잭션으로 처리한다. 이벤트의
	 * 실제 후속 처리는 AFTER_COMMIT listener가 맡아 롤백된 주문에는 실행되지 않는다.
	 *
	 * @param userId 주문 사용자 ID
	 * @param menuId 주문 메뉴 ID
	 * @return 주문 식별값, 결제 금액, 차감 후 잔액
	 * @throws BusinessException 사용자·메뉴·지갑이 없거나 메뉴가 비활성 또는 잔액이 부족한 경우
	 */
	@Transactional
	public OrderResponse order(Long userId, Long menuId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Menu menu = menuRepository.findById(menuId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
		if (menu.getStatus() != MenuStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.INACTIVE_MENU);
		}

		PointWallet wallet = pointWalletRepository.findByUser(user)
				.orElseThrow(() -> new BusinessException(ErrorCode.POINT_WALLET_NOT_FOUND));
		wallet.debit(menu.getPrice());
		pointHistoryRepository.save(PointHistory.use(user, menu.getPrice(), wallet.getBalance()));

		Instant orderedAt = Instant.now(clock);
		Order savedOrder = orderRepository.save(Order.completed(user, menu, menu.getPrice(), orderedAt));
		eventPublisher.publishEvent(new OrderCompletedEvent(
				savedOrder.getId(), user.getId(), menu.getId(), savedOrder.getOrderPrice(),
				savedOrder.getOrderedAt(), BUSINESS_ZONE.getId()));

		return new OrderResponse(savedOrder.getId(), user.getId(), menu.getId(), savedOrder.getOrderPrice(),
				wallet.getBalance(), LocalDateTime.ofInstant(savedOrder.getOrderedAt(), BUSINESS_ZONE));
	}
}
