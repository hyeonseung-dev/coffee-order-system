package com.example.coffeeordersystem.service;

import com.example.coffeeordersystem.domain.PointHistory;
import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.dto.PointChargeResponse;
import com.example.coffeeordersystem.exception.BusinessException;
import com.example.coffeeordersystem.exception.ErrorCode;
import com.example.coffeeordersystem.repository.PointHistoryRepository;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 충전 비즈니스 흐름을 담당하는 Service다.
 *
 * 사용자와 포인트 지갑을 조회한 뒤 잔액 증가와 충전 이력 저장을
 * 하나의 트랜잭션 안에서 처리한다.
 */
@Service
public class PointChargeService {

	private final UserRepository userRepository;
	private final PointWalletRepository pointWalletRepository;
	private final PointHistoryRepository pointHistoryRepository;

	public PointChargeService(
			UserRepository userRepository,
			PointWalletRepository pointWalletRepository,
			PointHistoryRepository pointHistoryRepository
	) {
		this.userRepository = userRepository;
		this.pointWalletRepository = pointWalletRepository;
		this.pointHistoryRepository = pointHistoryRepository;
	}

	/**
	 * 지정한 사용자의 포인트 지갑에 요청 금액을 충전한다.
	 *
	 * 사용자와 포인트 지갑이 존재해야 하며, 충전 금액은 0보다 커야 한다.
	 * 잔액 변경과 {@link PointHistory} 저장은 같은 트랜잭션에 묶여
	 * 이력 저장 실패 시 잔액 변경도 함께 롤백된다.
	 *
	 * @param userId 포인트를 충전할 사용자 식별자
	 * @param amount 충전할 포인트 금액
	 * @return 충전 금액과 충전 후 잔액을 담은 응답 DTO
	 * @throws BusinessException 사용자가 없거나 지갑이 없거나 충전 금액이 유효하지 않은 경우
	 */
	@Transactional
	public PointChargeResponse charge(Long userId, Long amount) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		PointWallet wallet = pointWalletRepository.findByUserForUpdate(user)
				.orElseThrow(() -> new BusinessException(ErrorCode.POINT_WALLET_NOT_FOUND));

		wallet.charge(amount);
		PointHistory history = PointHistory.charge(user, amount, wallet.getBalance());
		pointHistoryRepository.save(history);

		return new PointChargeResponse(user.getId(), amount, wallet.getBalance());
	}
}
