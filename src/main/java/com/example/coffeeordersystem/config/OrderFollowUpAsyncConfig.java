package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.event.OrderFollowUpAsyncExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.util.concurrent.ThreadPoolExecutor;

/** 주문 완료 후 외부 전송만 처리하는 전용 비동기 Executor 설정이다. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class OrderFollowUpAsyncConfig implements AsyncConfigurer {

	private final OrderFollowUpAsyncExceptionHandler asyncExceptionHandler;
	private final OrderFollowUpTaskRejectedHandler taskRejectedHandler;

	public OrderFollowUpAsyncConfig(OrderFollowUpAsyncExceptionHandler asyncExceptionHandler,
			OrderFollowUpTaskRejectedHandler taskRejectedHandler) {
		this.asyncExceptionHandler = asyncExceptionHandler;
		this.taskRejectedHandler = taskRejectedHandler;
	}

	@Bean(name = "orderFollowUpExecutor")
	ThreadPoolTaskExecutor orderFollowUpExecutor(
			@Value("${order.follow-up.executor.core-pool-size:2}") int corePoolSize,
			@Value("${order.follow-up.executor.max-pool-size:4}") int maxPoolSize,
			@Value("${order.follow-up.executor.queue-capacity:100}") int queueCapacity) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("order-follow-up-");
		// 주문 요청 스레드를 점유하지 않고, 포화 시 작업 유실을 드러내기 위해 즉시 거절한다.
		executor.setRejectedExecutionHandler(taskRejectedHandler);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return asyncExceptionHandler;
	}
}
