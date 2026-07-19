package com.example.coffeeordersystem.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_events_status_created_at", columnList = "status,created_at"))
public class OutboxEvent {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(nullable = false, unique = true, length = 36) private String eventId;
	@Column(nullable = false, length = 50) private String eventType;
	@Column(nullable = false) private Long aggregateId;
	@Lob @Column(nullable = false) private String payload;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OutboxEventStatus status;
	@Column(nullable = false) private int retryCount;
	@Column(length = 1000) private String lastError;
	@Column(nullable = false) private Instant createdAt;
	private Instant processedAt;
	protected OutboxEvent() {}
	private OutboxEvent(String eventId, String eventType, Long aggregateId, String payload, Instant createdAt) {
		this.eventId=eventId; this.eventType=eventType; this.aggregateId=aggregateId; this.payload=payload;
		this.status=OutboxEventStatus.PENDING; this.createdAt=createdAt;
	}
	public static OutboxEvent pending(String eventId, String eventType, Long aggregateId, String payload, Instant createdAt) { return new OutboxEvent(eventId,eventType,aggregateId,payload,createdAt); }
	public void markSent(Instant processedAt) { status=OutboxEventStatus.SENT; this.processedAt=processedAt; lastError=null; }
	public void markFailed(String error, int maxRetryCount) { retryCount++; lastError=error; if (retryCount >= maxRetryCount) status=OutboxEventStatus.FAILED; }
	public Long getId(){return id;} public String getEventId(){return eventId;} public String getEventType(){return eventType;} public Long getAggregateId(){return aggregateId;} public String getPayload(){return payload;} public OutboxEventStatus getStatus(){return status;} public int getRetryCount(){return retryCount;} public String getLastError(){return lastError;} public Instant getCreatedAt(){return createdAt;} public Instant getProcessedAt(){return processedAt;}
}
