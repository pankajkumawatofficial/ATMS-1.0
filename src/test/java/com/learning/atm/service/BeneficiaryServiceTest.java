package com.learning.atm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learning.atm.dto.AddBeneficiaryRequest;
import com.learning.atm.dto.BeneficiaryResponse;
import com.learning.atm.exception.AccountNotFoundException;
import com.learning.atm.exception.DuplicateBeneficiaryException;
import com.learning.atm.model.Account;
import com.learning.atm.model.Beneficiary;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.BeneficiaryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for the saved-payee rules — mainly the ones that protect the customer's own account. */
@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private BeneficiaryRepository beneficiaryRepository;

	private BeneficiaryService service;

	private Account owner;
	private Account target;

	@BeforeEach
	void setUp() {
		service = new BeneficiaryService(accountRepository, beneficiaryRepository);
		owner = new Account("1001", "1234", "Alice", java.math.BigDecimal.ZERO);
		owner.setId(1L);
		target = new Account("1002", "5678", "Bob", java.math.BigDecimal.ZERO);
		target.setId(2L);
	}

	@Test
	void addStoresTheTargetNameAsASnapshot() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(target));
		when(beneficiaryRepository.existsByOwnerIdAndAccountNumber(1L, "1002")).thenReturn(false);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> {
			Beneficiary b = inv.getArgument(0);
			b.setId(7L);
			return b;
		});

		BeneficiaryResponse response =
				service.add(1L, new AddBeneficiaryRequest("1002", "Landlord"));

		assertThat(response.id()).isEqualTo(7L);
		assertThat(response.ownerName()).isEqualTo("Bob");
		assertThat(response.nickname()).isEqualTo("Landlord");
	}

	@Test
	void addRejectsYourOwnAccount() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(owner));

		assertThatThrownBy(() -> service.add(1L, new AddBeneficiaryRequest("1001", "Me")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("your own account");
	}

	@Test
	void addRejectsAnUnknownAccountNumber() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(accountRepository.findByAccountNumber("9999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.add(1L, new AddBeneficiaryRequest("9999", "Ghost")))
				.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	void addRejectsADuplicatePayee() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(target));
		when(beneficiaryRepository.existsByOwnerIdAndAccountNumber(1L, "1002")).thenReturn(true);

		assertThatThrownBy(() -> service.add(1L, new AddBeneficiaryRequest("1002", "Rent")))
				.isInstanceOf(DuplicateBeneficiaryException.class);
	}

	@Test
	void removeReportsNotFoundWhenTheRowIsNotTheCallers() {
		// The delete is scoped by owner id, so someone else's payee simply deletes 0 rows.
		when(beneficiaryRepository.deleteByOwnerIdAndId(1L, 99L)).thenReturn(0);

		assertThatThrownBy(() -> service.remove(1L, 99L))
				.isInstanceOf(com.learning.atm.exception.BeneficiaryNotFoundException.class);
	}

	@Test
	void removePassesTheOwnerIdSoTheDeleteCannotHitAnotherAccountsRow() {
		when(beneficiaryRepository.deleteByOwnerIdAndId(1L, 7L)).thenReturn(1);

		service.remove(1L, 7L);

		verify(beneficiaryRepository).deleteByOwnerIdAndId(1L, 7L);
	}

	@Test
	void listIsScopedToTheOwner() {
		when(beneficiaryRepository.findByOwnerIdOrderByNicknameAsc(1L)).thenReturn(List.of());

		assertThat(service.list(1L)).isEmpty();
		verify(beneficiaryRepository).findByOwnerIdOrderByNicknameAsc(1L);
	}

	/** Guards the nickname field that the UI renders — it must not be stored untrimmed. */
	@Test
	void nicknameIsTrimmedBeforeStorage() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(target));
		when(beneficiaryRepository.existsByOwnerIdAndAccountNumber(1L, "1002")).thenReturn(false);
		when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

		service.add(1L, new AddBeneficiaryRequest("1002", "  Landlord  "));

		ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
		verify(beneficiaryRepository).save(captor.capture());
		assertThat(captor.getValue().getNickname()).isEqualTo("Landlord");
	}
}
