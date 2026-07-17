package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.dto.PopularMenuResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularMenuCacheTest {

	@Mock private StringRedisTemplate redisTemplate;
	@Mock private ValueOperations<String, String> valueOperations;

	private PopularMenuCache popularMenuCache;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		popularMenuCache = new PopularMenuCache(redisTemplate, new ObjectMapper(), 86_400L);
	}

	@Test
	void Cache_Miss이면_MySQL_결과를_TTL과_함께_저장한다() {
		LocalDate businessDate = LocalDate.of(2026, 7, 17);
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1")).thenReturn(null);

		List<PopularMenuResponse> result = popularMenuCache.findByBusinessDate(businessDate, () -> databaseResult);

		assertThat(result).isEqualTo(databaseResult);
		verify(valueOperations).set(
				eq("popular:menus:7days:2026-07-17:v1"),
				eq("[{\"menuId\":1,\"name\":\"Americano\",\"orderCount\":5}]"),
				eq(Duration.ofHours(24))
		);
	}

	@Test
	void Cache_Hit이면_MySQL_조회와_캐시_저장을_생략한다() {
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1"))
				.thenReturn("[{\"menuId\":1,\"name\":\"Americano\",\"orderCount\":5}]");
		AtomicInteger databaseCalls = new AtomicInteger();

		List<PopularMenuResponse> result = popularMenuCache.findByBusinessDate(
				LocalDate.of(2026, 7, 17),
				() -> {
					databaseCalls.incrementAndGet();
					return List.of();
				}
		);

		assertThat(result).containsExactly(new PopularMenuResponse(1L, "Americano", 5L));
		assertThat(databaseCalls).hasValue(0);
		verify(valueOperations, never()).set(any(), any(), any(Duration.class));
	}

	@Test
	void 빈_목록도_Cache_Hit으로_반환한다() {
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1")).thenReturn("[]");

		List<PopularMenuResponse> result = popularMenuCache.findByBusinessDate(
				LocalDate.of(2026, 7, 17),
				() -> { throw new AssertionError("MySQL을 조회하면 안 됩니다."); }
		);

		assertThat(result).isEmpty();
	}

	@Test
	void Redis_조회_실패면_MySQL_결과를_반환한다() {
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1")).thenThrow(new IllegalStateException("redis down"));
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));

		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 17), () -> databaseResult))
				.isEqualTo(databaseResult);
	}

	@Test
	void Redis_저장_실패여도_MySQL_결과를_반환한다() {
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1")).thenReturn(null);
		doThrow(new IllegalStateException("redis down")).when(valueOperations)
				.set(any(), any(), any(Duration.class));
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));

		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 17), () -> databaseResult))
				.isEqualTo(databaseResult);
	}

	@Test
	void 역직렬화_실패면_손상된_Key를_삭제하고_MySQL_결과를_반환한다() {
		when(valueOperations.get("popular:menus:7days:2026-07-17:v1")).thenReturn("not-json");
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));

		assertThat(popularMenuCache.findByBusinessDate(LocalDate.of(2026, 7, 17), () -> databaseResult))
				.isEqualTo(databaseResult);
		verify(redisTemplate).delete("popular:menus:7days:2026-07-17:v1");
	}

	@Test
	void KST_업무_날짜가_달라지면_다른_Key를_사용한다() {
		assertThat(popularMenuCache.keyOf(LocalDate.of(2026, 7, 17)))
				.isEqualTo("popular:menus:7days:2026-07-17:v1");
		assertThat(popularMenuCache.keyOf(LocalDate.of(2026, 7, 18)))
				.isEqualTo("popular:menus:7days:2026-07-18:v1");
	}
}
