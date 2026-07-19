package com.example.coffeeordersystem.event;

import java.time.Instant;

public record OrderCompletedOutboxPayload(String eventId, String eventType, Long orderId, Long userId, Long menuId,
		Long orderPrice, Instant orderedAt, String businessZone) {}
