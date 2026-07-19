package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.dto.PopularMenuResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/** MySQL 인기 메뉴 집계 결과를 Cache-Aside 방식으로 보관한다. */
@Component
public class PopularMenuCache {
	private static final Logger log = LoggerFactory.getLogger(PopularMenuCache.class);
	private static final String KEY_PREFIX = "popular:menus:7days:";
	private static final String KEY_SUFFIX = ":v1";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration ttl;
	private final boolean enabled;

	public PopularMenuCache(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			@Value("${popular-menu-cache.ttl-seconds}") long ttlSeconds,
			@Value("${popular-menu-cache.enabled:true}") boolean enabled
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.ttl = Duration.ofSeconds(ttlSeconds);
		this.enabled = enabled;
	}

	public List<PopularMenuResponse> findByBusinessDate(
			LocalDate businessDate,
			Supplier<List<PopularMenuResponse>> databaseQuery
	) {
		if (!enabled) {
			return databaseQuery.get();
		}

		String key = keyOf(businessDate);
		List<PopularMenuResponse> cached = get(key);
		if (cached != null) {
			return cached;
		}

		List<PopularMenuResponse> result = databaseQuery.get();
		put(key, result);
		return result;
	}

	String keyOf(LocalDate businessDate) {
		return KEY_PREFIX + businessDate + KEY_SUFFIX;
	}

	private List<PopularMenuResponse> get(String key) {
		try {
			String serialized = redisTemplate.opsForValue().get(key);
			if (serialized == null) {
				return null;
			}
			return objectMapper.readValue(serialized, new TypeReference<>() { });
		} catch (Exception exception) {
			log.warn("인기 메뉴 캐시 조회에 실패해 MySQL 원본을 조회합니다. key={}", key, exception);
			deleteCorruptedValue(key);
			return null;
		}
	}

	private void put(String key, List<PopularMenuResponse> result) {
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), ttl);
		} catch (JsonProcessingException exception) {
			log.warn("인기 메뉴 캐시 직렬화에 실패해 MySQL 결과만 반환합니다. key={}", key, exception);
		} catch (Exception exception) {
			log.warn("인기 메뉴 캐시 저장에 실패해 MySQL 결과만 반환합니다. key={}", key, exception);
		}
	}

	private void deleteCorruptedValue(String key) {
		try {
			redisTemplate.delete(key);
		} catch (Exception exception) {
			log.warn("손상된 인기 메뉴 캐시 삭제에 실패했습니다. key={}", key, exception);
		}
	}
}
