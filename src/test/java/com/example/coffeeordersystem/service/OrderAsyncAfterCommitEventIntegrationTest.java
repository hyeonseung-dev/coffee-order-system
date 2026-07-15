package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.event.OrderCompletedEvent;
import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
@Import(OrderAsyncAfterCommitEventIntegrationTest.AsyncObservationConfiguration.class)
@DisplayName("주문 Commit 이후 AFTER_COMMIT + Async Event")
class OrderAsyncAfterCommitEventIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(OrderAsyncAfterCommitEventIntegrationTest.class);
	private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private EventPublicationRecorder eventPublicationRecorder;
	@Autowired private OrderThenFailService orderThenFailService;
	@MockitoBean private OrderDataPlatformClient orderDataPlatformClient;

	private User user;
	private PointWallet wallet;
	private Menu menu;
	private long orderCountBefore;
	private long historyCountBefore;
	private Long orderId;
	private CountDownLatch listenerCalled;
	private CountDownLatch clientCalled;
	private volatile String listenerThreadName;
	private volatile String clientThreadName;
	private volatile Throwable clientException;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("async-after-commit-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("async-after-commit-menu", 3000L, MenuStatus.ACTIVE));
		orderCountBefore = orderRepository.count();
		historyCountBefore = pointHistoryRepository.count();
		eventPublicationRecorder.reset();
		listenerCalled = new CountDownLatch(1);
		clientCalled = new CountDownLatch(1);
		listenerThreadName = null;
		clientThreadName = null;
		clientException = null;
	}

	@AfterEach
	void tearDown() {
		if (orderId != null) orderRepository.deleteById(orderId);
		pointHistoryRepository.deleteAll();
		pointWalletRepository.deleteById(wallet.getId());
		menuRepository.deleteById(menu.getId());
		userRepository.deleteById(user.getId());
	}

	@Nested
	@DisplayName("정상 비동기 처리")
	class NormalAsync {
		@Test
		@DisplayName("Commit 후 Listener와 Client는 같은 전용 스레드에서 1회 실행되고 요청 스레드와 분리된다")
		void Commit_후_Listener와_Client는_같은_전용_스레드에서_1회_실행되고_요청_스레드와_분리된다() throws Exception {
			// given
			String requestThread = Thread.currentThread().getName();
			configureClient(() -> log.info("[CLIENT] condition=정상 응답 thread={}", Thread.currentThread().getName()));
			log.info("[GIVEN] condition=정상 AFTER_COMMIT Async requestThread={}", requestThread);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long orderReturnMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			await(clientCalled, "Client 완료");
			log.info("[WHEN] action=order orderReturnMillis={} publisherThread={}", orderReturnMillis, eventPublicationRecorder.threadName());

			// then
			assertAsyncThreads(requestThread);
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertCommittedOrderState();
			logResult("정상 AFTER_COMMIT Async", requestThread, orderReturnMillis, null);
		}
	}

	@Nested
	@DisplayName("지연 비동기 처리")
	class DelayedAsync {
		@Test
		@DisplayName("Client가 2초 지연돼도 order는 완료를 기다리지 않고 반환하며 주문은 먼저 Commit된다")
		void Client가_2초_지연돼도_order는_완료를_기다리지_않고_반환하며_주문은_먼저_Commit된다() throws Exception {
			// given
			CountDownLatch clientStarted = new CountDownLatch(1);
			CountDownLatch clientFinished = new CountDownLatch(1);
			long[] clientStartedAt = new long[1];
			long[] clientFinishedAt = new long[1];
			String requestThread = Thread.currentThread().getName();
			configureClient(() -> {
				clientStartedAt[0] = System.nanoTime();
				clientStarted.countDown();
				Thread.sleep(2_000L);
				clientFinishedAt[0] = System.nanoTime();
				clientFinished.countDown();
				log.info("[CLIENT] condition=2000ms 지연 완료 thread={}", Thread.currentThread().getName());
			});
			log.info("[GIVEN] condition=AFTER_COMMIT Async Client 2000ms 지연 requestThread={}", requestThread);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long orderReturnMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			await(clientStarted, "Client 시작");
			assertCommittedOrderState();
			await(clientFinished, "Client 완료");
			long clientDurationMillis = (clientFinishedAt[0] - clientStartedAt[0]) / 1_000_000L;
			log.info("[WHEN] action=order orderReturnMillis={} clientDurationMillis={}", orderReturnMillis, clientDurationMillis);

			// then
			assertAsyncThreads(requestThread);
			assertThat(orderReturnMillis).isLessThan(1_000L);
			assertThat(clientDurationMillis).isGreaterThanOrEqualTo(1_900L);
			logResult("AFTER_COMMIT Async Client 2초 지연", requestThread, orderReturnMillis, clientDurationMillis);
		}
	}

	@Nested
	@DisplayName("비동기 Client 예외")
	class FailingAsync {
		@Test
		@DisplayName("비동기 Client 예외는 호출자에게 전파되지 않고 예외 처리기에서 관찰되며 주문은 Commit된다")
		void 비동기_Client_예외는_호출자에게_전파되지_않고_예외_처리기에서_관찰되며_주문은_Commit된다() throws Exception {
			// given
			IllegalStateException failure = new IllegalStateException("data platform failure");
			String requestThread = Thread.currentThread().getName();
			configureClient(() -> { throw failure; });
			log.info("[GIVEN] condition=AFTER_COMMIT Async Client 예외 requestThread={}", requestThread);

			// when
			Throwable thrown = catchThrowable(() -> orderId = orderService.order(user.getId(), menu.getId()).orderId());
			await(clientCalled, "비동기 Client 예외 발생");
			log.info("[WHEN] action=order thrown={} asyncException={}", thrown == null ? "none" : thrown.getClass().getSimpleName(), clientException.getClass().getSimpleName());

			// then
			assertThat(thrown).isNull();
			assertThat(clientException).isSameAs(failure);
			assertAsyncThreads(requestThread);
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertCommittedOrderState();
			logResult("AFTER_COMMIT Async Client 예외", requestThread, 0L, null);
		}
	}

	@Nested
	@DisplayName("Event 발행 후 바깥 트랜잭션 Rollback")
	class RollbackAfterEventPublication {
		@Test
		@DisplayName("이벤트 발행 후 바깥 트랜잭션이 롤백되면 Async AFTER_COMMIT Listener는 실행되지 않는다")
		void 이벤트_발행_후_바깥_트랜잭션이_롤백되면_Async_AFTER_COMMIT_리스너는_실행되지_않는다() {
			// given
			String requestThread = Thread.currentThread().getName();
			log.info("[GIVEN] condition=Event 발행 이후 바깥 트랜잭션 강제 Rollback requestThread={}", requestThread);

			// when
			Throwable thrown = catchThrowable(() -> orderThenFailService.orderThenFail(user.getId(), menu.getId()));
			log.info("[WHEN] action=outerTransactionForcedRollback thrown={}", thrown == null ? "none" : thrown.getClass().getSimpleName());

			// then
			assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("forced rollback after event publication");
			assertThat(eventPublicationRecorder.threadName()).isEqualTo(requestThread);
			assertThat(listenerThreadName).isNull();
			verifyNoInteractions(orderDataPlatformClient);
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
			log.info("[EVENT] published=true publisherThread={}", eventPublicationRecorder.threadName());
			log.info("[THEN] listenerCallCount=0 clientCallCount=0 orderCount={} balance={} useHistoryCount={}", orderRepository.count(), 10000L, pointHistoryRepository.count());
		}
	}

	private void configureClient(ThrowingRunnable behavior) {
		doAnswer(invocation -> {
			clientThreadName = Thread.currentThread().getName();
			listenerThreadName = clientThreadName;
			listenerCalled.countDown();
			log.info("[LISTENER] phase=AFTER_COMMIT asyncThread={}", listenerThreadName);
			log.info("[CLIENT] thread={}", clientThreadName);
			try {
				behavior.run();
			} catch (Throwable exception) {
				clientException = exception;
				throw exception;
			} finally {
				clientCalled.countDown();
			}
			return null;
		}).when(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
	}

	private void assertAsyncThreads(String requestThread) throws InterruptedException {
		await(listenerCalled, "Listener 실행");
		assertThat(eventPublicationRecorder.threadName()).isEqualTo(requestThread);
		assertThat(listenerThreadName).startsWith("order-follow-up-").isNotEqualTo(requestThread);
		assertThat(clientThreadName).isEqualTo(listenerThreadName);
	}

	private void assertCommittedOrderState() {
		assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
		assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
		assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
	}

	private void await(CountDownLatch latch, String condition) throws InterruptedException {
		assertThat(latch.await(ASYNC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).as(condition).isTrue();
	}

	private long elapsedMillisSince(long startedAt) { return (System.nanoTime() - startedAt) / 1_000_000L; }

	private void logResult(String condition, String requestThread, long orderReturnMillis, Long clientDurationMillis) {
		log.info("[THEN] condition={} requestThread={} publisherThread={} listenerThread={} clientThread={} orderReturnMillis={} clientDurationMillis={} orderCount={} balance={} useHistoryCount={}",
				condition, requestThread, eventPublicationRecorder.threadName(), listenerThreadName, clientThreadName, orderReturnMillis,
				clientDurationMillis, orderRepository.count(), pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance(), pointHistoryRepository.count());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AsyncObservationConfiguration {
		@Bean EventPublicationRecorder eventPublicationRecorder() { return new EventPublicationRecorder(); }
		@Bean OrderThenFailService orderThenFailService(OrderService orderService) { return new OrderThenFailService(orderService); }
	}

	static class EventPublicationRecorder {
		private volatile String threadName;
		@EventListener @Order(Ordered.HIGHEST_PRECEDENCE)
		public void record(OrderCompletedEvent event) {
			threadName = Thread.currentThread().getName();
			log.info("[EVENT] event=OrderCompletedEvent thread={}", threadName);
		}
		String threadName() { return threadName; }
		void reset() { threadName = null; }
	}

	static class OrderThenFailService {
		private final OrderService orderService;
		OrderThenFailService(OrderService orderService) { this.orderService = orderService; }
		@Transactional
		public void orderThenFail(Long userId, Long menuId) {
			orderService.order(userId, menuId);
			throw new IllegalStateException("forced rollback after event publication");
		}
	}

	@FunctionalInterface
	interface ThrowingRunnable { void run() throws Exception; }
}
