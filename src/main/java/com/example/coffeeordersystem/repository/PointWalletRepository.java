package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 포인트 지갑 Entity의 영속성 접근을 담당하는 Repository다.
 *
 * 포인트 충전에서는 사용자에 연결된 지갑을 조회하고,
 * 개발용 초기 데이터에서는 이미 지갑이 있는지 확인한다.
 */
public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {

	/**
	 * 지정한 사용자가 소유한 포인트 지갑을 조회한다.
	 *
	 * @param user 지갑 소유자
	 * @return 사용자와 연결된 포인트 지갑
	 */
	Optional<PointWallet> findByUser(User user);

	/**
	 * 개발용 지갑 초기화 시 중복 생성을 막기 위해 사용자 지갑 존재 여부를 확인한다.
	 *
	 * @param user 지갑 소유자
	 * @return 해당 사용자 지갑이 이미 있으면 true
	 */
	boolean existsByUser(User user);
}
