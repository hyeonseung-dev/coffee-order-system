package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.Order;
import com.example.coffeeordersystem.domain.PointHistory;
import com.example.coffeeordersystem.domain.PointHistoryType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 주문 Service의 정상 처리 순서와 비즈니스 실패 조건을 mock 기반으로 검증한다. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private MenuRepository menuRepository;
	@Mock private PointWalletRepository pointWalletRepository;
	@Mock private PointHistoryRepository pointHistoryRepository;
	@Mock private OrderRepository orderRepository;
	@Mock private ApplicationEventPublisher eventPublisher;
	@Mock private Clock clock;
	@InjectMocks private OrderService orderService;

	@Test
	void 정상_주문은_포인트를_차감하고_USE_이력과_완료_주문_이벤트를_생성한다() {
		User user = user(1L);
		Menu menu = menu(2L, MenuStatus.ACTIVE, 3000L);
		PointWallet wallet = PointWallet.create(user, 10000L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(menuRepository.findById(2L)).thenReturn(Optional.of(menu));
		when(pointWalletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
		Instant orderedAt = Instant.parse("2026-07-12T03:00:00Z");
		fixedClock(orderedAt);
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			ReflectionTestUtils.setField(order, "id", 3L);
			return order;
		});

		OrderResponse response = orderService.order(1L, 2L);

		ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		ArgumentCaptor<OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
		verify(pointHistoryRepository).save(historyCaptor.capture());
		verify(orderRepository).save(orderCaptor.capture());
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(wallet.getBalance()).isEqualTo(7000L);
		assertThat(historyCaptor.getValue().getType()).isEqualTo(PointHistoryType.USE);
		assertThat(historyCaptor.getValue().getAmount()).isEqualTo(3000L);
		assertThat(historyCaptor.getValue().getBalanceAfter()).isEqualTo(7000L);
		assertThat(eventCaptor.getValue()).isEqualTo(
				new OrderCompletedEvent(3L, 1L, 2L, 3000L, orderedAt, "Asia/Seoul"));
		assertThat(eventCaptor.getValue().orderedAt()).isEqualTo(orderCaptor.getValue().getOrderedAt());
		assertThat(response.remainingBalance()).isEqualTo(7000L);
		assertThat(response.orderedAt()).isEqualTo(LocalDateTime.of(2026, 7, 12, 12, 0));
	}

	@Test
	void 비활성_메뉴는_포인트와_주문을_변경하지_않는다() {
		User user = user(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(menuRepository.findById(2L)).thenReturn(Optional.of(menu(2L, MenuStatus.INACTIVE, 3000L)));

		assertThatThrownBy(() -> orderService.order(1L, 2L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INACTIVE_MENU));
		verify(pointWalletRepository, never()).findByUser(any());
		verify(pointHistoryRepository, never()).save(any());
		verify(orderRepository, never()).save(any());
	}

	@Test
	void 포인트가_부족하면_USE_이력과_주문을_저장하지_않는다() {
		User user = user(1L);
		Menu menu = menu(2L, MenuStatus.ACTIVE, 3000L);
		PointWallet wallet = PointWallet.create(user, 2999L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(menuRepository.findById(2L)).thenReturn(Optional.of(menu));
		when(pointWalletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> orderService.order(1L, 2L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT));
		assertThat(wallet.getBalance()).isEqualTo(2999L);
		verify(pointHistoryRepository, never()).save(any());
		verify(orderRepository, never()).save(any());
	}

	private User user(Long id) {
		User user = User.create("주문사용자");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Menu menu(Long id, MenuStatus status, Long price) {
		Menu menu = Menu.create("아메리카노", price, status);
		ReflectionTestUtils.setField(menu, "id", id);
		return menu;
	}

	private void fixedClock(Instant instant) {
		when(clock.instant()).thenReturn(instant);
	}
}
