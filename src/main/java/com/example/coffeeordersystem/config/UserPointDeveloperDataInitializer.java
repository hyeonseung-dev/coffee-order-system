package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경에서 포인트 충전 API를 바로 호출할 수 있도록 기본 사용자와 지갑을 준비한다.
 *
 * prod 프로필에서는 실행되지 않으며, 사용자와 지갑을 각각 확인해 재시작 시 중복 생성을 피한다.
 */
@Component
@Profile("!prod")
public class UserPointDeveloperDataInitializer implements CommandLineRunner {

	private static final String DEFAULT_USER_NAME = "테스트사용자";

	private final UserRepository userRepository;
	private final PointWalletRepository pointWalletRepository;

	public UserPointDeveloperDataInitializer(
			UserRepository userRepository,
			PointWalletRepository pointWalletRepository
	) {
		this.userRepository = userRepository;
		this.pointWalletRepository = pointWalletRepository;
	}

	@Override
	public void run(String... args) {
		// 메뉴 초기화와 분리해 포인트 API 검증에 필요한 최소 사용자만 준비한다.
		User user = userRepository.findByName(DEFAULT_USER_NAME)
				.orElseGet(() -> userRepository.save(User.create(DEFAULT_USER_NAME)));

		// 사용자는 있지만 지갑이 없는 개발 DB 상태도 복구할 수 있게 지갑 존재 여부를 별도로 본다.
		if (pointWalletRepository.existsByUser(user)) {
			return;
		}

		pointWalletRepository.save(PointWallet.create(user, 0L));
	}
}
