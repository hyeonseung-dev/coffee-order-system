package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.dto.PopularMenuResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Redis 컨테이너를 중지한 명시적 실행에서 실제 네트워크 실패 fallback을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "redis.failure.test", matches = "true")
class PopularMenuCacheRedisUnavailableIntegrationTest {

	@Autowired private PopularMenuCache popularMenuCache;

	@Test
	void Redis가_중지돼도_읽기와_저장_실패를_격리하고_MySQL_결과를_반환한다() {
		List<PopularMenuResponse> databaseResult = List.of(new PopularMenuResponse(1L, "Americano", 5L));

		assertThat(popularMenuCache.findByBusinessDate(
				LocalDate.of(2026, 7, 18),
				() -> databaseResult
		)).isEqualTo(databaseResult);
	}
}
