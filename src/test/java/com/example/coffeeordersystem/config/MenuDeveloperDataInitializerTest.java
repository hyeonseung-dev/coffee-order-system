package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.domain.Menu;
import com.example.coffeeordersystem.domain.MenuStatus;
import com.example.coffeeordersystem.repository.MenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 개발용 메뉴 초기화가 기본 ACTIVE 메뉴를 한 번만 저장하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MenuDeveloperDataInitializerTest {

	@Mock
	private MenuRepository menuRepository;

	@InjectMocks
	private MenuDeveloperDataInitializer initializer;

	@Test
	@SuppressWarnings("unchecked")
	void 메뉴가_없으면_개발용_기본_ACTIVE_메뉴를_저장한다() {
		// given
		when(menuRepository.count()).thenReturn(0L);
		ArgumentCaptor<List<Menu>> captor = ArgumentCaptor.forClass(List.class);

		// when
		initializer.run();

		// then
		verify(menuRepository).saveAll(captor.capture());
		assertThat(captor.getValue())
				.extracting(Menu::getName)
				.containsExactly("아이스아메리카노", "카페모카", "아이스초코", "자몽허니블랙티", "허니브레드");
		assertThat(captor.getValue())
				.extracting(Menu::getPrice)
				.containsExactly(3000L, 4500L, 4500L, 4800L, 6000L);
		assertThat(captor.getValue())
				.extracting(Menu::getStatus)
				.containsOnly(MenuStatus.ACTIVE);
	}

	@Test
	void 메뉴가_이미_있으면_개발용_기본_메뉴를_저장하지_않는다() {
		// given
		when(menuRepository.count()).thenReturn(1L);

		// when
		initializer.run();

		// then
		verify(menuRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}
}
