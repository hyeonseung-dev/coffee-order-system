package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.Order;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import com.example.coffeeordersystem.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

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
@DisplayName("주문 저장 실패 시 트랜잭션 Rollback")
class OrderTransactionRollbackIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(OrderTransactionRollbackIntegrationTest.class);

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private OutboxEventRepository outboxEventRepository;
	@MockitoBean private OrderRepository orderRepository;

	private User user;
	private PointWallet wallet;
	private Menu menu;
	private long orderCountBefore;
	private long historyCountBefore;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("rollback-test-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("rollback-test-menu", 3000L, MenuStatus.ACTIVE));
		orderCountBefore = orderCount();
		historyCountBefore = pointHistoryRepository.count();
	}

	@AfterEach
	void tearDown() {
		pointWalletRepository.deleteById(wallet.getId());
		menuRepository.deleteById(menu.getId());
		userRepository.deleteById(user.getId());
	}

	@Test
	@DisplayName("주문 저장이 실패하면 주문을 실행한 호출자에게 예외가 전파되고 지갑·USE 이력·주문이 모두 롤백된다")
	void 주문_저장이_실패하면_주문을_실행한_호출자에게_예외가_전파되고_지갑_USE_이력_주문이_모두_롤백된다() {
		// given
		when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("order save failure"));
		String requestThreadName = Thread.currentThread().getName();
		log.info("[GIVEN] condition=주문 저장 예외 requestThread={}", requestThreadName);

		// when
		long startedAt = System.nanoTime();
		assertThatThrownBy(() -> orderService.order(user.getId(), menu.getId()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("order save failure");
		long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
		log.info("[WHEN] action=order throws=IllegalStateException elapsedMillis={} requestThread={}", elapsedMillis, requestThreadName);

		// then
		assertThat(orderCount()).isEqualTo(orderCountBefore);
		assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
		assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
		assertThat(outboxEventRepository.count()).isZero();
		log.info("[THEN] condition=주문 저장 예외 requestThread={} listenerThread=Event 발행 전 실패 clientThread=호출 전 실패 elapsedMillis={} orderCount={} balance={} useHistoryCount={}",
				requestThreadName, elapsedMillis,
				orderCount(), pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance(), pointHistoryRepository.count());
	}

	private long orderCount() {
		Long count = jdbcTemplate.queryForObject("select count(*) from orders", Long.class);
		return count == null ? 0L : count;
	}
}
