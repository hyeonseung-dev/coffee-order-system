package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.domain.PointHistoryType;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.repository.MenuRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL에서 지갑 행 잠금이 동일 사용자 주문을 직렬화하는지 검증한다.
 *
 * 기본 test task에는 포함하지 않는다. #10과 동일한 시작 동기화 조건으로 성공·실패·잔액·이력을
 * 강제 검증한다.
 */
@Tag("concurrency")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("concurrency")
class ConcurrentOrderMySqlIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentOrderMySqlIntegrationTest.class);
    private static final int CONCURRENT_REQUESTS = 10;
    private static final int SERVICE_ATTEMPTS = 10;
    private static final long INITIAL_BALANCE = 10_000L;
    private static final long MENU_PRICE = 3_000L;
    private static final long TIMEOUT_SECONDS = 30L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Autowired private OrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private PointWalletRepository pointWalletRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @LocalServerPort private int port;

    private final List<Fixture> fixtures = new ArrayList<>();

    @AfterEach
    void tearDown() {
        fixtures.forEach(this::deleteFixture);
        fixtures.clear();
    }

    @Test
    void 동일_사용자_동시_주문_Service는_10회_모두_정합성을_보장한다() throws Exception {
        List<Observation> observations = new ArrayList<>();

        for (int attempt = 1; attempt <= SERVICE_ATTEMPTS; attempt++) {
            Fixture fixture = createFixture("service-" + attempt);
            Observation observation = runConcurrently(
                    () -> {
                        try {
                            orderService.order(fixture.userId(), fixture.menuId());
                            return Outcome.successful();
                        } catch (BusinessException exception) {
                            return Outcome.failure(exception.getErrorCode().name());
                        }
                    },
                    fixture
            );
            observations.add(observation);
            logObservation("service", attempt, observation);
            assertExpectedResult(observation);
        }

        assertThat(observations).hasSize(SERVICE_ATTEMPTS);
    }

    @Test
    void 동일_사용자_동시_주문_API_보조_시나리오도_정합성을_보장한다() throws Exception {
        Fixture fixture = createFixture("api");

        Observation observation = runConcurrently(
                () -> {
                    HttpResponse<String> response = postOrder(fixture);
                    return response.statusCode() >= 200 && response.statusCode() < 300
                            ? Outcome.successful()
                            : Outcome.apiFailure(response.statusCode(), response.body().contains("\"code\":\"INSUFFICIENT_POINT\"")
                                    ? ErrorCode.INSUFFICIENT_POINT.name()
                                    : "HTTP_" + response.statusCode());
                },
                fixture
        );

        logObservation("api", 1, observation);
        assertExpectedResult(observation);
        assertThat(observation.failureStatusCodes()).containsOnly(Map.entry(400, 7L));
    }

    private Fixture createFixture(String scenario) {
        String suffix = UUID.randomUUID().toString();
        User user = userRepository.save(User.create("concurrency-" + scenario + "-" + suffix));
        PointWallet wallet = pointWalletRepository.save(PointWallet.create(user, INITIAL_BALANCE));
        Menu menu = menuRepository.save(Menu.create("concurrency-menu-" + suffix, MENU_PRICE, MenuStatus.ACTIVE));
        Fixture fixture = new Fixture(user.getId(), wallet.getId(), menu.getId());
        fixtures.add(fixture);
        return fixture;
    }

    private HttpResponse<String> postOrder(Fixture fixture) throws Exception {
        String requestBody = "{\"userId\":" + fixture.userId() + ",\"menuId\":" + fixture.menuId() + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Observation runConcurrently(Callable<Outcome> request, Fixture fixture) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_REQUESTS; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        return Outcome.failure("START_TIMEOUT");
                    }
                    try {
                        return request.call();
                    } catch (Exception exception) {
                        return Outcome.failure(exception.getClass().getSimpleName());
                    }
                }));
            }

            assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return observe(outcomes, fixture);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Observation observe(List<Outcome> outcomes, Fixture fixture) {
        long successCount = outcomes.stream().filter(Outcome::success).count();
        long failureCount = outcomes.size() - successCount;
        long orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Long.class, fixture.userId());
        long useHistoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM point_history WHERE user_id = ? AND type = ?",
                Long.class, fixture.userId(), PointHistoryType.USE.name());
        long balance = pointWalletRepository.findById(fixture.walletId()).orElseThrow().getBalance();
        List<String> violations = new ArrayList<>();
        if (successCount != orderCount || successCount != useHistoryCount) {
            violations.add("REQUEST_ORDER_HISTORY_MISMATCH");
        }
        if (balance != INITIAL_BALANCE - (MENU_PRICE * successCount)) {
            violations.add("LOST_UPDATE_OR_BALANCE_MISMATCH");
        }
        if (successCount > INITIAL_BALANCE / MENU_PRICE) {
            violations.add("EXCESS_SUCCESS_ORDER");
        }
        Map<String, Long> failureTypes = outcomes.stream()
                .filter(outcome -> !outcome.success())
                .collect(Collectors.groupingBy(Outcome::reason, Collectors.counting()));
        Map<Integer, Long> failureStatusCodes = outcomes.stream()
                .filter(outcome -> !outcome.success() && outcome.statusCode() != null)
                .collect(Collectors.groupingBy(Outcome::statusCode, Collectors.counting()));
        return new Observation(successCount, failureCount, orderCount, useHistoryCount, balance, failureTypes,
                failureStatusCodes, violations);
    }

    private void logObservation(String testType, int attempt, Observation observation) {
        log.info("[CONCURRENCY_OBSERVATION] type={} attempt={} success={} failure={} failureTypes={} orders={} useHistories={} balance={} consistencyViolation={}",
                testType, attempt, observation.successCount(), observation.failureCount(), observation.failureTypes(),
                observation.orderCount(), observation.useHistoryCount(), observation.balance(), observation.violations());
    }

    private void assertExpectedResult(Observation observation) {
        assertThat(observation.successCount()).isEqualTo(3L);
        assertThat(observation.failureCount()).isEqualTo(7L);
        assertThat(observation.failureTypes()).containsOnly(Map.entry(ErrorCode.INSUFFICIENT_POINT.name(), 7L));
        assertThat(observation.orderCount()).isEqualTo(3L);
        assertThat(observation.useHistoryCount()).isEqualTo(3L);
        assertThat(observation.balance()).isEqualTo(1_000L);
        assertThat(observation.violations()).isEmpty();
    }

    private void deleteFixture(Fixture fixture) {
        jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id IN (SELECT id FROM orders WHERE user_id = ?)", fixture.userId());
        jdbcTemplate.update("DELETE FROM orders WHERE user_id = ?", fixture.userId());
        jdbcTemplate.update("DELETE FROM point_history WHERE user_id = ?", fixture.userId());
        pointWalletRepository.deleteById(fixture.walletId());
        menuRepository.deleteById(fixture.menuId());
        userRepository.deleteById(fixture.userId());
    }

    private record Fixture(Long userId, Long walletId, Long menuId) {
    }

    private record Outcome(boolean success, Integer statusCode, String reason) {
        static Outcome successful() {
            return new Outcome(true, null, null);
        }

        static Outcome failure(String reason) {
            return new Outcome(false, null, reason);
        }

        static Outcome apiFailure(int statusCode, String reason) {
            return new Outcome(false, statusCode, reason);
        }
    }

    private record Observation(long successCount, long failureCount, long orderCount, long useHistoryCount,
                               long balance, Map<String, Long> failureTypes, Map<Integer, Long> failureStatusCodes,
                               List<String> violations) {
    }
}
