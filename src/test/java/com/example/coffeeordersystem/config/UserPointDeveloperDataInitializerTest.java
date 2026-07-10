package com.example.coffeeordersystem.config;

import com.example.coffeeordersystem.domain.PointWallet;
import com.example.coffeeordersystem.domain.User;
import com.example.coffeeordersystem.repository.PointWalletRepository;
import com.example.coffeeordersystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 개발용 사용자와 포인트 지갑 초기화가 중복 생성을 피하고 누락된 지갑을 보완하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserPointDeveloperDataInitializerTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PointWalletRepository pointWalletRepository;

	@InjectMocks
	private UserPointDeveloperDataInitializer initializer;

	@Test
	void 테스트사용자가_없으면_사용자와_잔액_0원_지갑을_생성한다() {
		// given
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<PointWallet> walletCaptor = ArgumentCaptor.forClass(PointWallet.class);
		when(userRepository.findByName("테스트사용자")).thenReturn(Optional.empty());
		when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		initializer.run();

		// then
		verify(userRepository).save(userCaptor.capture());
		verify(pointWalletRepository).save(walletCaptor.capture());
		assertThat(userCaptor.getValue().getName()).isEqualTo("테스트사용자");
		assertThat(walletCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
		assertThat(walletCaptor.getValue().getBalance()).isZero();
	}

	@Test
	void 테스트사용자와_지갑이_이미_있으면_중복_생성하지_않는다() {
		// given
		User user = User.create("테스트사용자");
		when(userRepository.findByName("테스트사용자")).thenReturn(Optional.of(user));
		when(pointWalletRepository.existsByUser(user)).thenReturn(true);

		// when
		initializer.run();

		// then
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verify(pointWalletRepository, never()).save(org.mockito.ArgumentMatchers.any(PointWallet.class));
	}

	@Test
	void 테스트사용자는_있지만_지갑이_없으면_지갑만_생성한다() {
		// given
		User user = User.create("테스트사용자");
		ArgumentCaptor<PointWallet> walletCaptor = ArgumentCaptor.forClass(PointWallet.class);
		when(userRepository.findByName("테스트사용자")).thenReturn(Optional.of(user));
		when(pointWalletRepository.existsByUser(user)).thenReturn(false);

		// when
		initializer.run();

		// then
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verify(pointWalletRepository).save(walletCaptor.capture());
		assertThat(walletCaptor.getValue().getUser()).isSameAs(user);
		assertThat(walletCaptor.getValue().getBalance()).isZero();
	}
}
