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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** AFTER_COMMIT Listener가 주문 Commit 이후 같은 요청 스레드에서 실행되는지 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Import(OrderAfterCommitEventIntegrationTest.EventObservationConfiguration.class)
@DisplayName("주문 Commit 이후 AFTER_COMMIT Event")
class OrderAfterCommitEventIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(OrderAfterCommitEventIntegrationTest.class);

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;
	@Autowired private EventPublicationRecorder eventPublicationRecorder;
	@Autowired private AfterCommitObservation afterCommitObservation;
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
		user = userRepository.save(User.create("after-commit-event-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("after-commit-event-menu", 3000L, MenuStatus.ACTIVE));
		clientStub = new OrderDataPlatformClientStub(orderDataPlatformClient);
		orderCountBefore = orderRepository.count();
		historyCountBefore = pointHistoryRepository.count();
		eventPublicationRecorder.reset();
		afterCommitObservation.reset();
		listenerThreadName = null;
		doAnswer(invocation -> {
			listenerThreadName = Thread.currentThread().getName();
			log.info("[LISTENER] phase=AFTER_COMMIT thread={}", listenerThreadName);
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
	@DisplayName("정상 AFTER_COMMIT 처리")
	class NormalAfterCommit {

		@Test
		@DisplayName("주문이 Commit되면 이후 같은 스레드의 Listener와 Client가 각 1회 호출된다")
		void 주문이_Commit되면_이후_같은_스레드의_Listener와_Client가_각_1회_호출된다() {
			// given
			clientStub.succeed();
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=정상 AFTER_COMMIT requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order publisherThread={} elapsedMillis={}", eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAfterCommitExecution(requestThreadName);
			verify(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertCommittedOrderState();
			logVerificationResult("정상 AFTER_COMMIT", requestThreadName, elapsedMillis, "성공 응답");
		}
	}

	@Nested
	@DisplayName("지연 AFTER_COMMIT 처리")
	class DelayedAfterCommit {

		@Test
		@DisplayName("Commit 이후 Listener 내부 Client가 2초 지연되면 DB는 먼저 Commit되고 호출 흐름은 최소 1.9초 지연된다")
		void Commit_이후_Listener_내부_Client가_2초_지연되면_DB는_먼저_Commit되고_호출_흐름은_최소_1점9초_지연된다() {
			// given
			Duration delay = Duration.ofSeconds(2);
			clientStub.delayFor(delay);
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=AFTER_COMMIT Listener 내부 Client {}ms 지연 requestThread={}", delay.toMillis(), requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse response = orderService.order(user.getId(), menu.getId());
			long elapsedMillis = elapsedMillisSince(startedAt);
			orderId = response.orderId();
			log.info("[WHEN] action=order publisherThread={} elapsedMillis={}", eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertAfterCommitExecution(requestThreadName);
			assertThat(afterCommitObservation.orderCommittedBeforeListener()).isTrue();
			assertThat(elapsedMillis).isGreaterThanOrEqualTo(1_900L);
			assertCommittedOrderState();
			logVerificationResult("AFTER_COMMIT Listener 내부 Client 2초 지연", requestThreadName, elapsedMillis, "성공 응답");
		}
	}

	@Nested
	@DisplayName("AFTER_COMMIT 외부 Client 예외")
	class FailingClientAfterCommit {

		@Test
		@DisplayName("Commit 이후 Listener Client 예외가 발생해도 호출은 성공하고 주문·포인트·USE 이력은 Commit 상태로 유지된다")
		void Commit_이후_Listener_Client_예외가_발생해도_호출은_성공하고_주문_포인트_USE_이력은_Commit_상태로_유지된다() {
			// given
			clientStub.failWith(new IllegalStateException("data platform failure"));
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=AFTER_COMMIT Listener 내부 Client 예외 requestThread={}", requestThreadName);

			// when
			long startedAt = System.nanoTime();
			OrderResponse[] responseHolder = new OrderResponse[1];
			Throwable thrown = catchThrowable(() -> responseHolder[0] = orderService.order(user.getId(), menu.getId()));
			long elapsedMillis = elapsedMillisSince(startedAt);
			log.info("[WHEN] action=order thrown={} publisherThread={} elapsedMillis={}",
					thrown == null ? "none" : thrown.getClass().getSimpleName(), eventPublicationRecorder.threadName(), elapsedMillis);

			// then
			assertThat(thrown).isNull();
			assertThat(responseHolder[0]).isNotNull();
			orderId = responseHolder[0].orderId();
			assertAfterCommitExecution(requestThreadName);
			verify(orderCompletedEventListener).handle(any(OrderCompletedEvent.class));
			verify(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());
			assertCommittedOrderState();
			logVerificationResult("AFTER_COMMIT Listener 내부 Client 예외", requestThreadName, elapsedMillis, "성공 응답, Listener 예외는 로그 처리");
		}
	}

	@Nested
	@DisplayName("Event 발행 후 바깥 트랜잭션 Rollback")
	class RollbackAfterEventPublication {

		@Test
		@DisplayName("이벤트 발행 후 바깥 트랜잭션이 롤백되면 AFTER_COMMIT 리스너는 실행되지 않는다")
		void 이벤트_발행_후_바깥_트랜잭션이_롤백되면_AFTER_COMMIT_리스너는_실행되지_않는다() {
			// given
			clientStub.succeed();
			String requestThreadName = Thread.currentThread().getName();
			log.info("[GIVEN] condition=Event 발행 이후 바깥 트랜잭션 강제 Rollback requestThread={}", requestThreadName);

			// when
			Throwable thrown = catchThrowable(() -> orderThenFailService.orderThenFail(user.getId(), menu.getId()));
			log.info("[WHEN] action=outerTransactionForcedRollback thrown={}",
					thrown == null ? "none" : thrown.getClass().getSimpleName());

			// then
			assertThat(thrown).isInstanceOf(IllegalStateException.class)
					.hasMessage("forced rollback after event publication");
			assertThat(eventPublicationRecorder.threadName()).isEqualTo(requestThreadName);
			assertThat(listenerThreadName).isNull();
			assertThat(afterCommitObservation.threadName()).isNull();
			verify(orderCompletedEventListener, never()).handle(any(OrderCompletedEvent.class));
			verifyNoInteractions(orderDataPlatformClient);
			assertThat(orderRepository.count()).isEqualTo(orderCountBefore);
			assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(10000L);
			assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore);
			log.info("[EVENT] published=true publisherThread={}", eventPublicationRecorder.threadName());
			log.info("[THEN] condition=Event 발행 후 바깥 트랜잭션 Rollback listenerCallCount={} clientCallCount={} orderCount={} balance={} useHistoryCount={}",
					mockingDetails(orderCompletedEventListener).getInvocations().size(),
					mockingDetails(orderDataPlatformClient).getInvocations().size(), orderRepository.count(),
					pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance(), pointHistoryRepository.count());
		}
	}

	private void assertAfterCommitExecution(String requestThreadName) {
		assertThat(eventPublicationRecorder.threadName()).isEqualTo(requestThreadName);
		assertThat(afterCommitObservation.threadName()).isEqualTo(requestThreadName);
		assertThat(listenerThreadName).isEqualTo(requestThreadName);
		assertThat(clientStub.clientThreadName()).isEqualTo(requestThreadName);
		assertThat(afterCommitObservation.orderCommittedBeforeListener()).isTrue();
		assertThat(afterCommitObservation.transactionActive()).isTrue();
	}

	private void assertCommittedOrderState() {
		assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 1);
		assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(7000L);
		assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 1);
	}

	private long elapsedMillisSince(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000L;
	}

	private void logVerificationResult(String condition, String requestThreadName, long elapsedMillis, String callResult) {
		log.info("[THEN] condition={} callResult={} requestThread={} publisherThread={} afterCommitThread={} listenerThread={} clientThread={} listenerTransactionActive={} orderCommittedBeforeListener={} elapsedMillis={} orderCount={} balance={} useHistoryCount={}",
				condition, callResult, requestThreadName, eventPublicationRecorder.threadName(), afterCommitObservation.threadName(),
				listenerThreadName, clientStub.clientThreadName(), afterCommitObservation.transactionActive(),
				afterCommitObservation.orderCommittedBeforeListener(), elapsedMillis, orderRepository.count(),
				pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance(), pointHistoryRepository.count());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class EventObservationConfiguration {
		@Bean
		EventPublicationRecorder eventPublicationRecorder() {
			return new EventPublicationRecorder();
		}

		@Bean
		AfterCommitObservation afterCommitObservation(JdbcTemplate jdbcTemplate) {
			return new AfterCommitObservation(jdbcTemplate);
		}

		@Bean
		OrderThenFailService orderThenFailService(OrderService orderService) {
			return new OrderThenFailService(orderService);
		}
	}

	static class EventPublicationRecorder {
		private String threadName;

		@EventListener
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public void record(OrderCompletedEvent event) {
			threadName = Thread.currentThread().getName();
			log.info("[EVENT] event=OrderCompletedEvent thread={}", threadName);
		}

		String threadName() { return threadName; }
		void reset() { threadName = null; }
	}

	static class OrderThenFailService {
		private final OrderService orderService;

		OrderThenFailService(OrderService orderService) {
			this.orderService = orderService;
		}

		@Transactional
		public void orderThenFail(Long userId, Long menuId) {
			orderService.order(userId, menuId);
			throw new IllegalStateException("forced rollback after event publication");
		}
	}

	static class AfterCommitObservation {
		private final JdbcTemplate jdbcTemplate;
		private String threadName;
		private Boolean transactionActive;
		private Boolean orderCommittedBeforeListener;

		AfterCommitObservation(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public void observe(OrderCompletedEvent event) {
			threadName = Thread.currentThread().getName();
			transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
			Long orderCount = jdbcTemplate.queryForObject("select count(*) from orders where id = ?", Long.class, event.orderId());
			orderCommittedBeforeListener = orderCount != null && orderCount == 1L;
			log.info("[LISTENER] phase=AFTER_COMMIT observerThread={} transactionActive={} orderCommittedBeforeListener={}",
					threadName, transactionActive, orderCommittedBeforeListener);
		}

		String threadName() { return threadName; }
		Boolean transactionActive() { return transactionActive; }
		Boolean orderCommittedBeforeListener() { return orderCommittedBeforeListener; }
		void reset() {
			threadName = null;
			transactionActive = null;
			orderCommittedBeforeListener = null;
		}
	}
}
