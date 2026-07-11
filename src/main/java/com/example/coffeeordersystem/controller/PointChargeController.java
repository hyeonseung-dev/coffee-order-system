package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.PointChargeRequest;
import com.example.coffeeordersystem.dto.PointChargeResponse;
import com.example.coffeeordersystem.dto.PointChargeResultResponse;
import com.example.coffeeordersystem.service.PointChargeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 포인트 충전 요청을 처리하는 REST Controller다.
 *
 * 경로 변수와 요청 본문 검증, 정상 응답 반환만 담당하며,
 * 사용자 조회와 잔액 변경 규칙은 {@link PointChargeService}에 위임한다.
 */
@RestController
@RequestMapping("/api/users/{userId}/points")
public class PointChargeController {

	private final PointChargeService pointChargeService;

	public PointChargeController(PointChargeService pointChargeService) {
		this.pointChargeService = pointChargeService;
	}

	/**
	 * POST /api/users/{userId}/points/charge 요청을 받아 포인트 충전을 실행한다.
	 *
	 * @param userId 포인트를 충전할 사용자 식별자
	 * @param request 충전 금액을 담은 요청 DTO
	 * @return 충전 결과를 data 필드에 담은 200 OK 응답
	 */
	@PostMapping("/charge")
	public ResponseEntity<PointChargeResultResponse> charge(
			@PathVariable Long userId,
			@Valid @RequestBody PointChargeRequest request
	) {
		PointChargeResponse response = pointChargeService.charge(userId, request.amount());
		return ResponseEntity.ok(new PointChargeResultResponse(response));
	}
}
