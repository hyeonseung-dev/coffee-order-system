package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.PointHistory;
import com.example.coffeeordersystem.domain.PointHistoryType;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.PointChargeResponse;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 포인트 충전 Service의 잔액 변경, 이력 저장, 실패 조건을 검증한다.
 *
 * Repository는 mock으로 대체하고, 도메인 객체의 실제 상태 변화를 함께 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class PointChargeServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PointWalletRepository pointWalletRepository;

	@Mock
	private PointHistoryRepository pointHistoryRepository;

	@InjectMocks
	private PointChargeService pointChargeService;

	@Test
	void 정상_충전하면_잔액이_증가하고_응답에_충전금액과_잔액을_반환한다() {
		// given
		User user = user(1L, "테스트사용자");
		PointWallet wallet = PointWallet.create(user, 1000L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(pointWalletRepository.findByUserForUpdate(user)).thenReturn(Optional.of(wallet));

		// when
		PointChargeResponse response = pointChargeService.charge(1L, 10000L);

		// then
		assertThat(wallet.getBalance()).isEqualTo(11000L);
		assertThat(response).isEqualTo(new PointChargeResponse(1L, 10000L, 11000L));
	}

	@Test
	void 정상_충전하면_CHARGE_이력을_충전후_잔액과_함께_저장한다() {
		// given
		User user = user(1L, "테스트사용자");
		PointWallet wallet = PointWallet.create(user, 1000L);
		ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(pointWalletRepository.findByUserForUpdate(user)).thenReturn(Optional.of(wallet));

		// when
		pointChargeService.charge(1L, 10000L);

		// then
		verify(pointHistoryRepository).save(historyCaptor.capture());
		PointHistory history = historyCaptor.getValue();
		assertThat(history.getUser()).isSameAs(user);
		assertThat(history.getAmount()).isEqualTo(10000L);
		assertThat(history.getType()).isEqualTo(PointHistoryType.CHARGE);
		assertThat(history.getBalanceAfter()).isEqualTo(11000L);
	}

	@Test
	void 충전금액이_0원이면_충전에_실패하고_이력을_저장하지_않는다() {
		// given
		User user = user(1L, "테스트사용자");
		PointWallet wallet = PointWallet.create(user, 1000L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(pointWalletRepository.findByUserForUpdate(user)).thenReturn(Optional.of(wallet));

		// when & then
		assertThatThrownBy(() -> pointChargeService.charge(1L, 0L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT))
				.hasMessage("충전 금액은 0보다 커야 합니다.");
		assertThat(wallet.getBalance()).isEqualTo(1000L);
		verify(pointHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 충전금액이_음수이면_충전에_실패하고_이력을_저장하지_않는다() {
		// given
		User user = user(1L, "테스트사용자");
		PointWallet wallet = PointWallet.create(user, 1000L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(pointWalletRepository.findByUserForUpdate(user)).thenReturn(Optional.of(wallet));

		// when & then
		assertThatThrownBy(() -> pointChargeService.charge(1L, -1000L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT))
				.hasMessage("충전 금액은 0보다 커야 합니다.");
		assertThat(wallet.getBalance()).isEqualTo(1000L);
		verify(pointHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 존재하지_않는_사용자이면_충전에_실패한다() {
		// given
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> pointChargeService.charge(999L, 10000L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND))
				.hasMessage("사용자를 찾을 수 없습니다.");
		verify(pointWalletRepository, never()).findByUserForUpdate(org.mockito.ArgumentMatchers.any());
		verify(pointHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 사용자_지갑이_없으면_충전에_실패한다() {
		// given
		User user = user(1L, "테스트사용자");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(pointWalletRepository.findByUserForUpdate(user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> pointChargeService.charge(1L, 10000L))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POINT_WALLET_NOT_FOUND))
				.hasMessage("포인트 지갑을 찾을 수 없습니다.");
		verify(pointHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	private User user(Long id, String name) {
		User user = User.create(name);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
