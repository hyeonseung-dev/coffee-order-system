package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로컬 개발 환경에서 메뉴 목록 API를 바로 확인할 수 있도록 기본 메뉴를 등록한다.
 *
 * prod 프로필에서는 실행되지 않으며, 기존 메뉴가 하나라도 있으면 중복 등록하지 않는다.
 */
@Component
@Profile("!prod")
public class MenuDeveloperDataInitializer implements CommandLineRunner {

	private final MenuRepository menuRepository;

	public MenuDeveloperDataInitializer(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	@Override
	public void run(String... args) {
		// 개발용 데이터가 재시작 때마다 누적되지 않도록 메뉴가 비어 있을 때만 저장한다.
		if (menuRepository.count() > 0) {
			return;
		}

		menuRepository.saveAll(List.of(
				Menu.create("아이스아메리카노", 3000L, MenuStatus.ACTIVE),
				Menu.create("카페모카", 4500L, MenuStatus.ACTIVE),
				Menu.create("아이스초코", 4500L, MenuStatus.ACTIVE),
				Menu.create("자몽허니블랙티", 4800L, MenuStatus.ACTIVE),
				Menu.create("허니브레드", 6000L, MenuStatus.ACTIVE)
		));
	}
}
