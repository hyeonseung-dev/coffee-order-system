package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.Order;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.event.OrderCompletedEvent;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 실제 Spring 트랜잭션에서 주문 저장 실패 시 결제 변경이 롤백되는지 검증한다.
 *
 * OrderRepository만 저장 실패하도록 대체하고 사용자·메뉴·지갑·이력은 실제 JPA 저장소를
 * 사용해 mock 기반 단위 테스트만으로는 확인할 수 없는 트랜잭션 경계를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(OrderTransactionRollbackIntegrationTest.EventRecorderConfiguration.class)
class OrderTransactionRollbackIntegrationTest {

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private AfterCommitEventRecorder eventRecorder;
	@MockitoBean private OrderRepository orderRepository;

	private User user;
	private PointWallet wallet;
	private Menu menu;
	private long historyCountBefore;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("rollback-test-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("rollback-test-menu", 3000L, MenuStatus.ACTIVE));
		historyCountBefore = pointHistoryRepository.count();
		eventRecorder.reset();
	}

	@AfterEach
	void tearDown() {
		pointWalletRepository.deleteById(wallet.getId());
		menuRepository.deleteById(menu.getId());
		userRepository.deleteById(user.getId());
	}

	@Test
	void 주문_저장_실패시_지갑_USE_이력_주문과_AFTER_COMMIT_처리가_모두_롤백된다() {
		when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("order save failure"));

		assertThatThrownBy(() -> orderService.order(user.getId(), menu.getId()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("order save failure");

		assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
		assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
		assertThat(eventRecorder.count()).isZero();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class EventRecorderConfiguration {
		@Bean
		AfterCommitEventRecorder afterCommitEventRecorder() {
			return new AfterCommitEventRecorder();
		}
	}

	static class AfterCommitEventRecorder {
		private final AtomicInteger eventCount = new AtomicInteger();

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void record(OrderCompletedEvent event) {
			eventCount.incrementAndGet();
		}

		int count() { return eventCount.get(); }
		void reset() { eventCount.set(0); }
	}
}
