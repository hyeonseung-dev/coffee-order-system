package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!prod")
public class MenuDeveloperDataInitializer implements CommandLineRunner {

	private final MenuRepository menuRepository;

	public MenuDeveloperDataInitializer(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	@Override
	public void run(String... args) {
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
