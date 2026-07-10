package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 Entity의 영속성 접근을 담당하는 Repository다.
 *
 * 포인트 충전에서는 사용자 식별자 조회를 사용하고,
 * 개발용 초기 데이터에서는 이름 기반 조회로 중복 생성을 방지한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 개발용 초기 사용자 생성을 위해 이름이 같은 사용자를 조회한다.
	 *
	 * @param name 사용자 이름
	 * @return 이름이 일치하는 사용자
	 */
	Optional<User> findByName(String name);
}
