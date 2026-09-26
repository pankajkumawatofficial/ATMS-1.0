package com.learning.atm.controller;

import com.learning.atm.dto.AccountResponse;
import com.learning.atm.dto.BalanceResponse;
import com.learning.atm.dto.CreateAccountRequest;
import com.learning.atm.dto.DepositRequest;
import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.dto.WithdrawRequest;
import com.learning.atm.exception.ForbiddenException;
import com.learning.atm.security.AuthInterceptor;
import com.learning.atm.service.AccountService;
import com.learning.atm.service.AtmService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account-oriented endpoints: open an account, inspect it, deposit and withdraw.
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

	public AccountController(AccountService accountService, AtmService atmService) {
		this.accountService = accountService;
		this.atmService = atmService;
	}

	/** Open a new account — intentionally public so the API is easy to explore. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
		return accountService.create(request);
	}

	@GetMapping("/{id}")
	public AccountResponse get(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.get(id);
	}

	@GetMapping("/{id}/balance")
	public BalanceResponse balance(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.balance(id);
	}

	@GetMapping("/{id}/transactions")
	public List<TransactionResponse> history(@PathVariable long id,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		requireOwner(sessionAccountId, id);
		return accountService.history(id);
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

	private static void requireOwner(long sessionAccountId, long targetAccountId) {
		if (sessionAccountId != targetAccountId) {
			throw new ForbiddenException("This account does not belong to the logged-in session");
		}
	}
}
