package com.example.coffeeordersystem.controller;

import com.example.coffeeordersystem.dto.PointChargeResponse;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.service.PointChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포인트 충전 Controller의 요청 검증, 정상 응답, 전역 예외 응답을 검증하는 Web MVC 테스트다.
 *
 * Service는 mock으로 대체해 HTTP 계층의 경로, 상태 코드, 응답 body 계약에 집중한다.
 */
@WebMvcTest(PointChargeController.class)
class PointChargeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PointChargeService pointChargeService;

	@Test
	void 정상_충전_요청은_성공_응답과_data_구조를_반환한다() throws Exception {
		// given
		when(pointChargeService.charge(1L, 10000L))
				.thenReturn(new PointChargeResponse(1L, 10000L, 10000L));

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userId").value(1))
				.andExpect(jsonPath("$.data.chargedAmount").value(10000))
				.andExpect(jsonPath("$.data.balance").value(10000));
	}

	@Test
	void 충전금액이_0원이면_잘못된_요청_응답을_반환한다() throws Exception {
		// given
		String requestBody = "{\"amount\":0}";

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."));
		verify(pointChargeService, never()).charge(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void 충전금액이_음수이면_잘못된_요청_응답을_반환한다() throws Exception {
		// given
		String requestBody = "{\"amount\":-1000}";

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."));
		verify(pointChargeService, never()).charge(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void 충전금액이_누락되면_잘못된_요청_응답을_반환한다() throws Exception {
		// given
		String requestBody = "{}";

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("충전 금액은 필수입니다."));
		verify(pointChargeService, never()).charge(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void 잘못된_JSON이면_잘못된_요청_응답을_반환한다() throws Exception {
		// given
		String requestBody = "{\"amount\":";

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
		verify(pointChargeService, never()).charge(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void 존재하지_않는_사용자이면_찾을_수_없음_응답을_반환한다() throws Exception {
		// given
		when(pointChargeService.charge(999L, 10000L))
				.thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 999L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
	}

	@Test
	void 사용자_지갑이_없으면_찾을_수_없음_응답을_반환한다() throws Exception {
		// given
		when(pointChargeService.charge(1L, 10000L))
				.thenThrow(new BusinessException(ErrorCode.POINT_WALLET_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POINT_WALLET_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("포인트 지갑을 찾을 수 없습니다."));
	}

	@Test
	void 도메인_검증에서_충전금액이_유효하지_않으면_잘못된_요청_응답을_반환한다() throws Exception {
		// given
		when(pointChargeService.charge(1L, 10000L))
				.thenThrow(new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT));

		// when & then
		mockMvc.perform(post("/api/users/{userId}/points/charge", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHARGE_AMOUNT"))
				.andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."));
	}
}
