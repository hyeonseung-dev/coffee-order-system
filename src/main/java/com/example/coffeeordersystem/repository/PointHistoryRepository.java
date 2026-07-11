package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 포인트 변동 이력 Entity의 영속성 접근을 담당하는 Repository다.
 *
 * 현재 기능에서는 충전 성공 시 생성된 이력을 저장하는 기본 JPA 기능만 사용한다.
 */
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
