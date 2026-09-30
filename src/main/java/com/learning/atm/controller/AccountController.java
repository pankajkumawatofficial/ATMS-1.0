package com.learning.atm.controller;

import com.learning.atm.dto.AccountLookupResponse;
import com.learning.atm.dto.AccountResponse;
import com.learning.atm.dto.AddBeneficiaryRequest;
import com.learning.atm.dto.BalanceResponse;
import com.learning.atm.dto.BeneficiaryResponse;
import com.learning.atm.dto.ChangePinRequest;
import com.learning.atm.dto.CreateAccountRequest;
import com.learning.atm.dto.DepositRequest;
import com.learning.atm.dto.PageResponse;
import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.dto.WithdrawRequest;
import com.learning.atm.exception.ForbiddenException;
import com.learning.atm.model.TransactionType;
import com.learning.atm.security.AuthInterceptor;
import com.learning.atm.service.AccountService;
import com.learning.atm.service.AtmService;
import com.learning.atm.service.AuthService;
import com.learning.atm.service.BeneficiaryService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account-oriented endpoints: open an account, inspect it, deposit and withdraw, plus the
 * statement, payee list and PIN rotation that hang off it.
 *
 * <p>Every handler (except {@code POST /api/accounts}) receives the logged-in account id from
 * {@link AuthInterceptor} and verifies the caller owns the account in the URL — authentication
 * proves <i>who you are</i>, this check proves <i>you may touch this account</i>.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	private final AccountService accountService;
	private final AtmService atmService;
	private final BeneficiaryService beneficiaryService;
	private final AuthService authService;

	public AccountController(AccountService accountService, AtmService atmService,
			BeneficiaryService beneficiaryService, AuthService authService) {
		this.accountService = accountService;
		this.atmService = atmService;
		this.beneficiaryService = beneficiaryService;
		this.authService = authService;
	}

	/** Open a new account — intentionally public so the API is easy to explore. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
		return accountService.create(request);
	}

	/**
	 * Confirms an account number and returns its holder's name, so the transfer screen can
	 * show "sending to Bob Verma (1002)" before the customer commits.
	 *
	 * <p>Learning note: an unknown number is a 200 with {@code exists=false}, not a 404 — the
	 * client renders it as a normal message rather than an error banner.
	 */
	@GetMapping("/lookup")
	public AccountLookupResponse lookup(@RequestParam String accountNumber) {
		return accountService.lookup(accountNumber.trim());
	}

	@GetMapping("/{id}")
	public AccountResponse get(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.get(id);
	}

	/** Balance plus how much of the daily withdrawal allowance is already used up. */
	@GetMapping("/{id}/balance")
	public BalanceResponse balance(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.balance(id);
	}

	/** The last few movements, for the "recent activity" strip on the home screen. */
	@GetMapping("/{id}/recent")
	public List<TransactionResponse> recent(@PathVariable long id,
			@RequestParam(defaultValue = "5") int limit,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.recent(id, limit);
	}

	/** The full statement, unpaged — kept for simple clients. */
	@GetMapping("/{id}/transactions")
	public List<TransactionResponse> history(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.history(id);
	}

	/**
	 * One page of the statement, filtered.
	 *
	 * <p>Every query parameter is optional: {@code GET .../statement?type=DEPOSIT&from=2026-09-01}
	 * is a valid request, and so is {@code GET .../statement} (everything, newest first).
	 *
	 * @param from inclusive start date, ISO format ({@code yyyy-MM-dd})
	 * @param to   inclusive end date — the whole day is included, not just midnight
	 */
	@GetMapping("/{id}/statement")
	public PageResponse<TransactionResponse> statement(
			@PathVariable long id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) TransactionType type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) BigDecimal minAmount,
			@RequestParam(required = false) BigDecimal maxAmount,
			@RequestParam(required = false) String search,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.statement(id, page, size, type, from, to,
				minAmount, maxAmount, search);
	}

	@PostMapping("/{id}/deposit")
	public TransactionResponse deposit(@PathVariable long id,
			@Valid @RequestBody DepositRequest request,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return atmService.deposit(id, request.amount());
	}

	@PostMapping("/{id}/withdraw")
	public TransactionResponse withdraw(@PathVariable long id,
			@Valid @RequestBody WithdrawRequest request,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return atmService.withdraw(id, request.amount(), request.pin());
	}

	/**
	 * Rotates the PIN. Every other signed-in device is logged out, because a PIN change is
	 * what a customer does <i>after</i> suspecting the old one leaked.
	 */
	@PostMapping("/{id}/change-pin")
	public Map<String, Object> changePin(@PathVariable long id,
			@Valid @RequestBody ChangePinRequest request,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId,
			@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		requireOwner(sessionAccountId, id);
		accountService.changePin(id, request.currentPin(), request.newPin());
		int kicked = authService.logoutOtherSessions(id, token);
		return Map.of("message", "PIN changed",
				"otherSessionsSignedOut", kicked);
	}

	// -------------------------------------------------------------------------
	// Beneficiaries (saved payees)
	// -------------------------------------------------------------------------

	@GetMapping("/{id}/beneficiaries")
	public List<BeneficiaryResponse> beneficiaries(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return beneficiaryService.list(id);
	}

	@PostMapping("/{id}/beneficiaries")
	@ResponseStatus(HttpStatus.CREATED)
	public BeneficiaryResponse addBeneficiary(@PathVariable long id,
			@Valid @RequestBody AddBeneficiaryRequest request,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return beneficiaryService.add(id, request);
	}

	@DeleteMapping("/{id}/beneficiaries/{beneficiaryId}")
	public Map<String, String> removeBeneficiary(@PathVariable long id,
			@PathVariable long beneficiaryId,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		beneficiaryService.remove(id, beneficiaryId);
		return Map.of("message", "Beneficiary removed");
	}

	private static void requireOwner(long sessionAccountId, long targetAccountId) {
		if (sessionAccountId != targetAccountId) {
			throw new ForbiddenException("This account does not belong to the logged-in session");
		}
	}
}
