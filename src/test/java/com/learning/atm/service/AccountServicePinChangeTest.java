package com.learning.atm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learning.atm.exception.InvalidPinException;
import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for the PIN rotation rule — the check that stops a stolen token locking out the owner. */
@ExtendWith(MockitoExtension.class)
class AccountServicePinChangeTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private com.learning.atm.repository.TransactionRepository transactionRepository;

	private AccountService service;

	private Account account;

	@BeforeEach
	void setUp() {
		service = new AccountService(accountRepository, transactionRepository,
				new AtmService(accountRepository, transactionRepository));
		account = new Account("1001", "1234", "Alice", new BigDecimal("100.00"));
		account.setId(1L);
	}

	@Test
	void correctCurrentPinRotatesToTheNewOne() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

		service.changePin(1L, "1234", "9876");

		assertThat(account.getPin()).isEqualTo("9876");
	}

	@Test
	void wrongCurrentPinIsRejectedAndThePinIsUnchanged() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> service.changePin(1L, "0000", "9876"))
				.isInstanceOf(InvalidPinException.class);

		assertThat(account.getPin()).isEqualTo("1234");
	}

	@Test
	void theNewPinMustDifferFromTheOldOne() {
		// No repository call is expected: this is rejected before touching the database.
		assertThatThrownBy(() -> service.changePin(1L, "1234", "1234"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("different");

		verify(accountRepository, never()).findById(any());
	}
}
