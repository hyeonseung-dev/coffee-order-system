package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 주문 Controller의 요청·응답 계약과 비즈니스 오류 변환을 검증하는 Web MVC 테스트다. */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired private MockMvc mockMvc;
	@MockitoBean private OrderService orderService;

	@Test
	void 정상_주문은_완료_주문_응답을_반환한다() throws Exception {
		when(orderService.order(1L, 2L)).thenReturn(new OrderResponse(3L, 1L, 2L, 3000L, 7000L,
				LocalDateTime.of(2026, 7, 12, 12, 0)));

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{\"userId\":1,\"menuId\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orderId").value(3))
				.andExpect(jsonPath("$.data.orderPrice").value(3000))
				.andExpect(jsonPath("$.data.remainingBalance").value(7000));
	}

	@Test
	void 포인트가_부족하면_정의된_오류_응답을_반환한다() throws Exception {
		when(orderService.order(1L, 2L)).thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{\"userId\":1,\"menuId\":2}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"));
	}
}
