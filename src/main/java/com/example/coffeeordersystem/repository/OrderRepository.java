package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 성공 주문 Entity의 영속성 접근을 담당하는 Repository다.
 *
 * Issue #8에서는 완료 주문 저장을 수행하며, 이후 인기 메뉴 조회는 이 데이터를 집계한다.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
