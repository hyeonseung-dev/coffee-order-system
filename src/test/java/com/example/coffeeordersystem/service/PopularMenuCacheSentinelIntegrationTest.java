package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.dto.PopularMenuResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "redis-ha"})
@EnabledIfSystemProperty(named = "redis.sentinel.integration.test", matches = "true")
class PopularMenuCacheSentinelIntegrationTest {
	private static final String MASTER = "coffee-order-redis";
	private static final String KEY = "popular:menus:7days:2026-07-18:v1";
	@Autowired private PopularMenuCache popularMenuCache;
	@Autowired private StringRedisTemplate redisTemplate;

	@Test
	void Sentinel이_발견한_Master에서_Cache_Aside와_TTL_재생성을_검증한다() throws Exception {
		try (RedisSentinelConnection sentinel = redisTemplate.getConnectionFactory().getSentinelConnection()) {
			assertThat(sentinel.masters()).anyMatch(master -> MASTER.equals(master.getName())
					&& master.getHost() != null && master.getPort() > 0);
		}
		redisTemplate.delete(KEY);
		AtomicInteger calls = new AtomicInteger();
		List<PopularMenuResponse> value = List.of(new PopularMenuResponse(1L, "Americano", 5L));
		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 18), () -> { calls.incrementAndGet(); return value; })).isEqualTo(value);
		assertThat(redisTemplate.hasKey(KEY)).isTrue();
		assertThat(redisTemplate.getExpire(KEY)).isPositive();
		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 18), List::of)).isEqualTo(value);
		assertThat(calls).hasValue(1);
		Thread.sleep(2_200L);
		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 18), () -> { calls.incrementAndGet(); return value; })).isEqualTo(value);
		assertThat(calls).hasValue(2);
	}
}
