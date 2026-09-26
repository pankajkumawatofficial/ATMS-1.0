package com.learning.atm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.exception.InsufficientFundsException;
import com.learning.atm.exception.InvalidPinException;
import com.learning.atm.model.Account;
import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the money logic — no database, no HTTP, just fakes ({@code @Mock}).
 * The service only talks to repositories, which is exactly why it is easy to test.
 */
@ExtendWith(MockitoExtension.class)
class AtmServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private AtmService atmService;

	private Account account;

	@BeforeEach
	void setUp() {
		account = new Account("1001", "1234", "Alice", new BigDecimal("100.00"));
		account.setId(1L);

		// JPA assigns generated ids during save(); mimic that for the mock.
		// lenient(): the failure-case tests never reach save().
		lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			transaction.setId(99L);
			return transaction;
		});
	}

	@Test
	void depositIncreasesBalanceAndWritesLedgerRow() {
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		TransactionResponse response = atmService.deposit(1L, new BigDecimal("50.50"));

		assertThat(account.getBalance()).isEqualByComparingTo("150.50");
		assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
		assertThat(response.balanceAfter()).isEqualByComparingTo("150.50");
		assertThat(response.id()).isEqualTo(99L);
	}

	@Test
	void withdrawDeductsMoneyWhenFundsAndPinAreValid() {
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		TransactionResponse response = atmService.withdraw(1L, new BigDecimal("40.00"), "1234");

		assertThat(account.getBalance()).isEqualByComparingTo("60.00");
		assertThat(response.type()).isEqualTo(TransactionType.WITHDRAWAL);
	}

	@Test
	void withdrawRejectsInsufficientFunds() {
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> atmService.withdraw(1L, new BigDecimal("500.00"), "1234"))
				.isInstanceOf(InsufficientFundsException.class)
				.hasMessageContaining("Insufficient funds");

		assertThat(account.getBalance()).isEqualByComparingTo("100.00"); // untouched
	}

	@Test
	void withdrawRejectsWrongPin() {
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> atmService.withdraw(1L, new BigDecimal("10.00"), "9999"))
				.isInstanceOf(InvalidPinException.class);

		assertThat(account.getBalance()).isEqualByComparingTo("100.00"); // untouched
	}

	@Test
	void transferMovesMoneyBetweenBothAccounts() {
		Account target = new Account("1002", "5678", "Bob", new BigDecimal("10.00"));
		target.setId(2L);

		// Locks are taken in ascending id order: account 1 first, then account 2.
		when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(target));
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
		when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

		List<TransactionResponse> result =
				atmService.transfer(1L, "1002", new BigDecimal("25.00"), "1234");

		assertThat(account.getBalance()).isEqualByComparingTo("75.00");
		assertThat(target.getBalance()).isEqualByComparingTo("35.00");
		assertThat(result).hasSize(2);
		assertThat(result.get(0).type()).isEqualTo(TransactionType.TRANSFER_OUT);
		assertThat(result.get(0).counterpartyAccountNumber()).isEqualTo("1002");
		assertThat(result.get(1).type()).isEqualTo(TransactionType.TRANSFER_IN);
	}
}
