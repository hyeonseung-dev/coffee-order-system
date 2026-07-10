package com.example.coffeeordersystem.repository;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	List<Menu> findAllByStatusOrderByIdAsc(MenuStatus status);
}
