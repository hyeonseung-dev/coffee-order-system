package com.example.coffeeordersystem.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/** Redis Sentinel 장애 전환 뒤 공유 연결의 유효성을 요청 전에 확인한다. */
@Configuration
@Profile("redis-ha")
public class RedisHaLettuceConnectionConfig {

	@Bean
	BeanPostProcessor redisHaLettuceConnectionValidator() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				if (bean instanceof LettuceConnectionFactory lettuceConnectionFactory) {
					lettuceConnectionFactory.setValidateConnection(true);
				}
				return bean;
			}
		};
	}
}
