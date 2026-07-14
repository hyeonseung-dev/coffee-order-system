package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.event.OrderCompletedEvent;
import com.example.coffeeordersystem.event.OrderCompletedEventListener;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

/** 일반 동기 Event가 주문 트랜잭션과 요청 스레드에 미치는 영향을 실제 JPA 트랜잭션으로 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(OrderSynchronousEventIntegrationTest.EventObservationConfiguration.class)
@DisplayName("주문 트랜잭션 내부 일반 동기 Event")
class OrderSynchronousEventIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(OrderSynchronousEventIntegrationTest.class);

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private EventPublicationRecorder eventPublicationRecorder;
	@Autowired private OrderThenFailService orderThenFailService;
	@MockitoBean private OrderDataPlatformClient orderDataPlatformClient;
	@MockitoSpyBean private OrderCompletedEventListener orderCompletedEventListener;

	private User user;
	private PointWallet wallet;
	private Menu menu;
	private OrderDataPlatformClientStub clientStub;
	private long orderCountBefore;
	private long historyCountBefore;
	private Long orderId;
	private String listenerThreadName;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("sync-event-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("sync-event-menu", 3000L, MenuStatus.ACTIVE));
		clientStub = new OrderDataPlatformClientStub(orderDataPlatformClient);
		orderCountBefore = orderRepository.count();
		historyCountBefore = pointHistoryRepository.count();
		eventPublicationRecorder.reset();
		listenerThreadName = null;
		doAnswer(invocation -> {
			listenerThreadName = Thread.currentThread().getName();
			log.info("[LISTENER] event=OrderCompletedEvent thread={}", listenerThreadName);
			return invocation.callRealMethod();
		}).when(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
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
	@DisplayName("정상 Event 처리")
	class NormalEvent {

		@Test
		@DisplayName("정상 Event를 발행하면 같은 스레드의 Listener와 Client가 각 1회 호출되고 주문·포인트·USE 이력이 커밋된다")
		void 정상_Event를_발행하면_같은_스레드의_Listener와_Client가_각_1회_호출되고_주문_포인트_USE_이력이_커밋된다() {
			// given
			clientStub.succeed();
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=정상 Event requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order eventPublisherThread={} elapsedMillis={}", eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAllThreadsAre(requestThreadName);
			verify(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
			logVerificationResult("정상 Event", requestThreadName, elapsedMillis);
		}
	}

	@Nested
	@DisplayName("지연 Event 처리")
	class DelayedEvent {

		@Test
		@DisplayName("Listener 내부 Client가 2초 지연되면 주문 처리도 최소 1.9초 지연되고 데이터는 커밋된다")
		void Listener_내부_Client가_2초_지연되면_주문_처리도_최소_1점9초_지연되고_데이터는_커밋된다() {
			// given
			Duration delay = Duration.ofSeconds(2);
			clientStub.delayFor(delay);
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=Listener 내부 Client {}ms 지연 requestThread={}", delay.toMillis(), requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order eventPublisherThread={} elapsedMillis={}", eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAllThreadsAre(requestThreadName);
			assertThat(elapsedMillis).isGreaterThanOrEqualTo(1_900L);
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
			logVerificationResult("Listener 내부 Client 2초 지연", requestThreadName, elapsedMillis);
		}
	}

	@Nested
	@DisplayName("외부 Client 예외")
	class FailingClient {

		@Test
		@DisplayName("Listener가 호출한 Client가 예외를 던지면 호출자에게 전파되고 주문·포인트·USE 이력이 롤백된다")
		void Listener가_호출한_Client가_예외를_던지면_호출자에게_전파되고_주문_포인트_USE_이력이_롤백된다() {
			// given
			IllegalStateException exception = new IllegalStateException("data platform failure");
			clientStub.failWith(exception);
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=Listener 내부 Client 예외 requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			assertThatThrownBy(() -> orderService.order(user.getId(), menu.getId()))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("data platform failure");
			long elapsedMillis = elapsedMillisSince(startedAt);
			log.info("[WHEN] action=order throws=IllegalStateException eventPublisherThread={} elapsedMillis={}",
					eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAllThreadsAre(requestThreadName);
			verify(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
			logVerificationResult("Listener 내부 Client 예외", requestThreadName, elapsedMillis);
		}
	}

	@Nested
	@DisplayName("Event 전달 후 강제 Rollback")
	class RollbackAfterEventDelivery {

		@Test
		@DisplayName("동기 Listener와 Client가 성공한 뒤 강제 예외가 발생하면 외부 전송만 남고 주문·포인트·USE 이력은 롤백된다")
		void 동기_Listener와_Client가_성공한_뒤_강제_예외가_발생하면_외부_전송만_남고_주문_포인트_USE_이력은_롤백된다() {
			// given
			clientStub.succeed();
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=Event 발행 이후 강제 Rollback requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			assertThatThrownBy(() -> orderThenFailService.orderThenFail(user.getId(), menu.getId()))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("forced rollback after synchronous event delivery");
			long elapsedMillis = elapsedMillisSince(startedAt);
			log.info("[WHEN] action=orderThenFail throws=IllegalStateException eventPublisherThread={} elapsedMillis={}",
					eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAllThreadsAre(requestThreadName);
			verify(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
			logVerificationResult("Event 전달 후 강제 Rollback", requestThreadName, elapsedMillis);
		}
	}

	private void assertAllThreadsAre(String requestThreadName) {
		assertThat(eventPublicationRecorder.threadName()).isEqualTo(requestThreadName);
		assertThat(listenerThreadName).isEqualTo(requestThreadName);
		assertThat(clientStub.clientThreadName()).isEqualTo(requestThreadName);
	}

	private long elapsedMillisSince(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000L;
	}

	private void logVerificationResult(String condition, String requestThreadName, long elapsedMillis) {
		long orderCount = orderRepository.count();
		long useHistoryCount = pointHistoryRepository.count();
		long balance = pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance();
		long listenerCallCount = mockingDetails(orderCompletedEventListener).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("handle"))
				.count();
		long clientCallCount = mockingDetails(orderDataPlatformClient).getInvocations().stream()
				.filter(invocation -> invocation.getMethod().getName().equals("sendOrderCompleted"))
				.count();
		log.info("[THEN] condition={} requestThread={} eventPublisherThread={} listenerThread={} clientThread={} elapsedMillis={} orderCount={} balance={} useHistoryCount={} listenerCallCount={} clientCallCount={}",
				condition, requestThreadName, eventPublicationRecorder.threadName(), listenerThreadName,
				clientStub.clientThreadName(), elapsedMillis, orderCount, balance, useHistoryCount, listenerCallCount, clientCallCount);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class EventObservationConfiguration {
		@Bean
		EventPublicationRecorder eventPublicationRecorder() {
			return new EventPublicationRecorder();
		}

		@Bean
		OrderThenFailService orderThenFailService(OrderService orderService) {
			return new OrderThenFailService(orderService);
		}
	}

	static class OrderThenFailService {
		private final OrderService orderService;

		OrderThenFailService(OrderService orderService) {
			this.orderService = orderService;
		}

		@Transactional
		public void orderThenFail(Long userId, Long menuId) {
			orderService.order(userId, menuId);
			throw new IllegalStateException("forced rollback after synchronous event delivery");
		}
	}

	static class EventPublicationRecorder {
		private String threadName;

		@EventListener
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public void record(OrderCompletedEvent event) {
			threadName = Thread.currentThread().getName();
			log.info("[LISTENER] event=OrderCompletedEvent publicationObserverThread={}", threadName);
		}

		String threadName() { return threadName; }
		void reset() { threadName = null; }
	}
}
