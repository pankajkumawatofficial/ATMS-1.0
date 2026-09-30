package com.learning.atm.service;

import com.learning.atm.dto.LoginRequest;
import com.learning.atm.dto.LoginResponse;
import com.learning.atm.dto.SessionInfo;
import com.learning.atm.exception.InvalidCredentialsException;
import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
 *   <li>Unlike the previous {@code Map<String, Long>}, each session now records
 *       <b>when</b> it was created, which is what makes idle expiry and the "other sessions"
 *       screen possible.</li>
 * </ul>
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	/** An idle session is rejected after this long, like a real ATM's automatic logout. */
	private static final Duration SESSION_TTL = Duration.ofMinutes(30);

	/**
	 * One live session. Package-private nested record — the outside world never sees it.
	 *
	 * @param createdAt  when the login happened, shown on the "active sessions" screen
	 * @param lastUsedAt when this token last carried a request; drives the idle timeout
	 */
	record Session(Long accountId, Instant createdAt, Instant lastUsedAt, String userAgent) {

		/**
		 * Returns a copy with {@code lastUsedAt} moved to now.
		 *
		 * <p>Learning note: records are immutable, so "sliding the expiry" means building a new
		 * instance and storing it back in the map. {@code createdAt} is deliberately preserved
		 * so the sessions screen can still say "signed in at 09:14" rather than losing history.
		 */
		Session touch() {
			return new Session(accountId, createdAt, Instant.now(), userAgent);
		}

		boolean isExpired() {
			return lastUsedAt.plus(SESSION_TTL).isBefore(Instant.now());
		}
	}

	private final AccountRepository accountRepository;

	/** token -> session details (previously just the account id) */
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	public AuthService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public LoginResponse login(LoginRequest request, String userAgent) {
		Account account = accountRepository.findByAccountNumber(request.accountNumber())
				.orElseThrow(InvalidCredentialsException::new);

		// Wrong PIN must look exactly like "no such account" to avoid probing.
		if (!account.getPin().equals(request.pin())) {
			throw new InvalidCredentialsException();
		}

		String token = UUID.randomUUID().toString().replace("-", "");
		Instant now = Instant.now();
		sessions.put(token, new Session(account.getId(), now, now, userAgent));

		return new LoginResponse(token, account.getId(), account.getAccountNumber(),
				account.getOwnerName(), account.getBalance());
	}

	/**
	 * Looks up the account behind a token, extending the session's life as a side effect.
	 *
	 * <p>Learning note: the expiry check and the "push the deadline forward" write happen
	 * together under the same {@code computeIfPresent}, so a request cannot observe a session
	 * as expired while another thread is already refreshing it.
	 *
	 * @return the account id behind this token, or {@code null} when the token is invalid
	 */
	public Long authenticate(String token) {
		if (token == null) {
			return null;
		}
		Session refreshed = sessions.computeIfPresent(token, (key, session) ->
				session.isExpired() ? null : session.touch());
		return refreshed == null ? null : refreshed.accountId();
	}

	/** The session behind a token, or {@code null} — used by the "active sessions" screen. */
	public Session currentSession(String token) {
		Session session = (token == null) ? null : sessions.get(token);
		return (session == null || session.isExpired()) ? null : session;
	}

	public void logout(String token) {
		if (token != null) {
			sessions.remove(token);
		}
	}

	/**
	 * All live sessions of one account, newest first.
	 *
	 * <p>This is what powers the "you are signed in on N devices" panel, and it is also how a
	 * PIN change finds the sessions to throw away.
	 */
	public List<Session> sessionsFor(long accountId) {
		return sessions.entrySet().stream()
				.filter(e -> !e.getValue().isExpired())
				.filter(e -> e.getValue().accountId() == accountId)
				.sorted((a, b) -> b.getValue().createdAt().compareTo(a.getValue().createdAt()))
				.map(Map.Entry::getValue)
				.toList();
	}

	/**
	 * A session plus whether it is the one making the request.
	 *
	 * <p>Learning note: {@code current} is decided here rather than in the controller because
	 * the comparison needs the token, and the token is deliberately not part of {@link Session}
	 * — returning tokens to the browser would hand out live credentials for every device.
	 * The controller only ever sees a boolean.
	 */
	public List<SessionInfo> sessionViews(long accountId, String currentToken) {
		return sessions.entrySet().stream()
				.filter(e -> !e.getValue().isExpired())
				.filter(e -> e.getValue().accountId() == accountId)
				.map(e -> {
					Session s = e.getValue();
					return new SessionInfo(s.createdAt(), s.lastUsedAt(), s.userAgent(),
							e.getKey().equals(currentToken));
				})
				.sorted((a, b) -> b.signedInAt().compareTo(a.signedInAt()))
				.toList();
	}

	/**
	 * Kicks every session of an account <b>except</b> {@code keepToken}.
	 *
	 * <p>Called after a PIN change: if the PIN was stolen, rotating it must also evict whoever
	 * else is holding a token for that account. The current session survives so the customer
	 * who made the change is not logged out of the machine they are sitting at.
	 *
	 * @return how many sessions were invalidated
	 */
	public int logoutOtherSessions(long accountId, String keepToken) {
		int before = sessions.size();
		sessions.entrySet().removeIf(e ->
				e.getValue().accountId() == accountId && !e.getKey().equals(keepToken));
		int removed = before - sessions.size();		if (removed > 0) {
			log.info("PIN changed for account id {} — invalidated {} other session(s)", accountId, removed);
		}
		return removed;
	}

	/**
	 * Housekeeping: drops sessions nobody has used within the TTL.
	 *
	 * <p>Without this, a client that closes the tab without calling {@code /logout} would leave
	 * its entry in the map forever — a slow memory leak. {@code @Scheduled} needs
	 * {@code @EnableScheduling}, which {@link com.learning.atm.AtmApplication} declares.
	 */
	@Scheduled(fixedDelay = 60_000)
	public void purgeExpiredSessions() {
		int before = sessions.size();
		sessions.entrySet().removeIf(e -> e.getValue().isExpired());
		int removed = before - sessions.size();
		if (removed > 0) {
			log.debug("Purged {} expired session(s)", removed);
		}
	}
}
