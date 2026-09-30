package com.learning.atm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.atm.dto.LoginRequest;
import com.learning.atm.dto.LoginResponse;
import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for session bookkeeping: multiple devices, and signing the others out on a PIN change. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AccountRepository accountRepository;

	private AuthService service;

	private Account account;

	@BeforeEach
	void setUp() {
		service = new AuthService(accountRepository);
		account = new Account("1001", "1234", "Alice", new BigDecimal("500.00"));
		account.setId(1L);
		when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(account));
	}

	private LoginResponse loginOn(String userAgent) {
		return service.login(new LoginRequest("1001", "1234"), userAgent);
	}

	@Test
	void twoLoginsProduceTwoIndependentSessions() {
		LoginResponse phone = loginOn("Phone");
		LoginResponse laptop = loginOn("Laptop");

		assertThat(phone.token()).isNotEqualTo(laptop.token());
		assertThat(service.authenticate(phone.token())).isEqualTo(1L);
		assertThat(service.authenticate(laptop.token())).isEqualTo(1L);
		assertThat(service.sessionsFor(1L)).hasSize(2);
	}

	@Test
	void logoutOnlyKillsTheTokenItWasGiven() {
		LoginResponse phone = loginOn("Phone");
		LoginResponse laptop = loginOn("Laptop");

		service.logout(phone.token());

		assertThat(service.authenticate(phone.token())).isNull();
		assertThat(service.authenticate(laptop.token())).isEqualTo(1L);
	}

	@Test
	void pinChangeKeepsTheCurrentDeviceButSignsOutTheOthers() {
		LoginResponse phone = loginOn("Phone");
		LoginResponse laptop = loginOn("Laptop");
		LoginResponse tablet = loginOn("Tablet");

		int kicked = service.logoutOtherSessions(1L, phone.token());

		assertThat(kicked).isEqualTo(2);
		assertThat(service.authenticate(phone.token())).isEqualTo(1L);
		assertThat(service.authenticate(laptop.token())).isNull();
		assertThat(service.authenticate(tablet.token())).isNull();
	}

	@Test
	void signingOutOthersDoesNotTouchAnotherAccountsSessions() {
		Account other = new Account("1002", "5678", "Bob", BigDecimal.ONE);
		other.setId(2L);
		when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(other));
		LoginResponse bob = service.login(new LoginRequest("1002", "5678"), "Bob's laptop");
		LoginResponse alice = loginOn("Alice's phone");

		service.logoutOtherSessions(1L, alice.token());

		assertThat(service.authenticate(bob.token())).isEqualTo(2L);
	}

	@Test
	void sessionViewsMarkOnlyTheRequestingDevice() {
		LoginResponse phone = loginOn("Phone");
		loginOn("Laptop");

		var views = service.sessionViews(1L, phone.token());

		assertThat(views).hasSize(2);
		assertThat(views.stream().filter(v -> v.current()).count()).isEqualTo(1);
		assertThat(views.stream().filter(v -> v.userAgent().equals("Laptop")).findFirst()
				.orElseThrow().current()).isFalse();
	}

	@Test
	void anUnknownTokenAuthenticatesAsNull() {
		assertThat(service.authenticate("not-a-real-token")).isNull();
		assertThat(service.authenticate(null)).isNull();
	}

	@Test
	void thePurgedSessionIsNoLongerAccepted() {
		LoginResponse phone = loginOn("Phone");

		service.purgeExpiredSessions();

		// Nothing has actually expired yet, so the session must survive the sweep.
		assertThat(service.authenticate(phone.token())).isEqualTo(1L);
	}
}
