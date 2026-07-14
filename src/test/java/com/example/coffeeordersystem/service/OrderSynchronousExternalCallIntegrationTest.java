package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import com.example.coffeeordersystem.service.support.OrderDataPlatformClientStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/** 직접 동기 외부 호출이 주문 트랜잭션과 요청 스레드에 미치는 영향을 실제 JPA 트랜잭션으로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("주문 트랜잭션 내부 직접 동기 외부 호출")
class OrderSynchronousExternalCallIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(OrderSynchronousExternalCallIntegrationTest.class);

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;
	@MockitoBean private OrderDataPlatformClient orderDataPlatformClient;

	private User user;
	private PointWallet wallet;
	private Menu menu;
	private OrderDataPlatformClientStub clientStub;
	private long orderCountBefore;
	private long historyCountBefore;
	private Long orderId;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("sync-external-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("sync-external-menu", 3000L, MenuStatus.ACTIVE));
		clientStub = new OrderDataPlatformClientStub(orderDataPlatformClient);
		orderCountBefore = orderRepository.count();
		historyCountBefore = pointHistoryRepository.count();
	}

	@AfterEach
	void tearDown() {
		if (orderId != null) {
			orderRepository.deleteById(orderId);
		}
		pointHistoryRepository.deleteAll();
		pointWalletRepository.deleteById(wallet.getId());
		menuRepository.deleteById(menu.getId());
		userRepository.deleteById(user.getId());
	}

	@Nested
	@DisplayName("정상 호출")
	class NormalCall {

		@Test
		@DisplayName("정상 외부 호출 시 주문을 실행하면 같은 요청 스레드에서 호출되고 주문·포인트·USE 이력이 커밋된다")
		void 정상_외부_호출시_주문을_실행하면_같은_요청_스레드에서_호출되고_주문_포인트_USE_이력이_커밋된다() {
			// given
			clientStub.succeed();
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=정상 외부 호출 requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order elapsedMillis={} requestThread={}", elapsedMillis, requestThreadName);

			// then
			assertThat(clientStub.clientThreadName()).isEqualTo(requestThreadName);
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
			logVerificationResult("정상 외부 호출", requestThreadName, elapsedMillis);
		}
	}

	@Nested
	@DisplayName("지연 호출")
	class DelayedCall {

		@Test
		@DisplayName("외부 호출이 2초 지연되면 주문을 실행한 요청과 트랜잭션도 최소 1.9초 지연되고 데이터는 커밋된다")
		void 외부_호출이_2초_지연되면_주문을_실행한_요청과_트랜잭션도_최소_1점9초_지연되고_데이터는_커밋된다() {
			// given
			Duration delay = Duration.ofSeconds(2);
			clientStub.delayFor(delay);
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=외부 호출 {}ms 지연 requestThread={}", delay.toMillis(), requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order elapsedMillis={} requestThread={}", elapsedMillis, requestThreadName);

			// then
			assertThat(elapsedMillis).isGreaterThanOrEqualTo(1_900L);
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
			logVerificationResult("외부 호출 2초 지연", requestThreadName, elapsedMillis);
		}
	}

	@Nested
	@DisplayName("예외 호출")
	class FailingCall {

		@Test
		@DisplayName("외부 호출이 예외를 던지면 주문을 실행한 호출자에게 예외가 전파되고 주문·포인트·USE 이력이 롤백된다")
		void 외부_호출이_예외를_던지면_주문을_실행한_호출자에게_예외가_전파되고_주문_포인트_USE_이력이_롤백된다() {
			// given
			IllegalStateException exception = new IllegalStateException("data platform failure");
			clientStub.failWith(exception);
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=외부 호출 예외 requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			assertThatThrownBy(() -> orderService.order(user.getId(), menu.getId()))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("data platform failure");
			long elapsedMillis = elapsedMillisSince(startedAt);
			log.info("[WHEN] action=order throws=IllegalStateException elapsedMillis={} requestThread={}", elapsedMillis, requestThreadName);

			// then
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
			logVerificationResult("외부 호출 예외", requestThreadName, elapsedMillis);
		}
	}

	private long elapsedMillisSince(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000L;
	}

	private void logVerificationResult(String condition, String requestThreadName, long elapsedMillis) {
		long orderCount = orderRepository.count();
		long useHistoryCount = pointHistoryRepository.count();
		long balance = pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance();
		log.info("[THEN] condition={} requestThread={} clientThread={} elapsedMillis={} orderCount={} balance={} useHistoryCount={}",
				condition, requestThreadName, clientStub.clientThreadName(), elapsedMillis, orderCount, balance, useHistoryCount);
	}
}
