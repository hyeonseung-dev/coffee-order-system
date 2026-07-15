package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.*;
import com.example.coffeeordersystem.event.OrderCompletedOutboxPayload;
import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import com.example.coffeeordersystem.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Component
public class OutboxPublisher {
	private final OutboxEventRepository repository; private final ObjectMapper objectMapper; private final OrderDataPlatformClient client; private final Clock clock; private final int batchSize; private final int maxRetryCount;
	public OutboxPublisher(OutboxEventRepository repository, ObjectMapper objectMapper, OrderDataPlatformClient client, Clock clock,
			@Value("${outbox.publisher.batch-size:100}") int batchSize, @Value("${outbox.publisher.max-retry-count:3}") int maxRetryCount) {
		this.repository=repository; this.objectMapper=objectMapper; this.client=client; this.clock=clock; this.batchSize=batchSize; this.maxRetryCount=maxRetryCount;
	}
	@Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
	@Transactional
	public void publishPending() {
		for (OutboxEvent event : repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0,batchSize))) {
			try { OrderCompletedOutboxPayload p=objectMapper.readValue(event.getPayload(), OrderCompletedOutboxPayload.class); client.sendOrderCompleted(p); event.markSent(clock.instant()); }
			catch (Exception e) { event.markFailed(e.getClass().getSimpleName()+": "+e.getMessage(), maxRetryCount); }
		}
	}
}
