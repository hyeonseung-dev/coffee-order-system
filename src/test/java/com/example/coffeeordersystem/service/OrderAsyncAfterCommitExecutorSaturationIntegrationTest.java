package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.config.OrderFollowUpTaskRejectedHandler;
import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.OrderRepository;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
		"order.follow-up.executor.core-pool-size=1",
		"order.follow-up.executor.max-pool-size=1",
		"order.follow-up.executor.queue-capacity=1"
})
@ActiveProfiles("test")
@DisplayName("주문 후속 Async Executor 포화")
class OrderAsyncAfterCommitExecutorSaturationIntegrationTest {

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private MenuRepository menuRepository;
	@Autowired private PointWalletRepository pointWalletRepository;
	@Autowired private PointHistoryRepository pointHistoryRepository;
	@Autowired private OrderRepository orderRepository;
	@MockitoBean private OrderDataPlatformClient orderDataPlatformClient;
	@MockitoSpyBean private OrderFollowUpTaskRejectedHandler taskRejectedHandler;

	private User user;
	private PointWallet wallet;
	private Menu menu;

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		pointHistoryRepository.deleteAll();
		if (wallet != null) pointWalletRepository.deleteById(wallet.getId());
		if (menu != null) menuRepository.deleteById(menu.getId());
		if (user != null) userRepository.deleteById(user.getId());
	}

	@Test
	@DisplayName("실행 1건과 대기열 1건이 점유된 뒤 세 번째 Event는 거절되고 주문 DB는 모두 Commit된다")
	void 실행_1건과_대기열_1건이_점유된_뒤_세번째_Event는_거절되고_주문_DB는_모두_Commit된다() throws Exception {
		// given
		user = userRepository.save(User.create("async-saturation-user"));
		wallet = pointWalletRepository.save(PointWallet.create(user, 10000L));
		menu = menuRepository.save(Menu.create("async-saturation-menu", 3000L, MenuStatus.ACTIVE));
		long orderCountBefore = orderRepository.count();
		long historyCountBefore = pointHistoryRepository.count();
		CountDownLatch firstClientStarted = new CountDownLatch(1);
		CountDownLatch releaseFirstClient = new CountDownLatch(1);
		CountDownLatch twoAcceptedClientsFinished = new CountDownLatch(2);
		AtomicInteger clientCallCount = new AtomicInteger();
		doAnswer(invocation -> {
			int call = clientCallCount.incrementAndGet();
			if (call == 1) {
				firstClientStarted.countDown();
				assertThat(releaseFirstClient.await(5, TimeUnit.SECONDS)).isTrue();
			}
			twoAcceptedClientsFinished.countDown();
			return null;
		}).when(orderDataPlatformClient).sendOrderCompleted(any(), any(), any(), any(), any(), any());

		// when
		orderService.order(user.getId(), menu.getId());
		assertThat(firstClientStarted.await(5, TimeUnit.SECONDS)).isTrue();
		orderService.order(user.getId(), menu.getId());
		Throwable thirdOrderThrown = catchThrowable(() -> orderService.order(user.getId(), menu.getId()));
		releaseFirstClient.countDown();
		assertThat(twoAcceptedClientsFinished.await(5, TimeUnit.SECONDS)).isTrue();

		// then
		assertThat(thirdOrderThrown).isNull();
		verify(taskRejectedHandler).rejectedExecution(any(), any());
		assertThat(clientCallCount.get()).isEqualTo(2);
		assertThat(orderRepository.count()).isEqualTo(orderCountBefore + 3);
		assertThat(pointWalletRepository.findById(wallet.getId()).orElseThrow().getBalance()).isEqualTo(1000L);
		assertThat(pointHistoryRepository.count()).isEqualTo(historyCountBefore + 3);
	}
}
