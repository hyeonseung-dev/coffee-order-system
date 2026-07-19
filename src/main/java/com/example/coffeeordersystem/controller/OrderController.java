package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.OrderRequest;
import com.example.coffeeordersystem.dto.OrderResponse;
import com.example.coffeeordersystem.dto.OrderResultResponse;
import com.example.coffeeordersystem.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커피 주문·결제 요청을 받는 REST Controller다.
 *
 * 요청 DTO 검증과 정상 응답 반환만 담당하고 주문 트랜잭션은 {@link OrderService}에 위임한다.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * POST /api/orders 요청을 받아 메뉴 한 개의 포인트 결제를 실행한다.
	 *
	 * @param request 주문 사용자와 메뉴 ID
	 * @return 완료 주문 결과를 data 필드에 담은 200 OK 응답
	 */
	@PostMapping
	public ResponseEntity<OrderResultResponse> order(@Valid @RequestBody OrderRequest request) {
		OrderResponse response = orderService.order(request.userId(), request.menuId());
		return ResponseEntity.ok(new OrderResultResponse(response));
	}
}
