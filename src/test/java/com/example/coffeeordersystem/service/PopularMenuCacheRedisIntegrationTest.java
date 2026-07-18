package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.dto.PopularMenuResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Docker Compose Redis에 실제로 연결해 캐시 Key, TTL, 만료 뒤 재생성을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "redis.integration.test", matches = "true")
class PopularMenuCacheRedisIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(PopularMenuCacheRedisIntegrationTest.class);
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
	private static final String KEY = "popular:menus:7days:2026-07-18:v1";

	@Autowired private PopularMenuCache popularMenuCache;
	@Autowired private StringRedisTemplate redisTemplate;

	@Test
	void Sentinel을_통해_현재_Master에_연결한다() {
		assertThat(redisTemplate.getConnectionFactory().getSentinelConnection())
				.isNotNull();
	}

	@AfterEach
	void tearDown() {
		redisTemplate.delete(KEY);
	}

	@Test
	void 실제_Redis에_KST_업무_날짜_Key와_2초_TTL로_저장하고_Hit과_만료_후_재생성을_검증한다() throws InterruptedException {
		LocalDate businessDate = Instant.parse("2026-07-17T15:00:00Z")
				.atZone(BUSINESS_ZONE)
				.toLocalDate();
		assertThat(businessDate).isEqualTo(LocalDate.of(2026, 7, 18));

		AtomicInteger databaseCalls = new AtomicInteger();
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));
		List<PopularMenuResponse> first = popularMenuCache.findByBusinessDate(businessDate, () -> {
			databaseCalls.incrementAndGet();
			return databaseResult;
		});

		assertThat(first).isEqualTo(databaseResult);
		assertThat(redisTemplate.opsForValue().get(KEY))
				.isEqualTo("[{\"menuId\":1,\"name\":\"Americano\",\"orderCount\":5}]");
		Long initialTtl = redisTemplate.getExpire(KEY, TimeUnit.SECONDS);
		assertThat(initialTtl).isBetween(1L, 2L);
		log.info("[REDIS-EVIDENCE] key={} value={} ttlSeconds={} databaseCalls={}",
				KEY, redisTemplate.opsForValue().get(KEY), initialTtl, databaseCalls.get());

		List<PopularMenuResponse> second = popularMenuCache.findByBusinessDate(businessDate, () -> {
			databaseCalls.incrementAndGet();
			return List.of();
		});
		assertThat(second).isEqualTo(databaseResult);
		assertThat(databaseCalls).hasValue(1);
		log.info("[REDIS-EVIDENCE] cacheHit key={} databaseCalls={}", KEY, databaseCalls.get());

		Thread.sleep(2_200L);
		assertThat(redisTemplate.hasKey(KEY)).isFalse();
		log.info("[REDIS-EVIDENCE] expired key={} exists={}", KEY, redisTemplate.hasKey(KEY));

		List<PopularMenuResponse> regenerated = popularMenuCache.findByBusinessDate(businessDate, () -> {
			databaseCalls.incrementAndGet();
			return databaseResult;
		});
		assertThat(regenerated).isEqualTo(databaseResult);
		assertThat(databaseCalls).hasValue(2);
		assertThat(redisTemplate.hasKey(KEY)).isTrue();
		log.info("[REDIS-EVIDENCE] regenerated key={} databaseCalls={}", KEY, databaseCalls.get());
	}

	@Test
	void 실제_Redis에_빈_목록을_Miss와_구분되는_JSON_값으로_저장한다() {
		AtomicInteger databaseCalls = new AtomicInteger();
		List<PopularMenuResponse> result = popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 18), () -> {
			databaseCalls.incrementAndGet();
			return List.of();
		});

		assertThat(result).isEmpty();
		assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo("[]");
		log.info("[REDIS-EVIDENCE] emptyValue key={} value={}", KEY, redisTemplate.opsForValue().get(KEY));

		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 18), () -> {
			databaseCalls.incrementAndGet();
			return List.of(new PopularMenuResponse(9L, "ShouldNotQuery", 1L));
		})).isEmpty();
		assertThat(databaseCalls).hasValue(1);
	}
}
