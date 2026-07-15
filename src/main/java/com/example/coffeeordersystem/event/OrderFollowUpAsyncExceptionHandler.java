package com.example.coffeeordersystem.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/** 반환값 없는 주문 후속 비동기 작업의 예외를 기록한다. */
@Component
public class OrderFollowUpAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderFollowUpAsyncExceptionHandler.class);

	@Override
	public void handleUncaughtException(Throwable exception, Method method, Object... parameters) {
		log.error("[ASYNC] method={} exception={}: {}", method.getName(),
				exception.getClass().getSimpleName(), exception.getMessage(), exception);
	}
}
