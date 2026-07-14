package com.example.coffeeordersystem.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OrderCompletedEventListenerTest {

	@Test
	void Mock_로그에_UTC_주문시각과_업무시간대를_남긴다(CapturedOutput output) {
		Instant orderedAt = Instant.parse("2026-07-13T15:30:00Z");

		new OrderCompletedEventListener().handle(
				new OrderCompletedEvent(1L, 2L, 3L, 3000L, orderedAt, "Asia/Seoul"));

		assertThat(output).contains("orderedAt=" + orderedAt, "businessZone=Asia/Seoul");
	}
}
