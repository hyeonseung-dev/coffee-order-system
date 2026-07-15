package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.*;
import com.example.coffeeordersystem.event.OrderCompletedOutboxPayload;
import com.example.coffeeordersystem.external.OrderDataPlatformClient;
import com.example.coffeeordersystem.repository.OutboxEventRepository;
import com.example.coffeeordersystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="outbox.publisher.max-retry-count=3") @ActiveProfiles("test")
class OutboxPublisherIntegrationTest {
	@Autowired OutboxPublisher publisher; @Autowired OutboxEventRepository repository; @Autowired ObjectMapper mapper;
	@Autowired OrderService orderService; @Autowired UserRepository users; @Autowired MenuRepository menus; @Autowired PointWalletRepository wallets; @Autowired PointHistoryRepository histories; @Autowired OrderRepository orders;
	@MockitoBean OrderDataPlatformClient client;
	@AfterEach void clean(){ repository.deleteAll(); orders.deleteAll(); histories.deleteAll(); wallets.deleteAll(); menus.deleteAll(); users.deleteAll(); }
	@Test void 주문과_포인트_USE_Outbox_PENDING은_함께_Commit되고_Payload가_주문값과_일치한다() throws Exception {
		User u=users.save(User.create("outbox-user")); wallets.save(PointWallet.create(u,10000L)); Menu m=menus.save(Menu.create("outbox-menu",3000L,MenuStatus.ACTIVE));
		var response=orderService.order(u.getId(),m.getId()); OutboxEvent event=repository.findAll().get(0); OrderCompletedOutboxPayload p=mapper.readValue(event.getPayload(),OrderCompletedOutboxPayload.class);
		assertThat(orders.count()).isEqualTo(1); assertThat(wallets.findByUser(u).orElseThrow().getBalance()).isEqualTo(7000); assertThat(histories.count()).isEqualTo(1); assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		assertThat(p.eventId()).isEqualTo(event.getEventId()); assertThat(p.eventType()).isEqualTo("ORDER_COMPLETED"); assertThat(p.orderId()).isEqualTo(response.orderId()); assertThat(p.userId()).isEqualTo(u.getId()); assertThat(p.menuId()).isEqualTo(m.getId()); assertThat(p.orderPrice()).isEqualTo(3000); assertThat(p.orderedAt()).isEqualTo(orders.findById(response.orderId()).orElseThrow().getOrderedAt()); assertThat(p.businessZone()).isEqualTo("Asia/Seoul");
	}
	@Test void 성공하면_SENT이고_다시_전송하지_않는다() throws Exception {
		OutboxEvent e=pending(); repository.save(e); publisher.publishPending();
		assertThat(repository.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.SENT); assertThat(repository.findById(e.getId()).orElseThrow().getProcessedAt()).isNotNull(); verify(client,times(1)).sendOrderCompleted(any(),any(),any(),any(),any(),any());
		publisher.publishPending(); verify(client,times(1)).sendOrderCompleted(any(),any(),any(),any(),any(),any());
	}
	@Test void 실패는_재시도후_SENT_최대면_FAILED() throws Exception {
		OutboxEvent e=repository.save(pending()); doThrow(new IllegalStateException("down")).when(client).sendOrderCompleted(any(),any(),any(),any(),any(),any());
		publisher.publishPending(); assertThat(repository.findById(e.getId()).orElseThrow().getRetryCount()).isEqualTo(1); assertThat(repository.findById(e.getId()).orElseThrow().getLastError()).contains("down"); assertThat(repository.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		doNothing().when(client).sendOrderCompleted(any(),any(),any(),any(),any(),any()); publisher.publishPending(); assertThat(repository.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.SENT);
		OutboxEvent failed=repository.save(pending()); doThrow(new IllegalStateException("down")).when(client).sendOrderCompleted(any(),any(),any(),any(),any(),any()); publisher.publishPending(); publisher.publishPending(); publisher.publishPending(); assertThat(repository.findById(failed.getId()).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.FAILED);
	}
	private OutboxEvent pending() throws Exception { String id=UUID.randomUUID().toString(); Instant at=Instant.parse("2026-07-15T00:00:00Z"); String p=mapper.writeValueAsString(new OrderCompletedOutboxPayload(id,"ORDER_COMPLETED",1L,2L,3L,3000L,at,"Asia/Seoul")); return OutboxEvent.pending(id,"ORDER_COMPLETED",1L,p,at); }
}
