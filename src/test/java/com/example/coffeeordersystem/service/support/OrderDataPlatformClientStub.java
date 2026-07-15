package com.example.coffeeordersystem.service.support;

import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** 테스트에서 외부 플랫폼 호출의 정상·지연·예외 동작을 읽기 쉽게 설정한다. */
public final class OrderDataPlatformClientStub {

	private static final Logger log = LoggerFactory.getLogger(OrderDataPlatformClientStub.class);

	private final OrderDataPlatformClient client;
	private String clientThreadName;

	public OrderDataPlatformClientStub(OrderDataPlatformClient client) {
		this.client = client;
	}

	public void succeed() {
		doAnswer(invocation -> {
			recordClientExecution("정상 응답");
			return null;
		}).when(client).sendOrderCompleted(any(), any(), any(), any(), any(), any());
	}

	public void delayFor(Duration duration) {
		doAnswer(invocation -> {
			recordClientExecution(duration.toMillis() + "ms 지연 시작");
			try {
				Thread.sleep(duration.toMillis());
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("외부 플랫폼 지연이 인터럽트되었습니다.", exception);
			}
			log.info("[CLIENT] condition={} thread={}", duration.toMillis() + "ms 지연 완료", clientThreadName);
			return null;
		}).when(client).sendOrderCompleted(any(), any(), any(), any(), any(), any());
	}

	public void failWith(RuntimeException exception) {
		doAnswer(invocation -> {
			recordClientExecution("예외 발생: " + exception.getMessage());
			throw exception;
		}).when(client).sendOrderCompleted(any(), any(), any(), any(), any(), any());
	}

	public String clientThreadName() {
		return clientThreadName;
	}

	private void recordClientExecution(String condition) {
		clientThreadName = Thread.currentThread().getName();
		log.info("[CLIENT] condition={} thread={}", condition, clientThreadName);
	}
}
