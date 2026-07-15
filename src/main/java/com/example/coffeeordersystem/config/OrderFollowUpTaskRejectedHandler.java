package com.example.coffeeordersystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/** 주문 후속 작업 포화를 기록하고 AbortPolicy와 동일하게 즉시 거절한다. */
@Component
public class OrderFollowUpTaskRejectedHandler implements RejectedExecutionHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderFollowUpTaskRejectedHandler.class);

	@Override
	public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
		log.error("[ASYNC] executor=orderFollowUpExecutor rejected activeCount={} queueSize={}",
				executor.getActiveCount(), executor.getQueue().size());
		new ThreadPoolExecutor.AbortPolicy().rejectedExecution(task, executor);
	}
}
