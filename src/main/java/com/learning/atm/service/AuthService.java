package com.learning.atm.service;

import com.learning.atm.dto.LoginRequest;
import com.learning.atm.dto.LoginResponse;
import com.learning.atm.exception.InvalidCredentialsException;
import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies session tokens.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>After a successful login we hand back a random token. The client stores it and sends
 *       {@code Authorization: Bearer <token>} on every request — this is called
 *       <b>token-based statelessness</b> (the server remembers nothing about cookies).</li>
 *   <li>Sessions live in a plain {@link ConcurrentHashMap}, so they vanish on restart and
 *       don't scale past one process. Real systems use JWT or a shared store such as Redis.</li>
 * </ul>
 */
@Service
public class AuthService {

	private final AccountRepository accountRepository;

	/** token -> id of the logged-in account */
	private final Map<String, Long> sessions = new ConcurrentHashMap<>();

	public AuthService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public LoginResponse login(LoginRequest request) {
		Account account = accountRepository.findByAccountNumber(request.accountNumber())
				.orElseThrow(InvalidCredentialsException::new);

		// Wrong PIN must look exactly like "no such account" to avoid probing.
		if (!account.getPin().equals(request.pin())) {
			throw new InvalidCredentialsException();
		}

		String token = UUID.randomUUID().toString().replace("-", "");
		sessions.put(token, account.getId());

		return new LoginResponse(token, account.getId(), account.getAccountNumber(),
				account.getOwnerName(), account.getBalance());
	}

	/** @return the account id behind this token, or {@code null} when the token is invalid */
	public Long authenticate(String token) {
		return token == null ? null : sessions.get(token);
	}

	public void logout(String token) {
		if (token != null) {
			sessions.remove(token);
		}
	}
}
